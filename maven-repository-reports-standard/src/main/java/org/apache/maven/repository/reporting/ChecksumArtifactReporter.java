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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files.
 * It validates MD5 and SHA-1 checksums.
 *
 * @todo remove stateful parts, change to singleton instantiation
 * @plexus.component role="org.apache.maven.repository.reporting.ArtifactReportProcessor" role-hint="checksum" instantiation-strategy="per-lookup"
 */
public class ChecksumArtifactReporter
    implements ArtifactReportProcessor
{
    protected InputStream md5InputStream;

    protected InputStream sha1InputStream;

    private boolean isLocal = true;

    private static final int BYTE_MASK = 0xFF;

    private static final int CHECKSUM_BUFFER_SIZE = 256;

    /**
     * Validate the checksum of the specified artifact.
     *
     * @param model
     * @param artifact
     * @param reporter
     * @param repository
     * @todo fix repo paths
     */
    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
    {
        String repositoryUrl;

        if ( !"file".equals( repository.getProtocol() ) )
        {
            isLocal = false;
            repositoryUrl = repository.getUrl();
        }
        else
        {
            repositoryUrl = repository.getBasedir();
        }

        String artifactUrl = repositoryUrl + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" +
            artifact.getBaseVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." +
            artifact.getType();

        //check if checksum files exist
        boolean md5Exists = getMD5File( artifactUrl );
        boolean sha1Exists = getSHA1File( artifactUrl );

        if ( md5Exists )
        {
            if ( validateChecksum( artifactUrl, "MD5" ) )
            {
                reporter.addSuccess( artifact );
            }
            else
            {
                reporter.addFailure( artifact, "MD5 checksum does not match." );
            }
        }
        else
        {
            reporter.addFailure( artifact, "MD5 checksum file does not exist." );
        }

        if ( sha1Exists )
        {
            if ( validateChecksum( artifactUrl, "SHA-1" ) )
            {
                reporter.addSuccess( artifact );
            }
            else
            {
                reporter.addFailure( artifact, "SHA-1 checksum does not match." );
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
     *
     * @todo fix repo paths
     */
    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository, ArtifactReporter reporter )
    {
        String repositoryUrl;
        String filename;
        if ( !"file".equals( repository.getProtocol() ) )
        {
            isLocal = false;
            repositoryUrl = repository.getUrl() + "/";
            filename = metadata.getRemoteFilename();
        }
        else
        {
            repositoryUrl = repository.getBasedir() + "/";
            filename = metadata.getLocalFilename( repository );
        }
        String metadataUrl;

        if ( metadata.storedInArtifactVersionDirectory() && !metadata.storedInGroupDirectory() )
        {
            //version metadata
            metadataUrl = repositoryUrl + metadata.getGroupId() + "/" + metadata.getArtifactId() + "/" +
                metadata.getBaseVersion() + "/";
        }
        else if ( !metadata.storedInArtifactVersionDirectory() && metadata.storedInGroupDirectory() )
        {
            //group metadata
            metadataUrl = repositoryUrl + metadata.getGroupId() + "/";
        }
        else
        {
            //artifact metadata
            metadataUrl = repositoryUrl + metadata.getGroupId() + "/" + metadata.getArtifactId() + "/";
        }

        //add the file name of the metadata
        metadataUrl = metadataUrl + filename;

        //check if checksum files exist
        boolean md5Exists = getMD5File( metadataUrl );
        boolean sha1Exists = getSHA1File( metadataUrl );

        if ( md5Exists )
        {
            if ( validateChecksum( metadataUrl, "MD5" ) )
            {
                reporter.addSuccess( metadata );
            }
            else
            {
                reporter.addFailure( metadata, "MD5 checksum does not match." );
            }
        }
        else
        {
            reporter.addFailure( metadata, "MD5 checksum file does not exist." );
        }

        if ( sha1Exists )
        {
            if ( validateChecksum( metadataUrl, "SHA-1" ) )
            {
                reporter.addSuccess( metadata );
            }
            else
            {
                reporter.addFailure( metadata, "SHA-1 checksum does not match." );
            }
        }
        else
        {
            reporter.addFailure( metadata, "SHA-1 checksum file does not exist." );
        }

    }

    /**
     * Get the MD5 Checksum file. If not found, return false.
     *
     * @param filename The name of the artifact whose MD5 Checksum file will be retrieved.
     * @todo fix this erroneous object state
     */
    public boolean getMD5File( String filename )
    {
        try
        {
            if ( isLocal )
            {
                md5InputStream = new FileInputStream( filename + ".md5" );
            }
            else
            {
                URL url = new URL( filename );
                md5InputStream = url.openStream();
            }

            md5InputStream.close();
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }

    /**
     * Get the SHA1 Checksum file. If not found, return false.
     *
     * @param filename The name of the artifact whose SHA-1 Checksum file will be retrieved.
     * @todo fix this erroneous object state
     */
    public boolean getSHA1File( String filename )
    {
        try
        {
            if ( isLocal )
            {
                sha1InputStream = new FileInputStream( filename + ".sha1" );
            }
            else
            {
                URL url = new URL( filename );
                sha1InputStream = url.openStream();
            }
            sha1InputStream.close();
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }

    /**
     * Validate the checksum of the file.
     *
     * @param fileUrl The file to be validated.
     * @param algo    The checksum algorithm used.
     */
    protected boolean validateChecksum( String fileUrl, String algo )
    {
        boolean valid = false;

        try
        {
            //Create checksum for jar file
            String ext = ".md5";
            if ( "SHA-1".equals( algo ) )
            {
                ext = ".sha1";
            }
            byte[] chk1 = createChecksum( fileUrl, algo );
            if ( chk1 != null )
            {

                //read the md5 file
                File f = new File( fileUrl + ext );
                InputStream is = null;

                //check whether the file is located locally or remotely
                if ( isLocal )
                {
                    is = new FileInputStream( f );
                }
                else
                {
                    URL url = new URL( fileUrl + ext );
                    is = url.openStream();
                }

                char[] chars = new char[is.available()];
                InputStreamReader isr = new InputStreamReader( is );
                isr.read( chars );
                isr.close();

                String chk2Str = new String( chars );

                valid = chk2Str.toUpperCase().equals( byteArrayToHexStr( chk1 ).toUpperCase() );
            }
        }
        catch ( Exception e )
        {
            // TODO: fix this error handling
        }
        return valid;
    }

    /**
     * Create a checksum from the specified metadata file.
     *
     * @param filename The file that will be created a checksum.
     * @param algo     The algorithm to be used (MD5, SHA-1)
     * @return
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @todo move to utility class
     */
    protected byte[] createChecksum( String filename, String algo )
        throws FileNotFoundException, NoSuchAlgorithmException, IOException
    {
        InputStream fis;

        //check whether file is located locally or remotely
        if ( isLocal )
        {
            fis = new FileInputStream( filename );
        }
        else
        {
            URL url = new URL( filename );
            fis = url.openStream();
        }
        byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];

        MessageDigest complete = MessageDigest.getInstance( algo );
        int numRead;
        do
        {
            numRead = fis.read( buffer );
            if ( numRead > 0 )
            {
                complete.update( buffer, 0, numRead );
            }
        }
        while ( numRead != -1 );
        fis.close();

        return complete.digest();
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
        String tempStr = "";
        int tempInt = 0;

        for ( int cnt = 0; cnt < data.length; cnt++ )
        {
            //Deposit a byte into the 8 lsb of an int.
            tempInt = data[cnt] & BYTE_MASK;

            //Get hex representation of the int as a string.
            tempStr = Integer.toHexString( tempInt );

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
