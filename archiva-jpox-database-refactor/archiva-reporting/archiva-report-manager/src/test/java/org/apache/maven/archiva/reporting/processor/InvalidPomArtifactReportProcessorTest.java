package org.apache.maven.archiva.reporting.processor;

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

import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
import org.apache.maven.artifact.Artifact;

/**
 * This class tests the InvalidPomArtifactReportProcessor class.
 */
public class InvalidPomArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactResultsDatabase database;

    public void setUp()
        throws Exception
    {
        super.setUp();
        database = (ArtifactResultsDatabase) lookup( ArtifactResultsDatabase.ROLE );
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "invalid-pom" );
    }

    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is an invalid pom.
     */
    public void testInvalidPomArtifactReportProcessorFailure()
    {
        Artifact artifact = createArtifact( "org.apache.maven", "artifactId", "1.0-alpha-3", "pom" );

        artifactReportProcessor.processArtifact( artifact, null );
        assertEquals( 1, database.getNumFailures() );
    }


    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is a valid pom.
     */
    public void testInvalidPomArtifactReportProcessorSuccess()
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-2", "pom" );

        artifactReportProcessor.processArtifact( artifact, null );
        assertEquals( 0, database.getNumFailures() );
        assertEquals( 0, database.getNumWarnings() );
        assertEquals( "Check no notices", 0, database.getNumNotices() );
    }


    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is not a pom.
     */
    public void testNotAPomArtifactReportProcessorSuccess()
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "jar" );

        artifactReportProcessor.processArtifact( artifact, null );
        assertEquals( 0, database.getNumFailures() );
        assertEquals( 0, database.getNumWarnings() );
        assertEquals( "Check no notices", 0, database.getNumNotices() );
    }
}
