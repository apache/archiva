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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files.
 * It validates MD5 and SHA-1 checksums.
 *
 * @todo remove stateful parts, change to singleton instantiation
 * @plexus.component role="org.apache.maven.repository.reporting.ArtifactReportProcessor" role-hint="checksum"
 */
public class ChecksumArtifactReporter
    implements ArtifactReportProcessor
{
    private static final int BYTE_MASK = 0xFF;

    private static final int CHECKSUM_BUFFER_SIZE = 256;

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

        File md5File = new File( repository.getBasedir(), path + ".md5" );
        if ( md5File.exists() )
        {
            try
            {
                if ( validateChecksum( file, md5File, "MD5" ) )
                {
                    reporter.addSuccess( artifact );
                }
                else
                {
                    reporter.addFailure( artifact, "MD5 checksum does not match." );
                }
            }
            catch ( NoSuchAlgorithmException e )
            {
                reporter.addFailure( artifact, "Unable to read MD5: " + e.getMessage() );
            }
            catch ( IOException e )
            {
                reporter.addFailure( artifact, "Unable to read MD5: " + e.getMessage() );
            }
        }
        else
        {
            reporter.addFailure( artifact, "MD5 checksum file does not exist." );
        }

        File sha1File = new File( repository.getBasedir(), path + ".sha1" );
        if ( sha1File.exists() )
        {
            try
            {
                if ( validateChecksum( file, sha1File, "SHA-1" ) )
                {
                    reporter.addSuccess( artifact );
                }
                else
                {
                    reporter.addFailure( artifact, "SHA-1 checksum does not match." );
                }
            }
            catch ( NoSuchAlgorithmException e )
            {
                reporter.addFailure( artifact, "Unable to read SHA-1: " + e.getMessage() );
            }
            catch ( IOException e )
            {
                reporter.addFailure( artifact, "Unable to read SHA-1: " + e.getMessage() );
            }
        }
        else
        {
            reporter.addFailure( artifact, "SHA-1 checksum file does not exist." );
        }
    }

    /**
     * Validate the checksums of the metadata. Get the metadata file from the
     * repository then validate the checksum.
     */
    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository, ArtifactReporter reporter )
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

        File md5File = new File( repository.getBasedir(), path + ".md5" );
        if ( md5File.exists() )
        {
            try
            {
                if ( validateChecksum( file, md5File, "MD5" ) )
                {
                    reporter.addSuccess( metadata );
                }
                else
                {
                    reporter.addFailure( metadata, "MD5 checksum does not match." );
                }
            }
            catch ( NoSuchAlgorithmException e )
            {
                reporter.addFailure( metadata, "Unable to read MD5: " + e.getMessage() );
            }
            catch ( IOException e )
            {
                reporter.addFailure( metadata, "Unable to read MD5: " + e.getMessage() );
            }
        }
        else
        {
            reporter.addFailure( metadata, "MD5 checksum file does not exist." );
        }

        File sha1File = new File( repository.getBasedir(), path + ".sha1" );
        if ( sha1File.exists() )
        {
            try
            {
                if ( validateChecksum( file, sha1File, "SHA-1" ) )
                {
                    reporter.addSuccess( metadata );
                }
                else
                {
                    reporter.addFailure( metadata, "SHA-1 checksum does not match." );
                }
            }
            catch ( NoSuchAlgorithmException e )
            {
                reporter.addFailure( metadata, "Unable to read SHA1: " + e.getMessage() );
            }
            catch ( IOException e )
            {
                reporter.addFailure( metadata, "Unable to read SHA1: " + e.getMessage() );
            }
        }
        else
        {
            reporter.addFailure( metadata, "SHA-1 checksum file does not exist." );
        }

    }

    /**
     * Validate the checksum of the file.
     *
     * @param file         The file to be validated.
     * @param checksumFile the checksum to validate against
     * @param algo         The checksum algorithm used.
     */
    private boolean validateChecksum( File file, File checksumFile, String algo )
        throws NoSuchAlgorithmException, IOException
    {
        boolean valid = false;

        //Create checksum for jar file
        byte[] chk1 = createChecksum( file, algo );
        if ( chk1 != null )
        {
            //read the checksum file
            String checksum = FileUtils.fileRead( checksumFile );
            valid = checksum.toUpperCase().equals( byteArrayToHexStr( chk1 ).toUpperCase() );
        }
        return valid;
    }

    /**
     * Create a checksum from the specified metadata file.
     *
     * @param file The file that will be created a checksum.
     * @param algo The algorithm to be used (MD5, SHA-1)
     * @return
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @todo move to utility class
     */
    private byte[] createChecksum( File file, String algo )
        throws FileNotFoundException, NoSuchAlgorithmException, IOException
    {
        MessageDigest digest = MessageDigest.getInstance( algo );

        InputStream fis = new FileInputStream( file );
        try
        {
            byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];
            int numRead;
            do
            {
                numRead = fis.read( buffer );
                if ( numRead > 0 )
                {
                    digest.update( buffer, 0, numRead );
                }
            }
            while ( numRead != -1 );
        }
        finally
        {
            fis.close();
        }

        return digest.digest();
    }

    /**
     * Convert an incoming array of bytes into a string that represents each of
     * the bytes as two hex characters.
     *
     * @param data
     * @todo move to utilities
     */
    public static String byteArrayToHexStr( byte[] data )
    {
        String output = "";

        for ( int cnt = 0; cnt < data.length; cnt++ )
        {
            //Deposit a byte into the 8 lsb of an int.
            int tempInt = data[cnt] & BYTE_MASK;

            //Get hex representation of the int as a string.
            String tempStr = Integer.toHexString( tempInt );

            //Append a leading 0 if necessary so that each hex string will contain 2 characters.
            if ( tempStr.length() == 1 )
            {
                tempStr = "0" + tempStr;
            }

            //Concatenate the two characters to the output string.
            output = output + tempStr;
        }

        return output.toUpperCase();
    }

}
