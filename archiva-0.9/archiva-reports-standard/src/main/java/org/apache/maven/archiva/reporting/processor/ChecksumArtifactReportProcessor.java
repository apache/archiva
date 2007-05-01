package org.apache.maven.archiva.reporting.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;

import java.io.File;
import java.io.IOException;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files.
 * It validates MD5 and SHA-1 checksums.
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor" role-hint="checksum"
 */
public class ChecksumArtifactReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester sha1Digester;

    /**
     * @plexus.requirement role-hint="md5"
     */
    private Digester md5Digester;
    
    /**
     * @plexus.requirement
     */
    private ArtifactResultsDatabase database;

    private static final String ROLE_HINT = "checksum";

    public void processArtifact( Artifact artifact, Model model )
    {
        ArtifactRepository repository = artifact.getRepository();

        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        //check if checksum files exist
        String path = repository.pathOf( artifact );
        File file = new File( repository.getBasedir(), path );

        // TODO: make md5 configurable
//        verifyChecksum( repository, path + ".md5", file, md5Digester, reporter, artifact );
        verifyChecksum( repository, path + ".sha1", file, sha1Digester, artifact );
    }

    private void verifyChecksum( ArtifactRepository repository, String path, File file, Digester digester,
                                 Artifact artifact )
    {
        File checksumFile = new File( repository.getBasedir(), path );
        if ( checksumFile.exists() )
        {
            try
            {
                digester.verify( file, FileUtils.readFileToString( checksumFile, null ) );
            }
            catch ( DigesterException e )
            {
                addFailure( artifact, "checksum-wrong", e.getMessage() );
            }
            catch ( IOException e )
            {
                addFailure( artifact, "checksum-io-exception", "Read file error: " + e.getMessage() );
            }
        }
        else
        {
            addFailure( artifact, "checksum-missing",
                        digester.getAlgorithm() + " checksum file does not exist." );
        }
    }

    private void addFailure( Artifact artifact, String problem, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        database.addFailure( artifact, ROLE_HINT, problem, reason );
    }
}
