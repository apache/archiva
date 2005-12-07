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

import org.apache.maven.model.Model;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;

import java.util.Iterator;

/**
 * 
 */
public class ArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private final static String EMPTY_STRING = "";

    private final static String VALID = "temp";

    protected MockArtifactReporter reporter;

    protected Artifact artifact;

    protected Model model;

    protected DefaultArtifactReportProcessor processor;

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
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.NULL_ARTIFACT.equals( result.getReason() ) );
    }

    public void testNoProjectDescriptor()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.processArtifact( null, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 1 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.NULL_MODEL.equals( result.getReason() ) );
    }

    public void testArtifactFoundButNoDirectDependencies()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 1 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testArtifactNotFound()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_NOT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.ARTIFACT_NOT_FOUND.equals( result.getReason() ) );
    }

    public void testValidArtifactWithNullDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 2 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testValidArtifactWithValidSingleDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 2 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testValidArtifactWithValidMultipleDependencies()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 6 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testValidArtifactWithAnInvalidDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_NOT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        setRequiredElements( artifact, VALID, VALID, VALID );
        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 5 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.DEPENDENCY_NOT_FOUND.equals( result.getReason() ) );
    }

    public void testEmptyGroupId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, EMPTY_STRING, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_GROUP_ID.equals( result.getReason() ) );
    }

    public void testEmptyArtifactId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, EMPTY_STRING, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_ARTIFACT_ID.equals( result.getReason() ) );
    }

    public void testEmptyVersion()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, VALID, EMPTY_STRING );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_VERSION.equals( result.getReason() ) );
    }

    public void testNullGroupId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, null, VALID, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_GROUP_ID.equals( result.getReason() ) );
    }

    public void testNullArtifactId()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, null, VALID );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_ARTIFACT_ID.equals( result.getReason() ) );
    }

    public void testNullVersion()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, VALID, VALID, null );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_VERSION.equals( result.getReason() ) );
    }

    public void testMultipleFailures()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );

        setRequiredElements( artifact, null, null, null );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 3 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_GROUP_ID.equals( result.getReason() ) );
        result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_ARTIFACT_ID.equals( result.getReason() ) );
        result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_VERSION.equals( result.getReason() ) );
    }

    public void testValidArtifactWithInvalidDependencyGroupId()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, null, VALID, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 1 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_DEPENDENCY_GROUP_ID.equals( result.getReason() ) );
    }

    public void testValidArtifactWithInvalidDependencyArtifactId()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, null, VALID );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 1 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_DEPENDENCY_ARTIFACT_ID.equals( result.getReason() ) );
    }

    public void testValidArtifactWithInvalidDependencyVersion()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, VALID, VALID, null );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 1 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_DEPENDENCY_VERSION.equals( result.getReason() ) );
    }

    public void testValidArtifactWithInvalidDependencyRequiredElements()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        setRequiredElements( artifact, VALID, VALID, VALID );
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
        setRequiredElements( dependency, null, null, null );
        model.addDependency( dependency );
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 1 );
        assertTrue( reporter.getFailures() == 3 );
        assertTrue( reporter.getWarnings() == 0 );

        Iterator failures = reporter.getArtifactFailureIterator();
        ArtifactResult result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_DEPENDENCY_GROUP_ID.equals( result.getReason() ) );
        result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_DEPENDENCY_ARTIFACT_ID.equals( result.getReason() ) );
        result = (ArtifactResult) failures.next();
        assertTrue( ArtifactReporter.EMPTY_DEPENDENCY_VERSION.equals( result.getReason() ) );
    }

    protected void tearDown()
        throws Exception
    {
        model = null;
        artifact = null;
        reporter = null;
        super.tearDown();
    }

    protected void setRequiredElements( Artifact artifact, String groupId, String artifactId, String version )
    {
        artifact.setGroupId( groupId );
        artifact.setArtifactId( artifactId );
        artifact.setVersion( version );
    }

    protected void setRequiredElements( Dependency dependency, String groupId, String artifactId, String version )
    {
        dependency.setGroupId( groupId );
        dependency.setArtifactId( artifactId );
        dependency.setVersion( version );
    }
}
