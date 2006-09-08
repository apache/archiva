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
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;

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

    private ReportingDatabase reporter = new ReportingDatabase();

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
        throws DigesterException, IOException
    {
        createChecksumFile( "VALID" );
        createChecksumFile( "INVALID" );

        Artifact artifact = createArtifact( "checksumTest", "validArtifact", "1.0" );

        artifactReportProcessor.processArtifact( artifact, null, reporter );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    /**
     * Test the ChecksumArtifactReportProcessor when the checksum files are invalid.
     */
    public void testChecksumArtifactReporterFailed()
    {
        String s = "invalidArtifact";
        String s1 = "1.0";
        Artifact artifact = createArtifact( "checksumTest", s, s1 );

        artifactReportProcessor.processArtifact( artifact, null, reporter );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    /**
     * Test the valid checksum of a metadata file.
     * The reporter should report 2 success validation.
     */
    public void testChecksumMetadataReporterSuccess()
        throws DigesterException, IOException
    {
        createMetadataFile( "VALID" );
        createMetadataFile( "INVALID" );

        Artifact artifact = createArtifact( "checksumTest", "validArtifact", "1.0" );

        //Version level metadata
        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );

        //Artifact level metadata
        metadata = new ArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );

        //Group level metadata
        metadata = new GroupRepositoryMetadata( "checksumTest" );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );
    }

    /**
     * Test the corrupted checksum of a metadata file.
     * The reporter must report 2 failures.
     */
    public void testChecksumMetadataReporterFailure()
    {
        Artifact artifact = createArtifact( "checksumTest", "invalidArtifact", "1.0" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getMetadataIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
    }

    /**
     * Test the conditional when the checksum files of the artifact & metadata do not exist.
     */
    public void testChecksumFilesDoNotExist()
        throws DigesterException, IOException
    {
        createChecksumFile( "VALID" );
        createMetadataFile( "VALID" );
        deleteChecksumFiles( "jar" );

        Artifact artifact = createArtifact( "checksumTest", "validArtifact", "1.0" );

        artifactReportProcessor.processArtifact( artifact, null, reporter );
        assertEquals( 1, reporter.getNumFailures() );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getMetadataIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );

        deleteTestDirectory( new File( repository.getBasedir() + "checksumTest" ) );
    }
}
