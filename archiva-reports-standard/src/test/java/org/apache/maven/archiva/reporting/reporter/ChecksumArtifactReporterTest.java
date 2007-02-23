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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
import org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.digest.DigesterException;

import java.io.IOException;

/**
 * This class tests the ChecksumArtifactReportProcessor.
 * It extends the AbstractChecksumArtifactReporterTestCase class.
 */
public class ChecksumArtifactReporterTest
    extends AbstractChecksumArtifactReporterTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactResultsDatabase database;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "checksum" );
        database = (ArtifactResultsDatabase) lookup( ArtifactResultsDatabase.ROLE );
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

        artifactReportProcessor.processArtifact( artifact, null );
        assertEquals( 0, database.getNumFailures() );
        assertEquals( 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );
    }

    /**
     * Test the ChecksumArtifactReportProcessor when the checksum files are invalid.
     */
    public void testChecksumArtifactReporterFailed()
    {
        String s = "invalidArtifact";
        String s1 = "1.0";
        Artifact artifact = createArtifact( "checksumTest", s, s1 );

        artifactReportProcessor.processArtifact( artifact, null );
        assertEquals( 1, database.getNumFailures() );
        assertEquals( 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );
    }
}
