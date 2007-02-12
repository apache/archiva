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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;

import java.io.File;
import java.util.Collections;

/**
 * @author Edwin Punzalan
 */
public class DuplicateArtifactFileReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private Artifact artifact;

    private Model model;

    private ArtifactReportProcessor processor;

    private ArtifactFactory artifactFactory;

    File indexDirectory;

    private ReportingDatabase reportDatabase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        indexDirectory = getTestFile( "target/indexDirectory" );
        FileUtils.deleteDirectory( indexDirectory );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "jar" );
        System.out.println( "artifact = " + artifact );
        model = new Model();

        RepositoryArtifactIndexFactory factory =
            (RepositoryArtifactIndexFactory) lookup( RepositoryArtifactIndexFactory.ROLE, "lucene" );

        RepositoryArtifactIndex index = factory.createStandardIndex( indexDirectory );

        RepositoryIndexRecordFactory recordFactory =
            (RepositoryIndexRecordFactory) lookup( RepositoryIndexRecordFactory.ROLE, "standard" );

        index.indexRecords( Collections.singletonList( recordFactory.createRecord( artifact ) ) );

        processor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "duplicate" );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "health" );
        reportDatabase = new ReportingDatabase( reportGroup );
    }

    public void testNullArtifactFile()
        throws Exception
    {
        artifact.setFile( null );

        processor.processArtifact( artifact, model, reportDatabase );

        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
        assertEquals( "Check warnings", 1, reportDatabase.getNumWarnings() );
        assertEquals( "Check no failures", 0, reportDatabase.getNumFailures() );
    }

    public void testSuccessOnAlreadyIndexedArtifact()
        throws Exception
    {
        processor.processArtifact( artifact, model, reportDatabase );

        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
        assertEquals( "Check warnings", 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no failures", 0, reportDatabase.getNumFailures() );
    }

    public void testSuccessOnDifferentGroupId()
        throws Exception
    {
        artifact.setGroupId( "different.groupId" );
        processor.processArtifact( artifact, model, reportDatabase );

        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
        assertEquals( "Check warnings", 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no failures", 0, reportDatabase.getNumFailures() );
    }

    public void testSuccessOnNewArtifact()
        throws Exception
    {
        Artifact newArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "pom" );

        processor.processArtifact( newArtifact, model, reportDatabase );

        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
        assertEquals( "Check warnings", 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no failures", 0, reportDatabase.getNumFailures() );
    }

    public void testFailure()
        throws Exception
    {
        Artifact duplicate = createArtifact( artifact.getGroupId(), "snapshot-artifact", "1.0-alpha-1-SNAPSHOT",
                                             artifact.getVersion(), artifact.getType() );
        duplicate.setFile( artifact.getFile() );

        processor.processArtifact( duplicate, model, reportDatabase );

        assertEquals( "Check warnings", 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
        assertEquals( "Check no failures", 1, reportDatabase.getNumFailures() );
    }

    private Artifact createArtifact( String groupId, String artifactId, String baseVersion, String version,
                                     String type )
    {
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, type );
        artifact.setBaseVersion( baseVersion );
        artifact.setRepository( repository );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        return artifact;
    }
}
