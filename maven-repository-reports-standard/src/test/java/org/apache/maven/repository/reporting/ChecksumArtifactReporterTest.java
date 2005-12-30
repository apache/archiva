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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.util.Iterator;

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

    public ChecksumArtifactReporterTest()
    {

    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "checksum" );
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
            Artifact artifact =
                new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "", handler );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            assertTrue( reporter.getSuccesses() == 2 );
            //System.out.println( "1 - SUCCESS ---> " + reporter.getSuccesses() );

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
            Artifact artifact =
                new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "", handler );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            assertTrue( reporter.getFailures() == 2 );
            //System.out.println( "2 - FAILURES ---> " + reporter.getFailures() );
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
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact =
                new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "", handler );

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
            //System.out.println( "3 - META SUCCESS ---> " + iter.hasNext() );
            assertTrue( "check if there is a success", iter.hasNext() );

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
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact =
                new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "", handler );

            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );

            //System.out.println("reporter.getFailures() ---> " + reporter.getFailures());

            Iterator iter = reporter.getRepositoryMetadataFailureIterator();
            //System.out.println( "4 - META FAILURE ---> " + iter.hasNext() );
            assertTrue( "check if there is a failure", iter.hasNext() );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the checksum of an artifact located in a remote location.
     */
    /*   public void testChecksumArtifactRemote()
    {
        ArtifactHandler handler = new DefaultArtifactHandler( remoteArtifactType );
        VersionRange version = VersionRange.createFromVersion( remoteArtifactVersion );
        Artifact artifact = new DefaultArtifact( remoteArtifactGroup, remoteArtifactId, version, remoteArtifactScope,
                                                 remoteArtifactType, "", handler );
        ArtifactRepository repository = new DefaultArtifactRepository( remoteRepoId, remoteRepoUrl,
                                                                       new DefaultRepositoryLayout() );

        artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
        if ( reporter.getFailures() == 2 )
            assertTrue( reporter.getFailures() == 2 );

        if ( reporter.getSuccesses() == 2 )
            assertTrue( reporter.getSuccesses() == 2 );

    }
    */

    /**
     * Test the checksum of a metadata file located in a remote location.
     */
    /*   public void testChecksumMetadataRemote()
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
            if ( iter.hasNext() )
                assertTrue( "check if there is a failure", iter.hasNext() );

            iter = reporter.getRepositoryMetadataSuccessIterator();
            if ( iter.hasNext() )
                assertTrue( "check if there is a success", iter.hasNext() );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
    */

    /**
     * Test deletion of the test directories created.
     */
    public void testDeleteChecksumFiles()
    {
        assertTrue( deleteChecksumFiles( "jar" ) );
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
    public void testChecksumFilesDoNotExist()
    {
        createChecksumFile( "VALID" );
        createMetadataFile( "VALID" );
        boolean b = deleteChecksumFiles( "jar" );

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact =
                new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "", handler );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            //System.out.println( "5 - ART FAILURE ---> " + reporter.getFailures() );
            assertTrue( reporter.getFailures() == 2 );

            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadataReportProcessor.processMetadata( metadata, repository, reporter );

            Iterator iter = reporter.getRepositoryMetadataFailureIterator();
            //System.out.println( "5 - META FAILURE ---> " + iter.hasNext() );
            assertTrue( "check if there is a failure", iter.hasNext() );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        b = deleteTestDirectory( new File( repository.getBasedir() + "checksumTest" ) );
    }
}
