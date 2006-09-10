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

import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.archiva.reporting.model.Result;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.util.Iterator;

/**
 *
 */
public class DefaultArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    private ReportingDatabase reportingDatabase;

    private RepositoryMetadata metadata;

    public void testEmptyArtifactReporter()
    {
        assertEquals( "No failures", 0, reportingDatabase.getNumFailures() );
        assertEquals( "No warnings", 0, reportingDatabase.getNumWarnings() );
        assertFalse( "No artifact failures", reportingDatabase.getArtifactIterator().hasNext() );
        assertFalse( "No metadata failures", reportingDatabase.getMetadataIterator().hasNext() );
    }

    public void testMetadataSingleFailure()
    {
        reportingDatabase.addFailure( metadata, "Single Failure Reason" );
        assertEquals( "failures count", 1, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );

        Iterator failures = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        Result result = (Result) failures.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "Single Failure Reason", result.getReason() );
        assertFalse( "no more failures", failures.hasNext() );
    }

    private void assertMetadata( MetadataResults result )
    {
        assertEquals( "check failure cause", metadata.getGroupId(), result.getGroupId() );
        assertEquals( "check failure cause", metadata.getArtifactId(), result.getArtifactId() );
        assertEquals( "check failure cause", metadata.getBaseVersion(), result.getVersion() );
    }

    public void testMetadataMultipleFailures()
    {
        reportingDatabase.addFailure( metadata, "First Failure Reason" );
        reportingDatabase.addFailure( metadata, "Second Failure Reason" );
        assertEquals( "failures count", 2, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );

        Iterator failures = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        Result result = (Result) failures.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "First Failure Reason", result.getReason() );
        assertTrue( "must have 2nd failure", failures.hasNext() );
        result = (Result) failures.next();
        assertEquals( "check failure reason", "Second Failure Reason", result.getReason() );
        assertFalse( "no more failures", failures.hasNext() );
    }

    public void testMetadataSingleWarning()
    {
        reportingDatabase.addWarning( metadata, "Single Warning Message" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 1, reportingDatabase.getNumWarnings() );

        Iterator warnings = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        MetadataResults results = (MetadataResults) warnings.next();
        warnings = results.getWarnings().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "Single Warning Message", result.getReason() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testMetadataMultipleWarnings()
    {
        reportingDatabase.addWarning( metadata, "First Warning" );
        reportingDatabase.addWarning( metadata, "Second Warning" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 2, reportingDatabase.getNumWarnings() );

        Iterator warnings = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        MetadataResults results = (MetadataResults) warnings.next();
        warnings = results.getWarnings().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "First Warning", result.getReason() );
        assertTrue( "must have 2nd warning", warnings.hasNext() );
        result = (Result) warnings.next();
        assertEquals( "check failure reason", "Second Warning", result.getReason() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );

        metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "health" );
        reportingDatabase = new ReportingDatabase( reportGroup );
    }
}
