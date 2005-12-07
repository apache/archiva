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
 * @TODO
 *  - Create more valid and invalid artifacts & metadata files for further testing.
 *
 * This class creates the artifact and metadata files used for testing the ChecksumArtifactReporter.
 * It is extended by ChecksumArtifactReporterTest class.
 */
public class AbstractChecksumArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    protected static final String[] validArtifactChecksumJars = { "validArtifact-1.0" };

    protected static final String[] invalidArtifactChecksumJars = { "invalidArtifact-1.0" };

    protected static final String metadataChecksumFilename = "maven-metadata";

    public AbstractChecksumArtifactReporterTest()
    {
    }

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
     * @param type The type of checksum file to be created.
     * @return
     */
    protected boolean createChecksumFile( String type )
    {
        boolean written = true;

        //loop through the valid artifact names..
        if ( type.equals( "VALID" ) )
        {
            for ( int i = 0; i < validArtifactChecksumJars.length; i++ )
            {
                written = writeChecksumFile( "checksumTest/", validArtifactChecksumJars[i], "jar", true );
                if ( written == false )
                {
                    i = validArtifactChecksumJars.length;
                }
            }
        }
        else if ( type.equals( "INVALID" ) )
        {
            for ( int i = 0; i < invalidArtifactChecksumJars.length; i++ )
            {
                written = writeChecksumFile( "checksumTest/", invalidArtifactChecksumJars[i], "jar", false );
                if ( written == false )
                {
                    i = invalidArtifactChecksumJars.length;
                }
            }
        }

        return written;
    }

    /**
     * Create checksum files for metadata.
     * @param type The type of checksum to be created. (Valid or invalid)
     * @return
     */
    protected boolean createMetadataFile( String type )
    {
        boolean written = true;

        //loop through the valid artifact names..
        if ( type.equals( "VALID" ) )
        {
            writeMetadataFile( "checksumTest/validArtifact/1.0/", metadataChecksumFilename, "xml", true );

        }
        else if ( type.equals( "INVALID" ) )
        {
            writeMetadataFile( "checksumTest/invalidArtifact/1.0/", metadataChecksumFilename, "xml", false );
        }

        return written;
    }

    /**
     * Create artifact together with its checksums.
     * @param relativePath The groupId
     * @param filename The filename of the artifact to be created.
     * @param type The file type (JAR)
     * @param isValid Indicates whether the checksum to be created is valid or not.
     * @return
     */
    private boolean writeChecksumFile( String relativePath, String filename, String type, boolean isValid )
    {
        System.out.println( " " );
        System.out.println( "========================= ARTIFACT CHECKSUM ==================================" );

        //Initialize variables for creating jar files
        FileOutputStream f = null;
        JarOutputStream out = null;
        String repoUrl = super.repository.getBasedir();
        try
        {
            String dirs = filename.replace( '-', '/' );
            //String[] split1 = repoUrl.split( "file:/" );
            //split1[1] = split1[1] + "/";

            //create the group level directory of the artifact    
            File dirFiles = new File( repoUrl + relativePath + dirs );

            if ( dirFiles.mkdirs() )
            {

                // create a jar file
                f = new FileOutputStream( repoUrl + relativePath + dirs + "/" + filename + "." + type );
                out = new JarOutputStream( new BufferedOutputStream( f ) );

                // jar sample.txt
                String filename1 = repoUrl + relativePath + dirs + "/sample.txt";
                boolean bool = createSampleFile( filename1 );

                BufferedReader in = new BufferedReader( new FileReader( filename1 ) );
                out.putNextEntry( new JarEntry( filename1 ) );
                int c;
                while ( ( c = in.read() ) != -1 )
                {
                    out.write( c );
                }
                in.close();
                out.close();

                //Create md5 and sha-1 checksum files..
                byte[] md5chk = createChecksum( repoUrl + relativePath + dirs + "/" + filename + "." + type, "MD5" );
                byte[] sha1chk = createChecksum( repoUrl + relativePath + dirs + "/" + filename + "." + type, "SHA-1" );
                System.out.println( "----- CREATED MD5 checksum ::: " + byteArrayToHexStr( md5chk ) );
                System.out.println( "----- CREATED SHA-1 checksum ::: " + byteArrayToHexStr( sha1chk ) );

                File file = null;

                if ( md5chk != null )
                {
                    file = new File( repoUrl + relativePath + dirs + "/" + filename + "." + type + ".md5" );
                    OutputStream os = new FileOutputStream( file );
                    OutputStreamWriter osw = new OutputStreamWriter( os );
                    if ( !isValid )
                        osw.write( byteArrayToHexStr( md5chk ) + "1" );
                    else
                        osw.write( byteArrayToHexStr( md5chk ) );
                    osw.close();
                }

                if ( sha1chk != null )
                {
                    file = new File( repoUrl + relativePath + dirs + "/" + filename + "." + type + ".sha1" );
                    OutputStream os = new FileOutputStream( file );
                    OutputStreamWriter osw = new OutputStreamWriter( os );
                    if ( !isValid )
                        osw.write( byteArrayToHexStr( sha1chk ) + "2" );
                    else
                        osw.write( byteArrayToHexStr( sha1chk ) );
                    osw.close();
                }
            }
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }

    /**
     * Create metadata file together with its checksums.
     * @param relativePath The groupId
     * @param filename The filename of the artifact to be created.
     * @param type The file type (JAR)
     * @param isValid Indicates whether the checksum to be created is valid or not.
     * @return
     */
    private boolean writeMetadataFile( String relativePath, String filename, String type, boolean isValid )
    {
        System.out.println( " " );
        System.out.println( "========================= METADATA CHECKSUM ==================================" );
        try
        {
            //create checksum for the metadata file..
            String repoUrl = repository.getBasedir();
            //System.out.println("repoUrl ---->>> " + repoUrl);

            System.out.println( "REPO URL :::: " + repoUrl );
           // String[] split1 = repoUrl.split( "file:/" );
           // split1[1] = split1[1] + "/";

            String url = repository.getBasedir() + "/" + filename + "." + type;

            boolean copied = copyFile( url, repoUrl + relativePath + filename + "." + type );
            //FileUtils.copyFile( new File( url ), new File( repoUrl + relativePath + filename + "." + type ) );
            //System.out.println( "META FILE COPIED ---->>> " + copied );

            //Create md5 and sha-1 checksum files..
            byte[] md5chk = createChecksum( repoUrl + relativePath + filename + "." + type, "MD5" );
            byte[] sha1chk = createChecksum( repoUrl + relativePath + filename + "." + type, "SHA-1" );
            System.out.println( "----- CREATED MD5 checksum ::: " + byteArrayToHexStr( md5chk ) );
            System.out.println( "----- CREATED SHA-1 checksum ::: " + byteArrayToHexStr( sha1chk ) );

            File file = null;

            if ( md5chk != null )
            {
                file = new File( repoUrl + relativePath + filename + "." + type + ".md5" );
                OutputStream os = new FileOutputStream( file );
                OutputStreamWriter osw = new OutputStreamWriter( os );
                if ( !isValid )
                    osw.write( byteArrayToHexStr( md5chk ) + "1" );
                else
                    osw.write( byteArrayToHexStr( md5chk ) );
                osw.close();
            }

            if ( sha1chk != null )
            {
                file = new File( repoUrl + relativePath + filename + "." + type + ".sha1" );
                OutputStream os = new FileOutputStream( file );
                OutputStreamWriter osw = new OutputStreamWriter( os );
                if ( !isValid )
                    osw.write( byteArrayToHexStr( sha1chk ) + "2" );
                else
                    osw.write( byteArrayToHexStr( sha1chk ) );
                osw.close();
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Create the sample file that will be included in the jar.
     * @param filename
     * @return
     */
    private boolean createSampleFile( String filename )
    {
        try
        {
            File file = new File( filename );
            OutputStream os = new FileOutputStream( file );
            OutputStreamWriter osw = new OutputStreamWriter( os );
            osw.write( "This is the content of the sample file that will be included in the jar file." );
            osw.close();
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }

    /**
     * Create a checksum from the specified metadata file.
     * 
     * @param metadataUrl
     * @return
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private byte[] createChecksum( String filename, String algo )
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
    private String byteArrayToHexStr( byte[] data )
    {
        String output = "";
        String tempStr = "";
        int tempInt = 0;

        for ( int cnt = 0; cnt < data.length; cnt++ )
        {
            tempInt = data[cnt] & 0xFF;
            tempStr = Integer.toHexString( tempInt );

            if ( tempStr.length() == 1 )
                tempStr = "0" + tempStr;
            output = output + tempStr;
        }

        return output.toUpperCase();
    }

    /**
     * Copy created metadata file to the repository.
     * @param srcUrl
     * @param destUrl
     * @return
     */
    private boolean copyFile( String srcUrl, String destUrl )
    {
        try
        {
            //source file
            File src = new File( srcUrl );
            //destination file
            File dest = new File( destUrl );

            InputStream in = new FileInputStream( src );
            OutputStream out = new FileOutputStream( dest );

            byte[] buf = new byte[1024];
            int len;
            while ( ( len = in.read( buf ) ) > 0 )
            {
                out.write( buf, 0, len );
            }
            in.close();
            out.close();
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }

    /**
     * Delete the test directory created in the repository.
     * @param dirname The directory to be deleted.
     * @return
     */
    protected boolean deleteTestDirectory( File dir )
    {
        boolean b = false;

        if ( dir.isDirectory() == true )
        {
            if ( dir.listFiles().length > 0 )
            {
                File[] files = dir.listFiles();
                for ( int i = 0; i < files.length; i++ )
                {
                    b = this.deleteTestDirectory( files[i] );
                                        
                    //check if this is the last file in the directory
                    //delete the parent file
                    if((i == (files.length - 1)) && b == true){
                        String[] split = dir.getAbsolutePath().split("/repository");
                        if(!files[i].getParent().equals(split[0] + "/repository")){
                            b = this.deleteTestDirectory(new File(files[i].getParent()));
                        }
                    }
                }
                
            }else{
                b = dir.delete();
            }
        }
        else
        {
            b = dir.delete();
        }

        return b;
    }

}
