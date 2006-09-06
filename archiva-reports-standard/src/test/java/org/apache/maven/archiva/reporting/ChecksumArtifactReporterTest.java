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

import org.apache.maven.archiva.digest.DigesterException;
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
import java.io.IOException;
import java.util.Iterator;

/**
 * This class tests the ChecksumArtifactReportProcessor.
 * It extends the AbstractChecksumArtifactReporterTestCase class.
 */
public class ChecksumArtifactReporterTest
    extends AbstractChecksumArtifactReporterTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactReporter reporter = new DefaultArtifactReporter();

    private MetadataReportProcessor metadataReportProcessor;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "checksum" );
        metadataReportProcessor = (MetadataReportProcessor) lookup( MetadataReportProcessor.ROLE, "checksum-metadata" );
    }

    /**
     * Test the ChecksumArtifactReportProcessor when the checksum files are valid.
     */
    public void testChecksumArtifactReporterSuccess()
        throws ReportProcessorException, IOException, DigesterException
    {
        createChecksumFile( "VALID" );
        createChecksumFile( "INVALID" );

        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        Artifact artifact =
            new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "", handler );

        artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
        assertEquals( 2, reporter.getNumSuccesses() );
    }

    /**
     * Test the ChecksumArtifactReportProcessor when the checksum files are invalid.
     */
    public void testChecksumArtifactReporterFailed()
        throws ReportProcessorException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        Artifact artifact =
            new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "", handler );

        artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
        assertEquals( 2, reporter.getNumFailures() );
    }

    /**
     * Test the valid checksum of a metadata file.
     * The reporter should report 2 success validation.
     */
    public void testChecksumMetadataReporterSuccess()
        throws ReportProcessorException, DigesterException, IOException
    {
        createMetadataFile( "VALID" );
        createMetadataFile( "INVALID" );

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
        assertTrue( "check if there is a success", iter.hasNext() );
    }

    /**
     * Test the corrupted checksum of a metadata file.
     * The reporter must report 2 failures.
     */
    public void testChecksumMetadataReporterFailure()
        throws ReportProcessorException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        Artifact artifact =
            new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "", handler );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator iter = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check if there is a failure", iter.hasNext() );
    }

    /**
     * Test the conditional when the checksum files of the artifact & metadata do not exist.
     */
    public void testChecksumFilesDoNotExist()
        throws ReportProcessorException, DigesterException, IOException
    {
        createChecksumFile( "VALID" );
        createMetadataFile( "VALID" );
        deleteChecksumFiles( "jar" );

        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        Artifact artifact =
            new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "", handler );

        artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
        assertEquals( 2, reporter.getNumFailures() );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator iter = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check if there is a failure", iter.hasNext() );

        deleteTestDirectory( new File( repository.getBasedir() + "checksumTest" ) );
    }
}
