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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * @TODO 
 *  - Test with multiple success and multiple failures
 *  - Test using remote repository
 * 
 * This class tests the ChecksumArtifactReporter. 
 * It extends the AbstractChecksumArtifactReporterTest class.
 */
public class ChecksumArtifactReporterTest
    extends AbstractChecksumArtifactReporterTest
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactReporter reporter = new MockArtifactReporter();

    private MetadataReportProcessor metadataReportProcessor;

    public ChecksumArtifactReporterTest()
    {

    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "default" );
        metadataReportProcessor = (MetadataReportProcessor) lookup( MetadataReportProcessor.ROLE, "checksum-metadata" );

        // boolean b = createChecksumFile( "VALID" );
        // b = createChecksumFile( "INVALID" );
        // b = createMetadataFile( "VALID" );
        // b = createMetadataFile( "INVALID" );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
        //String[] split = super.repository.getUrl().split("file:/");
        //boolean b = deleteTestDirectory(new File(split[1] + "checksumTest") );
    }

    /**
     * Test creation of artifact with checksum files.
     *
     */
    public void testCreateChecksumFile()
    {
        assertTrue( createChecksumFile( "VALID" ) );
        assertTrue( createChecksumFile( "INVALID" ) );
    }

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
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", System.getProperty( "basedir" )
                + "/src/test/repository/", new DefaultRepositoryLayout() );

            /*  VersionRange version = VersionRange.createFromVersion("0.3-3");
             Artifact artifact = new DefaultArtifact("HTTPClient", "HTTPClient", version, "compile", "jar", "", 
             handler);
             ArtifactRepository repository = new DefaultArtifactRepository("remote-repo", "http://www.ibiblio.org/maven2/", 
             new DefaultRepositoryLayout());
             */

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );

            Iterator iter = reporter.getArtifactSuccessIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            System.out.println( "ARTIFACT Number of success --- " + ctr );

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
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", System.getProperty( "basedir" )
                + "/src/test/repository/", new DefaultRepositoryLayout() );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );

            Iterator iter = reporter.getArtifactFailureIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            System.out.println( "ARTIFACT Number of failures --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public void testChecksumMetadataReporterSuccess()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", System.getProperty( "basedir" )
                + "/src/test/repository/", new DefaultRepositoryLayout() );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "",
                                                     handler );

            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );

            Iterator iter = reporter.getRepositoryMetadataSuccessIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                RepositoryMetadataResult result = (RepositoryMetadataResult) iter.next();
                ctr++;
            }
            System.out.println( "REPORT METADATA Number of success --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public void testChecksumMetadataReporterFailure()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", System.getProperty( "basedir" )
                + "/src/test/repository/", new DefaultRepositoryLayout() );
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
            System.out.println( "REPORT METADATA Number of failures --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
    
   
    public void testDeleteTestDirectory(){        
        String[] split = super.repository.getUrl().split("file:/");
        assertTrue(deleteTestDirectory(new File(split[1] + "checksumTest") ));
    }    

}
