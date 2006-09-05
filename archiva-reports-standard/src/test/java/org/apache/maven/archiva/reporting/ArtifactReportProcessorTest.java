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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.Iterator;

/**
 *
 */
public class ArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private static final String VALID_GROUP_ID = "groupId";

    private static final String VALID_ARTIFACT_ID = "artifactId";

    private static final String VALID_VERSION = "1.0-alpha-1";

    private ArtifactReporter reporter;

    private Model model;

    private ArtifactReportProcessor processor;

    private ArtifactFactory artifactFactory;

    private static final String INVALID = "invalid";

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = (ArtifactReporter) lookup( ArtifactReporter.ROLE );
        model = new Model();
        processor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "default" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    public void testArtifactFoundButNoDirectDependencies()
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();
        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    private Artifact createValidArtifact()
    {
        return artifactFactory.createProjectArtifact( VALID_GROUP_ID, VALID_ARTIFACT_ID, VALID_VERSION );
    }

    public void testArtifactNotFound()
        throws ReportProcessorException
    {
        Artifact artifact = artifactFactory.createProjectArtifact( INVALID, INVALID, INVALID );
        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 0, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.ARTIFACT_NOT_FOUND, result.getReason() );
    }

    public void testValidArtifactWithNullDependency()
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createValidDependency();
        model.addDependency( dependency );

        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 2, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    private Dependency createValidDependency()
    {
        return createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, VALID_VERSION );
    }

    public void testValidArtifactWithValidSingleDependency()
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createValidDependency();
        model.addDependency( dependency );

        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 2, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testValidArtifactWithValidMultipleDependencies()
        throws ReportProcessorException
    {
        Dependency dependency = createValidDependency();
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );

        Artifact artifact = createValidArtifact();
        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 6, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testValidArtifactWithAnInvalidDependency()
        throws ReportProcessorException
    {
        Dependency dependency = createValidDependency();
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( dependency );
        model.addDependency( createDependency( INVALID, INVALID, INVALID ) );

        Artifact artifact = createValidArtifact();
        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 5, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.DEPENDENCY_NOT_FOUND, result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyGroupId()
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( INVALID, VALID_ARTIFACT_ID, VALID_VERSION );
        model.addDependency( dependency );

        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.DEPENDENCY_NOT_FOUND, result.getReason() );
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
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, INVALID, VALID_VERSION );
        model.addDependency( dependency );

        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.DEPENDENCY_NOT_FOUND, result.getReason() );
    }

    public void testValidArtifactWithIncorrectDependencyVersion()
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, INVALID );
        model.addDependency( dependency );

        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.DEPENDENCY_NOT_FOUND, result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyVersion()
        throws ReportProcessorException
    {
        Artifact artifact = createValidArtifact();

        Dependency dependency = createDependency( VALID_GROUP_ID, VALID_ARTIFACT_ID, "[" );
        model.addDependency( dependency );

        processor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.DEPENDENCY_INVALID_VERSION, result.getReason() );
    }
}
