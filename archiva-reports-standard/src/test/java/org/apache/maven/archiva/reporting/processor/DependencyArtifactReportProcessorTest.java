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
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.model.ArtifactResults;
import org.apache.maven.archiva.reporting.model.Result;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.Iterator;

/**
 *
 */
public class DependencyArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private static final String VALID_GROUP_ID = "groupId";

    private static final String VALID_ARTIFACT_ID = "artifactId";

    private static final String VALID_VERSION = "1.0-alpha-1";

    private ReportingDatabase reportingDatabase;

    private Model model;

    private ArtifactReportProcessor processor;

    private ArtifactFactory artifactFactory;

    private static final String INVALID = "invalid";

    protected void setUp()
        throws Exception
    {
        super.setUp();
        model = new Model();
        processor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "dependency" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "health" );
        reportingDatabase = new ReportingDatabase( reportGroup );
    }

    public void testArtifactFoundButNoDirectDependencies()
    {
        Artifact artifact = createValidArtifact();
        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 0, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );
    }

    private Artifact createValidArtifact()
    {
        Artifact projectArtifact =
            artifactFactory.createProjectArtifact( VALID_GROUP_ID, VALID_ARTIFACT_ID, VALID_VERSION );
        projectArtifact.setRepository( repository );
        return projectArtifact;
    }

    public void testArtifactNotFound()
    {
        Artifact artifact = artifactFactory.createProjectArtifact( INVALID, INVALID, INVALID );
        artifact.setRepository( repository );
        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );
        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( "Artifact does not exist in the repository", result.getReason() );
    }

    public void testValidArtifactWithNullDependency()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createValidDependency();
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 0, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );
    }

    private Dependency createValidDependency()
    {
        return createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, VALID_VERSION );
    }

    public void testValidArtifactWithValidSingleDependency()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createValidDependency();
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 0, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );
    }

    public void testValidArtifactWithValidMultipleDependencies()
    {
        Dependency dependency = createValidDependency();
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );

        Artifact artifact = createValidArtifact();
        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 0, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );
    }

    public void testValidArtifactWithAnInvalidDependency()
    {
        Dependency dependency = createValidDependency();
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( createDependency( INVALID, INVALID, INVALID ) );

        Artifact artifact = createValidArtifact();
        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( getDependencyNotFoundMessage( createDependency( INVALID, INVALID, INVALID ) ),
                      result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyGroupId()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( INVALID, VALID_ARTIFACT_ID, VALID_VERSION );
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( getDependencyNotFoundMessage( dependency ), result.getReason() );
    }

    private Dependency createDependency( String o, String valid, String s )
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId( o );
        dependency.setArtifactId( valid );
        dependency.setVersion( s );
        return dependency;
    }

    public void testValidArtifactWithInvalidDependencyArtifactId()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, INVALID, VALID_VERSION );
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( getDependencyNotFoundMessage( dependency ), result.getReason() );
    }

    public void testValidArtifactWithIncorrectDependencyVersion()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, INVALID );
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( getDependencyNotFoundMessage( dependency ), result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyVersion()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, "[" );
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( getDependencyVersionInvalidMessage( dependency, "[" ), result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyVersionRange()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, "[1.0,)" );
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 0, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );
    }

    public void testValidArtifactWithMissingDependencyVersion()
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, null );
        model.addDependency( dependency );

        processor.processArtifact( artifact, model, reportingDatabase );
        assertEquals( 1, reportingDatabase.getNumFailures() );
        assertEquals( 0, reportingDatabase.getNumWarnings() );
        assertEquals( 0, reportingDatabase.getNumNotices() );

        Iterator failures = reportingDatabase.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) failures.next();
        assertFalse( failures.hasNext() );
        failures = results.getFailures().iterator();
        Result result = (Result) failures.next();
        assertEquals( getDependencyVersionInvalidMessage( dependency, null ), result.getReason() );
    }

    private String getDependencyVersionInvalidMessage( Dependency dependency, String version )
    {
        return "Artifact's dependency " + getDependencyString( dependency ) + " contains an invalid version " + version;
    }

    private static String getDependencyString( Dependency dependency )
    {
        return DependencyArtifactReportProcessor.getDependencyString( dependency );
    }

    private String getDependencyNotFoundMessage( Dependency dependency )
    {
        return "Artifact's dependency " + getDependencyString( dependency ) + " does not exist in the repository";
    }
}
