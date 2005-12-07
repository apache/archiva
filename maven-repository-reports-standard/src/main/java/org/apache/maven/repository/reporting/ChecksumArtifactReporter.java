package org.apache.maven.repository.reporting;

/* 
 * Copyright 2001-2005 The Apache Software Foundation. 
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.artifact.repository.*;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files. 
 * It validates MD5 and SHA-1 chacksums.
 * 
 * @TODO 
 *  - Validate using remote repository.
 */
public class ChecksumArtifactReporter
    implements ArtifactReportProcessor, MetadataReportProcessor
{
    String ROLE = ChecksumArtifactReporter.class.getName();

    protected InputStream md5InputStream;

    protected InputStream sha1InputStream;

    /**
     * Validate the checksum of the specified artifact.
     * @param model
     * @param artifact
     * @param reporter
     * @param repository
     */
    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                ArtifactRepository repository )
    {
        System.out.println( " " );
        System.out
            .println( "===================================== +++++  PROCESS ARTIFACT +++++ ====================================" );
        String artifactUrl = "";
        String repositoryUrl = repository.getUrl();
        System.out.println("REPOSITORY PROTOCOL ------>>>> " + repository.getProtocol());

        artifactUrl = repositoryUrl + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/"
            + artifact.getBaseVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "."
            + artifact.getType();
        //System.out.println("ARTIFACT URL ------->>>> " + artifactUrl);

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
    }

    /**
     * Validate the checksums of the metadata. Get the metadata file from the 
     * repository then validate the checksum.
     */
    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository, ArtifactReporter reporter )
    {
        System.out.println( " " );
        System.out
            .println( "====================================== +++++  PROCESS METADATA +++++ ==============================" );

        String metadataUrl = "";
        String repositoryUrl = repository.getUrl();
        String filename = metadata.getRemoteFilename();

        //version metadata
        if ( metadata.storedInArtifactVersionDirectory() == true && metadata.storedInGroupDirectory() == false )
        {
            metadataUrl = repositoryUrl + metadata.getGroupId() + "/" + metadata.getArtifactId() + "/"
                + metadata.getBaseVersion() + "/";
            //group metadata
        }
        else if ( metadata.storedInArtifactVersionDirectory() == false && metadata.storedInGroupDirectory() == true )
        {
            metadataUrl = repositoryUrl + metadata.getGroupId() + "/";
            //artifact metadata
        }
        else
        {
            metadataUrl = repositoryUrl + metadata.getGroupId() + "/" + metadata.getArtifactId() + "/";
        }

        //add the file name of the metadata
        metadataUrl = metadataUrl + filename;
        //System.out.println( "METADATA URL -------> " + metadataUrl );

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

    }

    /**
     * Get the MD5 Checksum file. If not found, return false.
     * @param filename The name of the artifact whose MD5 Checksum file will be retrieved.
     * @return
     */
    public boolean getMD5File( String filename )
    {
        try
        {
            md5InputStream = new FileInputStream( filename + ".md5" );
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
     * @param filename The name of the artifact whose SHA-1 Checksum file will be retrieved.
     * @return
     */
    public boolean getSHA1File( String filename )
    {
        try
        {
            sha1InputStream = new FileInputStream( filename + ".sha1" );
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
     * @param fileUrl The file to be validated.
     * @param algo The checksum algorithm used. 
     * @return
     */
    protected boolean validateChecksum( String fileUrl, String algo )
    {
        boolean valid = false;
        byte[] chk1 = null, chk2 = null;

        try
        {
            //Create checksum for jar file
            String ext = ".md5";
            if ( algo.equals( "SHA-1" ) )
                ext = ".sha1";
            chk1 = createChecksum( fileUrl, algo );
            if ( chk1 != null )
            {

                //read the md5 file
                chk2 = new byte[chk1.length];

                File f = new File( fileUrl + ext );
                InputStream is = new FileInputStream( f );
                char[] chars = new char[is.available()];
                InputStreamReader isr = new InputStreamReader( is );
                isr.read( chars );
                isr.close();

                String chk2Str = new String( chars );
                System.out.println( "-----" + algo + " Checksum value (CHK1 - created checksum for jar file) ::::: "
                    + byteArrayToHexStr( chk1 ) );
                System.out.println( "-----" + algo + " Checksum value (CHK2 - content of CHECKSUM file) ::::: "
                    + chk2Str );

                if ( chk2Str.equals( byteArrayToHexStr( chk1 ) ) )
                {
                    valid = true;
                }
                else
                {
                    valid = false;
                }
            }
            return valid;

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return valid;
        }
    }

    /**
     * Create a checksum from the specified metadata file.
     * @param filename The file that will be created a checksum.
     * @param algo The algorithm to be used (MD5, SHA-1)
     * @return
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    protected byte[] createChecksum( String filename, String algo )
        throws FileNotFoundException, NoSuchAlgorithmException, IOException
    {

        InputStream fis = new FileInputStream( filename );
        byte[] buffer = new byte[1024];

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
     * @param data
     * @return
     */
    public String byteArrayToHexStr( byte[] data )
    {
        String output = "";
        String tempStr = "";
        int tempInt = 0;

        for ( int cnt = 0; cnt < data.length; cnt++ )
        {
            //Deposit a byte into the 8 lsb of an int.
            tempInt = data[cnt] & 0xFF;

            //Get hex representation of the int as a string.
            tempStr = Integer.toHexString( tempInt );

            //Append a leading 0 if necessary so that each hex string will contain 2 characters.
            if ( tempStr.length() == 1 )
                tempStr = "0" + tempStr;

            //Concatenate the two characters to the output string.
            output = output + tempStr;
        }

        return output.toUpperCase();
    }

}
