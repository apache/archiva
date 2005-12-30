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

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * This class creates the artifact and metadata files used for testing the ChecksumArtifactReporter.
 * It is extended by ChecksumArtifactReporterTest class.
 */
public abstract class AbstractChecksumArtifactReporterTestCase
    extends AbstractRepositoryReportsTestCase
{
    protected static final String[] validArtifactChecksumJars = {"validArtifact-1.0"};

    protected static final String[] invalidArtifactChecksumJars = {"invalidArtifact-1.0"};

    protected static final String metadataChecksumFilename = "maven-metadata-repository";

    private static final int CHECKSUM_BUFFER_SIZE = 256;

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Create checksum files.
     *
     * @param type The type of checksum file to be created.
     */
    protected void createChecksumFile( String type )
        throws NoSuchAlgorithmException, IOException
    {
        //loop through the valid artifact names..
        if ( "VALID".equals( type ) )
        {
            for ( int i = 0; i < validArtifactChecksumJars.length; i++ )
            {
                writeChecksumFile( "checksumTest/", validArtifactChecksumJars[i], "jar", true );
            }
        }
        else if ( "INVALID".equals( type ) )
        {
            for ( int i = 0; i < invalidArtifactChecksumJars.length; i++ )
            {
                writeChecksumFile( "checksumTest/", invalidArtifactChecksumJars[i], "jar", false );
            }
        }
    }

    /**
     * Create checksum files for metadata.
     *
     * @param type The type of checksum to be created. (Valid or invalid)
     */
    protected void createMetadataFile( String type )
        throws NoSuchAlgorithmException, IOException
    {
        //loop through the valid artifact names..
        if ( "VALID".equals( type ) )
        {
            writeMetadataFile( "checksumTest/validArtifact/1.0/", metadataChecksumFilename, "xml", true );
            writeMetadataFile( "checksumTest/validArtifact/", metadataChecksumFilename, "xml", true );
            writeMetadataFile( "checksumTest/", metadataChecksumFilename, "xml", true );
        }
        else if ( "INVALID".equals( type ) )
        {
            writeMetadataFile( "checksumTest/invalidArtifact/1.0/", metadataChecksumFilename, "xml", false );
        }
    }

    /**
     * Create artifact together with its checksums.
     *
     * @param relativePath The groupId
     * @param filename     The filename of the artifact to be created.
     * @param type         The file type (JAR)
     * @param isValid      Indicates whether the checksum to be created is valid or not.
     */
    private void writeChecksumFile( String relativePath, String filename, String type, boolean isValid )
        throws IOException, NoSuchAlgorithmException
    {
        //Initialize variables for creating jar files
        String repoUrl = repository.getBasedir();

        String dirs = filename.replace( '-', '/' );
        //create the group level directory of the artifact
        File dirFiles = new File( repoUrl + relativePath + dirs );

        if ( dirFiles.mkdirs() )
        {

            // create a jar file
            FileOutputStream f = new FileOutputStream( repoUrl + relativePath + dirs + "/" + filename + "." + type );
            JarOutputStream out = new JarOutputStream( new BufferedOutputStream( f ) );

            // jar sample.txt
            String filename1 = repoUrl + relativePath + dirs + "/sample.txt";
            createSampleFile( filename1 );

            BufferedReader in = new BufferedReader( new FileReader( filename1 ) );
            out.putNextEntry( new JarEntry( filename1 ) );
            IOUtil.copy( in, out );
            in.close();
            out.close();

            //Create md5 and sha-1 checksum files..
            byte[] md5chk = createChecksum( repoUrl + relativePath + dirs + "/" + filename + "." + type, "MD5" );
            byte[] sha1chk = createChecksum( repoUrl + relativePath + dirs + "/" + filename + "." + type, "SHA-1" );

            File file;

            if ( md5chk != null )
            {
                file = new File( repoUrl + relativePath + dirs + "/" + filename + "." + type + ".md5" );
                OutputStream os = new FileOutputStream( file );
                OutputStreamWriter osw = new OutputStreamWriter( os );
                if ( !isValid )
                {
                    osw.write( ChecksumArtifactReporter.byteArrayToHexStr( md5chk ) + "1" );
                }
                else
                {
                    osw.write( ChecksumArtifactReporter.byteArrayToHexStr( md5chk ) );
                }
                osw.close();
            }

            if ( sha1chk != null )
            {
                file = new File( repoUrl + relativePath + dirs + "/" + filename + "." + type + ".sha1" );
                OutputStream os = new FileOutputStream( file );
                OutputStreamWriter osw = new OutputStreamWriter( os );
                if ( !isValid )
                {
                    osw.write( ChecksumArtifactReporter.byteArrayToHexStr( sha1chk ) + "2" );
                }
                else
                {
                    osw.write( ChecksumArtifactReporter.byteArrayToHexStr( sha1chk ) );
                }
                osw.close();
            }
        }
    }

    /**
     * Create metadata file together with its checksums.
     *
     * @param relativePath The groupId
     * @param filename     The filename of the artifact to be created.
     * @param type         The file type (JAR)
     * @param isValid      Indicates whether the checksum to be created is valid or not.
     */
    private void writeMetadataFile( String relativePath, String filename, String type, boolean isValid )
        throws IOException, NoSuchAlgorithmException
    {
        //create checksum for the metadata file..
        String repoUrl = repository.getBasedir();
        String url = repository.getBasedir() + "/" + filename + "." + type;

        FileUtils.copyFile( new File( url ), new File( repoUrl + relativePath + filename + "." + type ) );

        //Create md5 and sha-1 checksum files..
        byte[] md5chk = createChecksum( repoUrl + relativePath + filename + "." + type, "MD5" );
        byte[] sha1chk = createChecksum( repoUrl + relativePath + filename + "." + type, "SHA-1" );

        File file;

        if ( md5chk != null )
        {
            file = new File( repoUrl + relativePath + filename + "." + type + ".md5" );
            OutputStream os = new FileOutputStream( file );
            OutputStreamWriter osw = new OutputStreamWriter( os );
            if ( !isValid )
            {
                osw.write( ChecksumArtifactReporter.byteArrayToHexStr( md5chk ) + "1" );
            }
            else
            {
                osw.write( ChecksumArtifactReporter.byteArrayToHexStr( md5chk ) );
            }
            osw.close();
        }

        if ( sha1chk != null )
        {
            file = new File( repoUrl + relativePath + filename + "." + type + ".sha1" );
            OutputStream os = new FileOutputStream( file );
            OutputStreamWriter osw = new OutputStreamWriter( os );
            if ( !isValid )
            {
                osw.write( ChecksumArtifactReporter.byteArrayToHexStr( sha1chk ) + "2" );
            }
            else
            {
                osw.write( ChecksumArtifactReporter.byteArrayToHexStr( sha1chk ) );
            }
            osw.close();
        }
    }

    /**
     * Create the sample file that will be included in the jar.
     *
     * @param filename
     */
    private void createSampleFile( String filename )
        throws IOException
    {
        File file = new File( filename );
        OutputStream os = new FileOutputStream( file );
        OutputStreamWriter osw = new OutputStreamWriter( os );
        osw.write( "This is the content of the sample file that will be included in the jar file." );
        osw.close();
    }

    /**
     * Create a checksum from the specified metadata file.
     *
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private byte[] createChecksum( String filename, String algo )
        throws FileNotFoundException, NoSuchAlgorithmException, IOException
    {

        // TODO: share with ArtifactRepositoryIndex.getChecksum(), ChecksumArtifactReporter.getChecksum()
        InputStream fis = new FileInputStream( filename );
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
     * Delete the test directory created in the repository.
     *
     * @param dir The directory to be deleted.
     */
    protected boolean deleteTestDirectory( File dir )
    {
        boolean b;

        try
        {
            FileUtils.deleteDirectory( dir );
            b = true;
        }
        catch ( IOException ioe )
        {
            b = false;
        }

        return b;
    }

    private void deleteFile( String filename )
    {
        File f = new File( filename );
        f.delete();
    }

    protected void deleteChecksumFiles( String type )
    {
        //delete valid checksum files of artifacts created
        for ( int i = 0; i < validArtifactChecksumJars.length; i++ )
        {
            deleteFile( repository.getBasedir() + "checksumTest/" + validArtifactChecksumJars[i].replace( '-', '/' ) +
                "/" + validArtifactChecksumJars[i] + "." + type + ".md5" );

            deleteFile( repository.getBasedir() + "checksumTest/" + validArtifactChecksumJars[i].replace( '-', '/' ) +
                "/" + validArtifactChecksumJars[i] + "." + type + ".sha1" );
        }

        //delete valid checksum files of metadata file
        for ( int i = 0; i < validArtifactChecksumJars.length; i++ )
        {
            deleteFile( repository.getBasedir() + "checksumTest/" + validArtifactChecksumJars[i].replace( '-', '/' ) +
                "/" + metadataChecksumFilename + ".xml.md5" );

            deleteFile( repository.getBasedir() + "checksumTest/" + validArtifactChecksumJars[i].replace( '-', '/' ) +
                "/" + metadataChecksumFilename + ".xml.sha1" );
        }
    }

}
