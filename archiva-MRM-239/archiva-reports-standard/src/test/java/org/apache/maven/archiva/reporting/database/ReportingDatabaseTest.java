package org.apache.maven.archiva.reporting.database;

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

import junit.framework.TestCase;
import org.apache.maven.archiva.reporting.model.ArtifactResults;
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Test for {@link ReportingDatabase}.
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class ReportingDatabaseTest
    extends TestCase
{
    private Artifact artifact;

    private String processor, problem, reason;

    private ReportingDatabase reportingDatabase;

    private RepositoryMetadata metadata;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        artifact = new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1.0" ), "scope", "type",
                                        "classifier", null );
        processor = "processor";
        problem = "problem";
        reason = "reason";
        reportingDatabase = new ReportingDatabase( null );

        metadata = new ArtifactRepositoryMetadata( artifact );
    }

    public void testAddNoticeArtifactStringStringString()
    {
        reportingDatabase.addNotice( artifact, processor, problem, reason );
        ArtifactResults artifactResults = reportingDatabase.getArtifactResults( artifact );

        assertEquals( 1, reportingDatabase.getNumNotices() );
        assertEquals( 1, artifactResults.getNotices().size() );

        reportingDatabase.addNotice( artifact, processor, problem, reason );
        artifactResults = reportingDatabase.getArtifactResults( artifact );

        assertEquals( 1, reportingDatabase.getNumNotices() );
        assertEquals( 1, artifactResults.getNotices().size() );
    }

    public void testAddWarningArtifactStringStringString()
    {
        reportingDatabase.addWarning( artifact, processor, problem, reason );
        ArtifactResults artifactResults = reportingDatabase.getArtifactResults( artifact );

        assertEquals( 1, reportingDatabase.getNumWarnings() );
        assertEquals( 1, artifactResults.getWarnings().size() );

        reportingDatabase.addWarning( artifact, processor, problem, reason );
        artifactResults = reportingDatabase.getArtifactResults( artifact );

        assertEquals( 1, reportingDatabase.getNumWarnings() );
        assertEquals( 1, artifactResults.getWarnings().size() );
    }

    public void testAddFailureArtifactStringStringString()
    {
        reportingDatabase.addFailure( artifact, processor, problem, reason );
        ArtifactResults artifactResults = reportingDatabase.getArtifactResults( artifact );

        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 1, artifactResults.getFailures().size() );

        reportingDatabase.addFailure( artifact, processor, problem, reason );
        artifactResults = reportingDatabase.getArtifactResults( artifact );

        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 1, artifactResults.getFailures().size() );
    }

    public void testAddNoticeRepositoryMetadataStringStringString()
    {
        reportingDatabase.addNotice( metadata, processor, problem, reason );
        MetadataResults metadataResults = reportingDatabase.getMetadataResults( metadata, System.currentTimeMillis() );

        assertEquals( 1, reportingDatabase.getNumNotices() );
        assertEquals( 1, metadataResults.getNotices().size() );

        reportingDatabase.addNotice( metadata, processor, problem, reason );
        metadataResults = reportingDatabase.getMetadataResults( metadata, System.currentTimeMillis() );

        assertEquals( 1, reportingDatabase.getNumNotices() );
        assertEquals( 1, metadataResults.getNotices().size() );
    }

    public void testAddWarningRepositoryMetadataStringStringString()
    {
        reportingDatabase.addWarning( metadata, processor, problem, reason );
        MetadataResults metadataResults = reportingDatabase.getMetadataResults( metadata, System.currentTimeMillis() );

        assertEquals( 1, reportingDatabase.getNumWarnings() );
        assertEquals( 1, metadataResults.getWarnings().size() );

        reportingDatabase.addWarning( metadata, processor, problem, reason );
        metadataResults = reportingDatabase.getMetadataResults( metadata, System.currentTimeMillis() );

        assertEquals( 1, reportingDatabase.getNumWarnings() );
        assertEquals( 1, metadataResults.getWarnings().size() );
    }

    public void testAddFailureRepositoryMetadataStringStringString()
    {
        reportingDatabase.addFailure( metadata, processor, problem, reason );
        MetadataResults metadataResults = reportingDatabase.getMetadataResults( metadata, System.currentTimeMillis() );

        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 1, metadataResults.getFailures().size() );

        reportingDatabase.addFailure( metadata, processor, problem, reason );
        metadataResults = reportingDatabase.getMetadataResults( metadata, System.currentTimeMillis() );

        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 1, metadataResults.getFailures().size() );
    }
}
