package org.apache.maven.repository.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.repository.digest.Digester;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files.
 * It validates MD5 and SHA-1 checksums.
 *
 * @plexus.component role="org.apache.maven.repository.reporting.ArtifactReportProcessor" role-hint="checksum"
 */
public class ChecksumArtifactReporter
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement
     */
    private Digester digester;

    /**
     * Validate the checksum of the specified artifact.
     *
     * @param model
     * @param artifact
     * @param reporter
     * @param repository
     */
    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        //check if checksum files exist
        String path = repository.pathOf( artifact );
        File file = new File( repository.getBasedir(), path );

        verifyChecksum( repository, path + ".md5", file, Digester.MD5, reporter, artifact );
        verifyChecksum( repository, path + ".sha1", file, Digester.SHA1, reporter, artifact );
    }

    private void verifyChecksum( ArtifactRepository repository, String path, File file, String checksumAlgorithm,
                                 ArtifactReporter reporter, Artifact artifact )
    {
        File checksumFile = new File( repository.getBasedir(), path );
        if ( checksumFile.exists() )
        {
            try
            {
                if ( digester.verifyChecksum( file, FileUtils.fileRead( checksumFile ), checksumAlgorithm ) )
                {
                    reporter.addSuccess( artifact );
                }
                else
                {
                    reporter.addFailure( artifact, checksumAlgorithm + " checksum does not match." );
                }
            }
            catch ( NoSuchAlgorithmException e )
            {
                reporter.addFailure( artifact, "Unable to read " + checksumAlgorithm + ": " + e.getMessage() );
            }
            catch ( IOException e )
            {
                reporter.addFailure( artifact, "Unable to read " + checksumAlgorithm + ": " + e.getMessage() );
            }
        }
        else
        {
            reporter.addFailure( artifact, checksumAlgorithm + " checksum file does not exist." );
        }
    }
}
