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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
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
 * DefaultArtifactReporterTest 
 *
 * @version $Id$
 */
public class DefaultArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    private ReportingDatabase database;

    private RepositoryMetadata metadata;

    private static final String PROCESSOR = "processor";

    private static final String PROBLEM = "problem";

    private Artifact artifact;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        database = (ReportingDatabase) lookup( ReportingDatabase.ROLE );

        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );

        metadata = new ArtifactRepositoryMetadata( artifact, versioning );
    }

    public void testEmptyArtifactReporter()
    {
        assertEquals( "No failures", 0, database.getNumFailures() );
        assertEquals( "No warnings", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );
        assertFalse( "No artifact failures", database.getArtifactIterator().hasNext() );
        assertFalse( "No metadata failures", database.getMetadataIterator().hasNext() );
    }

    public void testMetadataSingleFailure()
    {
        database.getMetadataDatabase().addFailure( metadata, PROCESSOR, PROBLEM, "Single Failure Reason" );
        assertEquals( "failures count", 1, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator failures = database.getMetadataIterator();
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
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The MetadataResults object has a complex primary key consisting of groupId, artifactId, and version.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */

        assertEquals( "check failure cause", StringUtils.defaultString( metadata.getGroupId() ), result.getGroupId() );
        assertEquals( "check failure cause", StringUtils.defaultString( metadata.getArtifactId() ), result
            .getArtifactId() );
        assertEquals( "check failure cause", StringUtils.defaultString( metadata.getBaseVersion() ), result
            .getVersion() );
    }

    public void testMetadataMultipleFailures()
    {
        database.getMetadataDatabase().addFailure( metadata, PROCESSOR, PROBLEM, "First Failure Reason" );
        database.getMetadataDatabase().addFailure( metadata, PROCESSOR, PROBLEM, "Second Failure Reason" );
        assertEquals( "failures count", 2, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator failures = database.getMetadataIterator();
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
        database.getMetadataDatabase().addWarning( metadata, PROCESSOR, PROBLEM, "Single Warning Message" );
        assertEquals( "warnings count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 1, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator warnings = database.getMetadataIterator();
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
        database.getMetadataDatabase().addWarning( metadata, PROCESSOR, PROBLEM, "First Warning" );
        database.getMetadataDatabase().addWarning( metadata, PROCESSOR, PROBLEM, "Second Warning" );
        assertEquals( "warnings count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 2, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator warnings = database.getMetadataIterator();
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
        database.getMetadataDatabase().addNotice( metadata, PROCESSOR, PROBLEM, "Single Notice Message" );
        assertEquals( "failure count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check notices", 1, database.getNumNotices() );

        Iterator warnings = database.getMetadataIterator();
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
        database.getMetadataDatabase().addNotice( metadata, PROCESSOR, PROBLEM, "First Notice" );
        database.getMetadataDatabase().addNotice( metadata, PROCESSOR, PROBLEM, "Second Notice" );
        assertEquals( "warnings count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 2, database.getNumNotices() );

        Iterator warnings = database.getMetadataIterator();
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
        database.getArtifactDatabase().addFailure( artifact, PROCESSOR, PROBLEM, "Single Failure Reason" );
        assertEquals( "failures count", 1, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator failures = database.getArtifactIterator();
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
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The ArtifactResults object has a complex primary key consisting of groupId, artifactId, version,
         * type, classifier.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */

        assertEquals( "check failure cause", StringUtils.defaultString( artifact.getGroupId() ), results.getGroupId() );
        assertEquals( "check failure cause", StringUtils.defaultString( artifact.getArtifactId() ), results
            .getArtifactId() );
        assertEquals( "check failure cause", StringUtils.defaultString( artifact.getVersion() ), results.getVersion() );
        assertEquals( "check failure cause", StringUtils.defaultString( artifact.getClassifier() ), results
            .getClassifier() );
        assertEquals( "check failure cause", StringUtils.defaultString( artifact.getType() ), results.getType() );
    }

    public void testArtifactMultipleFailures()
    {
        database.getArtifactDatabase().addFailure( artifact, PROCESSOR, PROBLEM, "First Failure Reason" );
        database.getArtifactDatabase().addFailure( artifact, PROCESSOR, PROBLEM, "Second Failure Reason" );
        assertEquals( "failures count", 2, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator failures = database.getArtifactIterator();
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
        database.getArtifactDatabase().addWarning( artifact, PROCESSOR, PROBLEM, "Single Warning Message" );
        assertEquals( "warnings count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 1, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator warnings = database.getArtifactIterator();
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
        database.getArtifactDatabase().addWarning( artifact, PROCESSOR, PROBLEM, "First Warning" );
        database.getArtifactDatabase().addWarning( artifact, PROCESSOR, PROBLEM, "Second Warning" );
        assertEquals( "warnings count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 2, database.getNumWarnings() );
        assertEquals( "check no notices", 0, database.getNumNotices() );

        Iterator warnings = database.getArtifactIterator();
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
        database.getArtifactDatabase().addNotice( artifact, PROCESSOR, PROBLEM, "Single Notice Message" );
        assertEquals( "failure count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check notices", 1, database.getNumNotices() );

        Iterator warnings = database.getArtifactIterator();
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
        database.getArtifactDatabase().addNotice( artifact, PROCESSOR, PROBLEM, "First Notice" );
        database.getArtifactDatabase().addNotice( artifact, PROCESSOR, PROBLEM, "Second Notice" );
        assertEquals( "warnings count", 0, database.getNumFailures() );
        assertEquals( "warnings count", 0, database.getNumWarnings() );
        assertEquals( "check no notices", 2, database.getNumNotices() );

        Iterator warnings = database.getArtifactIterator();
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

}
