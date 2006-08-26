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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.repository.indexing.RepositoryArtifactIndex;
import org.apache.maven.repository.indexing.RepositoryArtifactIndexFactory;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecordFactory;
import org.codehaus.plexus.util.FileUtils;

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

    protected void setUp()
        throws Exception
    {
        super.setUp();

        indexDirectory = getTestFile( "target/indexDirectory" );
        FileUtils.deleteDirectory( indexDirectory );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "jar" );
        model = new Model();

        RepositoryArtifactIndexFactory factory =
            (RepositoryArtifactIndexFactory) lookup( RepositoryArtifactIndexFactory.ROLE, "lucene" );

        RepositoryArtifactIndex index = factory.createStandardIndex( indexDirectory );

        RepositoryIndexRecordFactory recordFactory =
            (RepositoryIndexRecordFactory) lookup( RepositoryIndexRecordFactory.ROLE, "standard" );

        index.indexRecords( Collections.singletonList( recordFactory.createRecord( artifact ) ) );

        processor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "duplicate" );
    }

    public void testNullArtifactFile()
        throws Exception
    {
        artifact.setFile( null );

        MockArtifactReporter reporter = new MockArtifactReporter();

        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 0, reporter.getSuccesses() );
        assertEquals( "Check warnings", 1, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testSuccessOnAlreadyIndexedArtifact()
        throws Exception
    {
        MockArtifactReporter reporter = new MockArtifactReporter();

        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testSuccessOnDifferentGroupId()
        throws Exception
    {
        MockArtifactReporter reporter = new MockArtifactReporter();

        artifact.setGroupId( "different.groupId" );
        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testSuccessOnNewArtifact()
        throws Exception
    {
        Artifact newArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "pom" );

        MockArtifactReporter reporter = new MockArtifactReporter();

        processor.processArtifact( model, newArtifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testFailure()
        throws Exception
    {
        Artifact duplicate = createArtifact( artifact.getGroupId(), "snapshot-artifact", "1.0-alpha-1-SNAPSHOT",
                                             artifact.getVersion(), artifact.getType() );
        duplicate.setFile( artifact.getFile() );

        MockArtifactReporter reporter = new MockArtifactReporter();

        processor.processArtifact( model, duplicate, reporter, repository );

        assertEquals( "Check no successes", 0, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 1, reporter.getFailures() );
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
