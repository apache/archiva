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

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class ArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
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
    }

    public void testNoProjectDescriptor()
    {
        processor.processArtifact( null, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testArtifactFoundButNoDirectDependencies()
    {
        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );
        processor.setRepositoryQueryLayer( queryLayer );
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
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testValidArtifactWithValidSingleDependency()
    {
        MockArtifactFactory artifactFactory = new MockArtifactFactory();
        processor.setArtifactFactory( artifactFactory );

        MockRepositoryQueryLayer queryLayer = new MockRepositoryQueryLayer();
        queryLayer.addReturnValue( RepositoryQueryLayer.ARTIFACT_FOUND );

        Dependency dependency = new Dependency();
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

        processor.setRepositoryQueryLayer( queryLayer );
        processor.processArtifact( model, artifact, reporter, null );
        assertTrue( reporter.getSuccesses() == 5 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    protected void tearDown()
        throws Exception
    {
        model = null;
        artifact = null;
        reporter = null;
        super.tearDown();
    }
}
