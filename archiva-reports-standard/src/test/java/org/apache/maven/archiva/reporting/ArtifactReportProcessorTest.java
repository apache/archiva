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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.Iterator;

/**
 * 
 */
public class ArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private static final String EMPTY_STRING = "";

    private static final String VALID = "temp";

    private MockArtifactReporter reporter;

    private Artifact artifact;

    private Model model;

    private DefaultArtifactReportProcessor processor;

    private static final boolean ARTIFACT_FOUND = true;

    private static final boolean ARTIFACT_NOT_FOUND = false;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = new MockArtifactReporter();
        artifact = new MockArtifact();
        model = new Model();
        processor = new DefaultArtifactReportProcessor();
    }

    public void testNullArtifact()
    {
        processor.processArtifact( model, null, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.NULL_ARTIFACT, result.getReason() );
    }

    public void testNoProjectDescriptor()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.processArtifact( null, artifact, reporter, null );
        assertEquals( 1, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.NULL_MODEL, result.getReason() );
    }

    public void testArtifactFoundButNoDirectDependencies()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 1, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
    }

    public void testArtifactNotFound()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_NOT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.ARTIFACT_NOT_FOUND, result.getReason() );
    }

    public void testValidArtifactWithNullDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 2, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
    }

    public void testValidArtifactWithValidSingleDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 2, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
    }

    public void testValidArtifactWithValidMultipleDependencies()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 6, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
    }

    public void testValidArtifactWithAnInvalidDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_NOT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 5, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.DEPENDENCY_NOT_FOUND, result.getReason() );
    }

    public void testEmptyGroupId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, EMPTY_STRING, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_GROUP_ID, result.getReason() );
    }

    public void testEmptyArtifactId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, EMPTY_STRING, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_ARTIFACT_ID, result.getReason() );
    }

    public void testEmptyVersion()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, VALID, EMPTY_STRING );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_VERSION, result.getReason() );
    }

    public void testNullGroupId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, null, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_GROUP_ID, result.getReason() );
    }

    public void testNullArtifactId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, null, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_ARTIFACT_ID, result.getReason() );
    }

    public void testNullVersion()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, VALID, null );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_VERSION, result.getReason() );
    }

    public void testMultipleFailures()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, null, null, null );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 3, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_GROUP_ID, result.getReason() );
        result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_ARTIFACT_ID, result.getReason() );
        result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_VERSION, result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyGroupId()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, null, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 1, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_DEPENDENCY_GROUP_ID, result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyArtifactId()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, null, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 1, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_DEPENDENCY_ARTIFACT_ID, result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyVersion()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, null );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 1, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_DEPENDENCY_VERSION, result.getReason() );
    }

    public void testValidArtifactWithInvalidDependencyRequiredElements()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, null, null, null );
        model.addDependency( dependency );
        queryLayer.addReturnValue( ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertEquals( 1, reporter.getSuccesses() );
        assertEquals( 3, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_DEPENDENCY_GROUP_ID, result.getReason() );
        result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_DEPENDENCY_ARTIFACT_ID, result.getReason() );
        result = (ArtifactResult) failures.next();
        assertEquals( ArtifactReporter.EMPTY_DEPENDENCY_VERSION, result.getReason() );
    }

    protected void tearDown()
        throws Exception
    {
        model = null;
        artifact = null;
        reporter = null;
        super.tearDown();
    }

    private void setRequiredElements( Artifact artifact, String groupId, String artifactId, String version )
    {
        artifact.setGroupId( groupId );
        artifact.setArtifactId( artifactId );
        artifact.setVersion( version );
    }

    private void setRequiredElements( Dependency dependency, String groupId, String artifactId, String version )
    {
        dependency.setGroupId( groupId );
        dependency.setArtifactId( artifactId );
        dependency.setVersion( version );
    }
}
