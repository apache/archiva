package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.util.Iterator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

/**
 *
 */
public class DefaultArtifactReporterTest
        extends AbstractRepositoryReportsTestCase
{
    private ArtifactReporter reporter;
    
    private Artifact artifact;
    
    private RepositoryMetadata metadata;
    
    public void testEmptyArtifactReporter()
    {
        assertEquals( "No failures", 0, reporter.getFailures() );
        assertEquals( "No warnings", 0, reporter.getWarnings() );
        assertEquals( "No successes", 0, reporter.getSuccesses() );
        assertFalse( "No artifact failures", reporter.getArtifactFailureIterator().hasNext() );
        assertFalse( "No artifact warnings", reporter.getArtifactWarningIterator().hasNext() );
        assertFalse( "No artifact successes", reporter.getArtifactSuccessIterator().hasNext() );
        assertFalse( "No metadata failures", reporter.getRepositoryMetadataFailureIterator().hasNext() );
        assertFalse( "No metadata warnings", reporter.getRepositoryMetadataWarningIterator().hasNext() );
        assertFalse( "No metadata successes", reporter.getRepositoryMetadataSuccessIterator().hasNext() );
    }
    
    public void testMetadataSingleFailure()
    {
        reporter.addFailure( metadata, "Single Failure Reason" );
        assertEquals( "failures count", 1, reporter.getFailures() );
        assertEquals( "warnings count", 0, reporter.getWarnings() );
        assertEquals( "successes count", 0, reporter.getSuccesses() );

        Iterator results = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "must have failures", results.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) results.next();
        assertEquals( "check failure cause", metadata, result.getMetadata() );
        assertEquals( "check failure reason", "Single Failure Reason", result.getReason() );
        assertFalse( "no more failures", results.hasNext() );
    }
    
    public void testMetadataMultipleFailures()
    {
        reporter.addFailure( metadata, "First Failure Reason" );
        reporter.addFailure( metadata, "Second Failure Reason" );
        assertEquals( "failures count", 2, reporter.getFailures() );
        assertEquals( "warnings count", 0, reporter.getWarnings() );
        assertEquals( "successes count", 0, reporter.getSuccesses() );

        Iterator results = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "must have failures", results.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) results.next();
        assertEquals( "check failure cause", metadata, result.getMetadata() );
        assertEquals( "check failure reason", "First Failure Reason", result.getReason() );
        assertTrue( "must have 2nd failure", results.hasNext() );
        result = (RepositoryMetadataResult) results.next();
        assertEquals( "check failure cause", metadata, result.getMetadata() );
        assertEquals( "check failure reason", "Second Failure Reason", result.getReason() );
        assertFalse( "no more failures", results.hasNext() );
    }

    public void testMetadataSingleWarning()
    {
        reporter.addWarning( metadata, "Single Warning Message" );
        assertEquals( "failures count", 0, reporter.getFailures() );
        assertEquals( "warnings count", 1, reporter.getWarnings() );
        assertEquals( "successes count", 0, reporter.getSuccesses() );

        Iterator results = reporter.getRepositoryMetadataWarningIterator();
        assertTrue( "must have failures", results.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) results.next();
        assertEquals( "check failure cause", metadata, result.getMetadata() );
        assertEquals( "check failure reason", "Single Warning Message", result.getReason() );
        assertFalse( "no more failures", results.hasNext() );
    }
    
    public void testMetadataMultipleWarnings()
    {
        reporter.addWarning( metadata, "First Warning" );
        reporter.addWarning( metadata, "Second Warning" );
        assertEquals( "failures count", 0, reporter.getFailures() );
        assertEquals( "warnings count", 2, reporter.getWarnings() );
        assertEquals( "successes count", 0, reporter.getSuccesses() );

        Iterator results = reporter.getRepositoryMetadataWarningIterator();
        assertTrue( "must have warnings", results.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) results.next();
        assertEquals( "check failure cause", metadata, result.getMetadata() );
        assertEquals( "check failure reason", "First Warning", result.getReason() );
        assertTrue( "must have 2nd warning", results.hasNext() );
        result = (RepositoryMetadataResult) results.next();
        assertEquals( "check failure cause", metadata, result.getMetadata() );
        assertEquals( "check failure reason", "Second Warning", result.getReason() );
        assertFalse( "no more failures", results.hasNext() );
    }

    public void testMetadataSingleSuccess()
    {
        reporter.addSuccess( metadata );
        assertEquals( "failures count", 0, reporter.getFailures() );
        assertEquals( "warnings count", 0, reporter.getWarnings() );
        assertEquals( "successes count", 1, reporter.getSuccesses() );

        Iterator results = reporter.getRepositoryMetadataSuccessIterator();
        assertTrue( "must have successes", results.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) results.next();
        assertEquals( "check success metadata", metadata, result.getMetadata() );
        assertNull( "check no reason", result.getReason() );
        assertFalse( "no more failures", results.hasNext() );
    }
    
    public void testMetadataMultipleSuccesses()
    {
        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-beta-1" );
        versioning.addVersion( "1.0-beta-2" );        
        RepositoryMetadata metadata2 = new ArtifactRepositoryMetadata( artifact, versioning );
        
        reporter.addSuccess( metadata );
        reporter.addSuccess( metadata2 );
        assertEquals( "failures count", 0, reporter.getFailures() );
        assertEquals( "warnings count", 0, reporter.getWarnings() );
        assertEquals( "successes count", 2, reporter.getSuccesses() );

        Iterator results = reporter.getRepositoryMetadataSuccessIterator();
        assertTrue( "must have successes", results.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) results.next();
        assertEquals( "check success metadata", metadata, result.getMetadata() );
        assertNull( "check no reason", result.getReason() );
        assertTrue( "must have 2nd success", results.hasNext() );
        result = (RepositoryMetadataResult) results.next();
        assertEquals( "check success metadata", metadata2, result.getMetadata() );
        assertNull( "check no reason", result.getReason() );
        assertFalse( "no more successes", results.hasNext() );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        
        reporter = new DefaultArtifactReporter();
        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );        
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        reporter = null;
        metadata = null;
    }
}
