package org.apache.maven.archiva.reporting.reporter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
import org.apache.maven.archiva.reporting.database.MetadataResultsDatabase;
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor;
import org.apache.maven.archiva.reporting.processor.MetadataReportProcessor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.codehaus.plexus.digest.DigesterException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * ChecksumMetadataReporterTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ChecksumMetadataReporterTest
    extends AbstractChecksumArtifactReporterTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private MetadataReportProcessor metadataReportProcessor;

    private MetadataResultsDatabase database;

    private ArtifactResultsDatabase artifactsDatabase;

    public void setUp()
        throws Exception
    {
        super.setUp();
        metadataReportProcessor = (MetadataReportProcessor) lookup( MetadataReportProcessor.ROLE, "checksum-metadata" );
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "checksum" );
        database = (MetadataResultsDatabase) lookup( MetadataResultsDatabase.ROLE, "default" );
        artifactsDatabase = (ArtifactResultsDatabase) lookup( ArtifactResultsDatabase.ROLE, "default" );
    }

    /**
     * Test the valid checksum of a metadata file.
     * The reportingDatabase should report 2 success validation.
     */
    public void testChecksumMetadataReporterSuccess()
        throws DigesterException, IOException
    {
        createMetadataFile( "VALID" );
        createMetadataFile( "INVALID" );

        Artifact artifact = createArtifact( "checksumTest", "validArtifact", "1.0" );

        //Version level metadata
        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository );

        //Artifact level metadata
        metadata = new ArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository );

        //Group level metadata
        metadata = new GroupRepositoryMetadata( "checksumTest" );
        metadataReportProcessor.processMetadata( metadata, repository );
    }

    /**
     * Test the corrupted checksum of a metadata file.
     * The reportingDatabase must report 2 failures.
     */
    public void testChecksumMetadataReporterFailure()
    {
        Artifact artifact = createArtifact( "checksumTest", "invalidArtifact", "1.0" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
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

        artifactReportProcessor.processArtifact( artifact, null );
        assertEquals( 1, artifactsDatabase.getNumFailures() );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        metadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );

        deleteTestDirectory( new File( repository.getBasedir() + "checksumTest" ) );
    }

}
