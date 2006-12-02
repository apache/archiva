package org.apache.maven.archiva.reporting.reporter;

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

import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * This class creates the artifact and metadata files used for testing the ChecksumArtifactReportProcessor.
 * It is extended by ChecksumArtifactReporterTest class.
 */
public abstract class AbstractChecksumArtifactReporterTestCase
    extends AbstractRepositoryReportsTestCase
{
    private static final String[] validArtifactChecksumJars = {"validArtifact-1.0"};

    private static final String[] invalidArtifactChecksumJars = {"invalidArtifact-1.0"};

    private static final String metadataChecksumFilename = "maven-metadata";

    private Digester sha1Digest;

    private Digester md5Digest;

    public void setUp()
        throws Exception
    {
        super.setUp();

        sha1Digest = (Digester) lookup( Digester.ROLE, "sha1" );
        md5Digest = (Digester) lookup( Digester.ROLE, "md5" );
    }

    /**
     * Create checksum files.
     *
     * @param type The type of checksum file to be created.
     */
    protected void createChecksumFile( String type )
        throws DigesterException, IOException
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
        throws DigesterException, IOException
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
        throws IOException, DigesterException
    {
        //Initialize variables for creating jar files
        String repoUrl = repository.getBasedir();

        String dirs = filename.replace( '-', '/' );
        //create the group level directory of the artifact
        File dirFiles = new File( repoUrl + relativePath + dirs );

        if ( dirFiles.mkdirs() )
        {
            // create a jar file
            String path = repoUrl + relativePath + dirs + "/" + filename + "." + type;
            FileOutputStream f = new FileOutputStream( path );
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

            File file = new File( path + ".md5" );
            OutputStream os = new FileOutputStream( file );
            OutputStreamWriter osw = new OutputStreamWriter( os );
            String sum = md5Digest.calc( new File( path ) );
            if ( !isValid )
            {
                osw.write( sum + "1" );
            }
            else
            {
                osw.write( sum );
            }
            osw.close();

            file = new File( path + ".sha1" );
            os = new FileOutputStream( file );
            osw = new OutputStreamWriter( os );
            String sha1sum = sha1Digest.calc( new File( path ) );
            if ( !isValid )
            {
                osw.write( sha1sum + "2" );
            }
            else
            {
                osw.write( sha1sum );
            }
            osw.close();
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
        throws IOException, DigesterException
    {
        //create checksum for the metadata file..
        String repoUrl = repository.getBasedir();
        String url = repository.getBasedir() + "/" + filename + "." + type;

        String path = repoUrl + relativePath + filename + "." + type;
        FileUtils.copyFile( new File( url ), new File( path ) );

        //Create md5 and sha-1 checksum files..
        File file = new File( path + ".md5" );
        OutputStream os = new FileOutputStream( file );
        OutputStreamWriter osw = new OutputStreamWriter( os );
        String md5sum = md5Digest.calc( new File( path ) );
        if ( !isValid )
        {
            osw.write( md5sum + "1" );
        }
        else
        {
            osw.write( md5sum );
        }
        osw.close();

        file = new File( path + ".sha1" );
        os = new FileOutputStream( file );
        osw = new OutputStreamWriter( os );
        String sha1sum = sha1Digest.calc( new File( path ) );
        if ( !isValid )
        {
            osw.write( sha1sum + "2" );
        }
        else
        {
            osw.write( sha1sum );
        }
        osw.close();
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
     * Delete the test directory created in the repository.
     *
     * @param dir The directory to be deleted.
     */
    protected void deleteTestDirectory( File dir )
    {
        try
        {
            FileUtils.deleteDirectory( dir );
        }
        catch ( IOException e )
        {
            // ignore
        }
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
