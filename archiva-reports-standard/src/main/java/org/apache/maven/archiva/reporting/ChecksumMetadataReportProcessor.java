package org.apache.maven.archiva.reporting;

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

import org.apache.maven.archiva.digest.Digester;
import org.apache.maven.archiva.digest.DigesterException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files.
 * It validates MD5 and SHA-1 checksums.
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.MetadataReportProcessor" role-hint="checksum-metadata"
 */
public class ChecksumMetadataReportProcessor
    implements MetadataReportProcessor
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
     * Validate the checksums of the metadata. Get the metadata file from the
     * repository then validate the checksum.
     */
    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository,
                                 ReportingDatabase reporter )
    {
        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        //check if checksum files exist
        String path = repository.pathOfRemoteRepositoryMetadata( metadata );
        File file = new File( repository.getBasedir(), path );

        verifyChecksum( repository, path + ".md5", file, md5Digester, reporter, metadata );
        verifyChecksum( repository, path + ".sha1", file, sha1Digester, reporter, metadata );
    }

    private void verifyChecksum( ArtifactRepository repository, String path, File file, Digester digester,
                                 ReportingDatabase reporter, RepositoryMetadata metadata )
    {
        File checksumFile = new File( repository.getBasedir(), path );
        if ( checksumFile.exists() )
        {
            try
            {
                digester.verify( file, FileUtils.fileRead( checksumFile ) );
            }
            catch ( DigesterException e )
            {
                reporter.addFailure( metadata, e.getMessage() );
            }
            catch ( IOException e )
            {
                reporter.addFailure( metadata, "Read file error: " + e.getMessage() );
            }
        }
        else
        {
            reporter.addFailure( metadata, digester.getAlgorithm() + " checksum file does not exist." );
        }
    }

}
