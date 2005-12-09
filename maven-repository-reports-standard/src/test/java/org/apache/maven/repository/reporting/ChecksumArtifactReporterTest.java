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
import java.util.Iterator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.versioning.VersionRange;

/** 
 * This class tests the ChecksumArtifactReporter. 
 * It extends the AbstractChecksumArtifactReporterTest class.
 */
public class ChecksumArtifactReporterTest
    extends AbstractChecksumArtifactReporterTest
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactReporter reporter = new MockArtifactReporter();

    private MetadataReportProcessor metadataReportProcessor;

    private static final String remoteRepoUrl = "http://public.planetmirror.com/pub/maven2/";

    private static final String remoteArtifactGroup = "HTTPClient";

    private static final String remoteArtifactId = "HTTPClient";

    private static final String remoteArtifactVersion = "0.3-3";

    private static final String remoteArtifactScope = "compile";

    private static final String remoteArtifactType = "jar";

    private static final String remoteRepoId = "remote-repo";

    public ChecksumArtifactReporterTest()
    {

    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "default" );
        metadataReportProcessor = (MetadataReportProcessor) lookup( MetadataReportProcessor.ROLE, "checksum-metadata" );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Test creation of artifact with checksum files.
     */
    public void testCreateChecksumFile()
    {
        assertTrue( createChecksumFile( "VALID" ) );
        assertTrue( createChecksumFile( "INVALID" ) );
    }

    /**
     * Test creation of metadata file together with its checksums.
     */
    public void testCreateMetadataFile()
    {
        assertTrue( createMetadataFile( "VALID" ) );
        assertTrue( createMetadataFile( "INVALID" ) );
    }

    /**
     * Test the ChecksumArtifactReporter when the checksum files are valid.
     */
    public void testChecksumArtifactReporterSuccess()
    {
        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "",
                                                     handler );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", "file:/"
                + System.getProperty( "basedir" ) + "/src/test/repository/", new DefaultRepositoryLayout() );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            Iterator iter = reporter.getArtifactSuccessIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            //System.out.println( "ARTIFACT Number of success --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the ChecksumArtifactReporter when the checksum files are invalid.
     */
    public void testChecksumArtifactReporterFailed()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "",
                                                     handler );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", "file:/"
                + System.getProperty( "basedir" ) + "/src/test/repository/", new DefaultRepositoryLayout() );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );

            Iterator iter = reporter.getArtifactFailureIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            //System.out.println( "ARTIFACT Number of failures --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the valid checksum of a metadata file. 
     * The reporter should report 2 success validation.
     */
    public void testChecksumMetadataReporterSuccess()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", "file:/"
                + System.getProperty( "basedir" ) + "/src/test/repository/", new DefaultRepositoryLayout() );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "",
                                                     handler );

        //Version level metadata
            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );            
            
       //Artifact level metadata
            metadata = new ArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );
            
            
       //Group level metadata
            metadata = new GroupRepositoryMetadata( "checksumTest" );

            metadataReportProcessor.processMetadata( metadata, repository, reporter );
            Iterator iter = reporter.getRepositoryMetadataSuccessIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                RepositoryMetadataResult result = (RepositoryMetadataResult) iter.next();
                ctr++;
            }
            //System.out.println( "METADATA Number of success --- " + ctr );
            
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the corrupted checksum of a metadata file. 
     * The reporter must report 2 failures.
     */
    public void testChecksumMetadataReporterFailure()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", "file:/"
                + System.getProperty( "basedir" ) + "/src/test/repository/", new DefaultRepositoryLayout() );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "",
                                                     handler );

            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );

            Iterator iter = reporter.getRepositoryMetadataFailureIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                RepositoryMetadataResult result = (RepositoryMetadataResult) iter.next();
                ctr++;
            }
            //System.out.println( "METADATA Number of failures --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the checksum of an artifact located in a remote location.
     */
    public void testChecksumArtifactRemote()
    {
        ArtifactHandler handler = new DefaultArtifactHandler( remoteArtifactType );
        VersionRange version = VersionRange.createFromVersion( remoteArtifactVersion );
        Artifact artifact = new DefaultArtifact( remoteArtifactGroup, remoteArtifactId, version, remoteArtifactScope,
                                                 remoteArtifactType, "", handler );
        ArtifactRepository repository = new DefaultArtifactRepository( remoteRepoId, remoteRepoUrl,
                                                                       new DefaultRepositoryLayout() );

        artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
        Iterator iter = reporter.getArtifactSuccessIterator();
        int ctr = 0;
        while ( iter.hasNext() )
        {
            ArtifactResult result = (ArtifactResult) iter.next();
            ctr++;
        }
        //System.out.println( "[REMOTE] ARTIFACT Number of success --- " + ctr );
    }

    /**
     * Test the checksum of a metadata file located in a remote location.
     */
    public void testChecksumMetadataRemote()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( remoteArtifactType );
            VersionRange version = VersionRange.createFromVersion( remoteArtifactVersion );
            Artifact artifact = new DefaultArtifact( remoteArtifactGroup, remoteArtifactId, version,
                                                     remoteArtifactScope, remoteArtifactType, "", handler );
            ArtifactRepository repository = new DefaultArtifactRepository( remoteRepoId, remoteRepoUrl,
                                                                           new DefaultRepositoryLayout() );
       
            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );

            metadataReportProcessor.processMetadata( metadata, repository, reporter );
            Iterator iter = reporter.getRepositoryMetadataFailureIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                RepositoryMetadataResult result = (RepositoryMetadataResult) iter.next();
                ctr++;
            }
            //System.out.println( "[REMOTE] METADATA in version directory, Number of sucess --- " + ctr );
      
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test deletion of the test directories created.
     */
    public void testDeleteChecksumFiles()
    {
        //System.out.println("TESTDELETE CHECKSUM FILES....." + deleteChecksumFiles("jar"));
        
        assertTrue( deleteChecksumFiles("jar"));
    }
    
    /**
     * Test deletion of the test directories created.
     */
    public void testDeleteTestDirectory()
    {   
        assertTrue( deleteTestDirectory( new File( repository.getBasedir() + "checksumTest" ) ) );
    }

    /**
     * Test the conditional when the checksum files of the artifact & metadata do not exist.
     */
    public void testChecksumFilesDoNotExist(){
        createChecksumFile("VALID");
        createMetadataFile("VALID");
        boolean b = deleteChecksumFiles("jar");   
        
        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "",
                                                     handler );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", "file:/"
                + System.getProperty( "basedir" ) + "/src/test/repository/", new DefaultRepositoryLayout() );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            Iterator iter = reporter.getArtifactFailureIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            //System.out.println( "[FILE DOES NOT EXIST] ARTIFACT Number of failures --- " + ctr );
           
            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );

            iter = reporter.getRepositoryMetadataFailureIterator();
            ctr = 0;
            while ( iter.hasNext() )
            {
                RepositoryMetadataResult result = (RepositoryMetadataResult) iter.next();
                ctr++;
            }
            //System.out.println( "[FILE DOES NOT EXIST] METADATA Number of failures --- " + ctr );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }        
        
        b = deleteTestDirectory( new File( repository.getBasedir() + "checksumTest" ) );
    }
}
