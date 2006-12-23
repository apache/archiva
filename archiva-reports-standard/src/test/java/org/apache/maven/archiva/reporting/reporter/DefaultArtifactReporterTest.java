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

import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.model.ArtifactResults;
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

    private static final String PROCESSOR = "processor";

    private static final String PROBLEM = "problem";

    private Artifact artifact;

    public void testEmptyArtifactReporter()
    {
        assertEquals( "No failures", 0, reportingDatabase.getNumFailures() );
        assertEquals( "No warnings", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );
        assertFalse( "No artifact failures", reportingDatabase.getArtifactIterator().hasNext() );
        assertFalse( "No metadata failures", reportingDatabase.getMetadataIterator().hasNext() );
    }

    public void testMetadataSingleFailure()
    {
        reportingDatabase.addFailure( metadata, PROCESSOR, PROBLEM, "Single Failure Reason" );
        assertEquals( "failures count", 1, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        Result result = (Result) failures.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "Single Failure Reason", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
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
        reportingDatabase.addFailure( metadata, PROCESSOR, PROBLEM, "First Failure Reason" );
        reportingDatabase.addFailure( metadata, PROCESSOR, PROBLEM, "Second Failure Reason" );
        assertEquals( "failures count", 2, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        Result result = (Result) failures.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "First Failure Reason", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertTrue( "must have 2nd failure", failures.hasNext() );
        result = (Result) failures.next();
        assertEquals( "check failure reason", "Second Failure Reason", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more failures", failures.hasNext() );
    }

    public void testMetadataSingleWarning()
    {
        reportingDatabase.addWarning( metadata, PROCESSOR, PROBLEM, "Single Warning Message" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 1, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        MetadataResults results = (MetadataResults) warnings.next();
        warnings = results.getWarnings().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "Single Warning Message", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testMetadataMultipleWarnings()
    {
        reportingDatabase.addWarning( metadata, PROCESSOR, PROBLEM, "First Warning" );
        reportingDatabase.addWarning( metadata, PROCESSOR, PROBLEM, "Second Warning" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 2, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        MetadataResults results = (MetadataResults) warnings.next();
        warnings = results.getWarnings().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "First Warning", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertTrue( "must have 2nd warning", warnings.hasNext() );
        result = (Result) warnings.next();
        assertEquals( "check failure reason", "Second Warning", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testMetadataSingleNotice()
    {
        reportingDatabase.addNotice( metadata, PROCESSOR, PROBLEM, "Single Notice Message" );
        assertEquals( "failure count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check notices", 1, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        MetadataResults results = (MetadataResults) warnings.next();
        warnings = results.getNotices().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "Single Notice Message", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testMetadataMultipleNotices()
    {
        reportingDatabase.addNotice( metadata, PROCESSOR, PROBLEM, "First Notice" );
        reportingDatabase.addNotice( metadata, PROCESSOR, PROBLEM, "Second Notice" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 2, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getMetadataIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        MetadataResults results = (MetadataResults) warnings.next();
        warnings = results.getNotices().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertMetadata( results );
        assertEquals( "check failure reason", "First Notice", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertTrue( "must have 2nd warning", warnings.hasNext() );
        result = (Result) warnings.next();
        assertEquals( "check failure reason", "Second Notice", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testArtifactSingleFailure()
    {
        reportingDatabase.addFailure( artifact, PROCESSOR, PROBLEM, "Single Failure Reason" );
        assertEquals( "failures count", 1, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ArtifactResults results = (ArtifactResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        Result result = (Result) failures.next();
        assertArtifact( results );
        assertEquals( "check failure reason", "Single Failure Reason", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more failures", failures.hasNext() );
    }

    private void assertArtifact( ArtifactResults results )
    {
        assertEquals( "check failure cause", artifact.getGroupId(), results.getGroupId() );
        assertEquals( "check failure cause", artifact.getArtifactId(), results.getArtifactId() );
        assertEquals( "check failure cause", artifact.getVersion(), results.getVersion() );
        assertEquals( "check failure cause", artifact.getClassifier(), results.getClassifier() );
        assertEquals( "check failure cause", artifact.getType(), results.getType() );
    }

    public void testArtifactMultipleFailures()
    {
        reportingDatabase.addFailure( artifact, PROCESSOR, PROBLEM, "First Failure Reason" );
        reportingDatabase.addFailure( artifact, PROCESSOR, PROBLEM, "Second Failure Reason" );
        assertEquals( "failures count", 2, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ArtifactResults results = (ArtifactResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        Result result = (Result) failures.next();
        assertArtifact( results );
        assertEquals( "check failure reason", "First Failure Reason", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertTrue( "must have 2nd failure", failures.hasNext() );
        result = (Result) failures.next();
        assertEquals( "check failure reason", "Second Failure Reason", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more failures", failures.hasNext() );
    }

    public void testArtifactSingleWarning()
    {
        reportingDatabase.addWarning( artifact, PROCESSOR, PROBLEM, "Single Warning Message" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 1, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getArtifactIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        ArtifactResults results = (ArtifactResults) warnings.next();
        warnings = results.getWarnings().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertArtifact( results );
        assertEquals( "check failure reason", "Single Warning Message", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testArtifactMultipleWarnings()
    {
        reportingDatabase.addWarning( artifact, PROCESSOR, PROBLEM, "First Warning" );
        reportingDatabase.addWarning( artifact, PROCESSOR, PROBLEM, "Second Warning" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 2, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 0, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getArtifactIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        ArtifactResults results = (ArtifactResults) warnings.next();
        warnings = results.getWarnings().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertArtifact( results );
        assertEquals( "check failure reason", "First Warning", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertTrue( "must have 2nd warning", warnings.hasNext() );
        result = (Result) warnings.next();
        assertEquals( "check failure reason", "Second Warning", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testArtifactSingleNotice()
    {
        reportingDatabase.addNotice( artifact, PROCESSOR, PROBLEM, "Single Notice Message" );
        assertEquals( "failure count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check notices", 1, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getArtifactIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        ArtifactResults results = (ArtifactResults) warnings.next();
        warnings = results.getNotices().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertArtifact( results );
        assertEquals( "check failure reason", "Single Notice Message", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    public void testArtifactMultipleNotices()
    {
        reportingDatabase.addNotice( artifact, PROCESSOR, PROBLEM, "First Notice" );
        reportingDatabase.addNotice( artifact, PROCESSOR, PROBLEM, "Second Notice" );
        assertEquals( "warnings count", 0, reportingDatabase.getNumFailures() );
        assertEquals( "warnings count", 0, reportingDatabase.getNumWarnings() );
        assertEquals( "check no notices", 2, reportingDatabase.getNumNotices() );

        Iterator warnings = reportingDatabase.getArtifactIterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        ArtifactResults results = (ArtifactResults) warnings.next();
        warnings = results.getNotices().iterator();
        assertTrue( "check there is a failure", warnings.hasNext() );
        Result result = (Result) warnings.next();
        assertArtifact( results );
        assertEquals( "check failure reason", "First Notice", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertTrue( "must have 2nd warning", warnings.hasNext() );
        result = (Result) warnings.next();
        assertEquals( "check failure reason", "Second Notice", result.getReason() );
        assertEquals( "check failure parameters", PROCESSOR, result.getProcessor() );
        assertEquals( "check failure parameters", PROBLEM, result.getProblem() );
        assertFalse( "no more warnings", warnings.hasNext() );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );

        metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "health" );
        reportingDatabase = new ReportingDatabase( reportGroup );
    }
}
