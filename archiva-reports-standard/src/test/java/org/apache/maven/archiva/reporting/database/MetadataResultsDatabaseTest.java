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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * MetadataResultsDatabaseTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MetadataResultsDatabaseTest
    extends AbstractRepositoryReportsTestCase
{
    private MetadataResultsDatabase database;

    private RepositoryMetadata metadata;

    private String processor, problem, reason;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        database = (MetadataResultsDatabase) lookup( MetadataResultsDatabase.ROLE, "default" );

        Artifact artifact = new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1.0" ), "scope",
                                                 "type", "classifier", null );
        metadata = new ArtifactRepositoryMetadata( artifact );

        processor = "processor";
        problem = "problem";
        reason = "reason";
    }

    protected void tearDown()
        throws Exception
    {
        release( database );

        super.tearDown();
    }

    public void testAddNoticeRepositoryMetadataStringStringString()
    {
        database.addNotice( metadata, processor, problem, reason );
        MetadataResults metadataResults = database.getMetadataResults( metadata );

        assertEquals( 1, database.getNumNotices() );
        assertEquals( 1, metadataResults.getNotices().size() );

        database.addNotice( metadata, processor, problem, reason );
        metadataResults = database.getMetadataResults( metadata );

        assertEquals( 1, database.getNumNotices() );
        assertEquals( 1, metadataResults.getNotices().size() );
    }

    public void testAddWarningRepositoryMetadataStringStringString()
    {
        database.addWarning( metadata, processor, problem, reason );
        MetadataResults metadataResults = database.getMetadataResults( metadata );

        assertEquals( 1, database.getNumWarnings() );
        assertEquals( 1, metadataResults.getWarnings().size() );

        database.addWarning( metadata, processor, problem, reason );
        metadataResults = database.getMetadataResults( metadata );

        assertEquals( 1, database.getNumWarnings() );
        assertEquals( 1, metadataResults.getWarnings().size() );
    }

    public void testAddFailureRepositoryMetadataStringStringString()
    {
        database.addFailure( metadata, processor, problem, reason );
        MetadataResults metadataResults = database.getMetadataResults( metadata );

        assertEquals( 1, database.getNumFailures() );
        assertEquals( 1, metadataResults.getFailures().size() );

        database.addFailure( metadata, processor, problem, reason );
        metadataResults = database.getMetadataResults( metadata );

        assertEquals( 1, database.getNumFailures() );
        assertEquals( 1, metadataResults.getFailures().size() );
    }
}
