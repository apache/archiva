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

import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
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

    private DefaultArtifactReporter reporter = new DefaultArtifactReporter();

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

        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 0, reporter.getNumSuccesses() );
        assertEquals( "Check warnings", 1, reporter.getNumWarnings() );
        assertEquals( "Check no failures", 0, reporter.getNumFailures() );
    }

    public void testSuccessOnAlreadyIndexedArtifact()
        throws Exception
    {
        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getNumSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getNumWarnings() );
        assertEquals( "Check no failures", 0, reporter.getNumFailures() );
    }

    public void testSuccessOnDifferentGroupId()
        throws Exception
    {
        artifact.setGroupId( "different.groupId" );
        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getNumSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getNumWarnings() );
        assertEquals( "Check no failures", 0, reporter.getNumFailures() );
    }

    public void testSuccessOnNewArtifact()
        throws Exception
    {
        Artifact newArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "pom" );

        processor.processArtifact( model, newArtifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getNumSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getNumWarnings() );
        assertEquals( "Check no failures", 0, reporter.getNumFailures() );
    }

    public void testFailure()
        throws Exception
    {
        Artifact duplicate = createArtifact( artifact.getGroupId(), "snapshot-artifact", "1.0-alpha-1-SNAPSHOT",
                                             artifact.getVersion(), artifact.getType() );
        duplicate.setFile( artifact.getFile() );

        processor.processArtifact( model, duplicate, reporter, repository );

        assertEquals( "Check no successes", 0, reporter.getNumSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getNumWarnings() );
        assertEquals( "Check no failures", 1, reporter.getNumFailures() );
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
