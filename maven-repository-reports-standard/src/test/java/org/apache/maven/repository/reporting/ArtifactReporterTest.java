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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Model;

import java.util.Iterator;

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class ArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    protected ArtifactReporter reporter;

    protected ArtifactFactory artifactFactory;

    protected Artifact artifact;

    protected MockArtifactReportProcessor processor;

    protected Versioning versioning;

    protected Model model;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = new DefaultArtifactReporter();
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );
        processor = new MockArtifactReportProcessor();
        versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.setLastUpdated( "20050611.202020" );
        model = new Model();
    }

    public void testArtifactReporterSingleSuccess()
    {
        processor.addReturnValue( ReportCondition.SUCCESS, artifact, "all is good" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator success = reporter.getArtifactSuccessIterator();
        assertTrue( success.hasNext() );
        success.next();
        assertFalse( success.hasNext() );
    }

    public void testArtifactReporterMultipleSuccess()
    {
        processor.clearList();
        processor.addReturnValue( ReportCondition.SUCCESS, artifact, "one" );
        processor.addReturnValue( ReportCondition.SUCCESS, artifact, "two" );
        processor.addReturnValue( ReportCondition.SUCCESS, artifact, "three" );
        reporter = new DefaultArtifactReporter();
        processor.processArtifact( model, artifact, reporter, null );
        Iterator success = reporter.getArtifactSuccessIterator();
        assertTrue( success.hasNext() );
        int i;
        for ( i = 0; success.hasNext(); i++ )
        {
            success.next();
        }
        assertTrue( i == 3 );
        assertTrue( reporter.getSuccesses() == 3 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testArtifactReporterSingleFailure()
    {
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed once" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertTrue( failure.hasNext() );
        failure.next();
        assertFalse( failure.hasNext() );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 1 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testArtifactReporterMultipleFailure()
    {
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed once" );
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed twice" );
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed thrice" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertTrue( failure.hasNext() );
        int i;
        for ( i = 0; failure.hasNext(); i++ )
        {
            failure.next();
        }
        assertTrue( i == 3 );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 3 );
        assertTrue( reporter.getWarnings() == 0 );
    }

    public void testFailureMessages()
    {
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed once" );
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed twice" );
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed thrice" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertTrue( "failed once".equals( ((ArtifactResult) failure.next()).getReason() ) );
        assertTrue( "failed twice".equals( ((ArtifactResult) failure.next()).getReason() ) );
        assertTrue( "failed thrice".equals( ((ArtifactResult) failure.next()).getReason() ) );
    }

    public void testArtifactReporterSingleWarning()
    {
        processor.addReturnValue( ReportCondition.WARNING, artifact, "you've been warned" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator warning = reporter.getArtifactWarningIterator();
        assertTrue( warning.hasNext() );
        warning.next();
        assertFalse( warning.hasNext() );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 1 );
    }

    public void testArtifactReporterMultipleWarning()
    {
        processor.addReturnValue( ReportCondition.WARNING, artifact, "i'm warning you" );
        processor.addReturnValue( ReportCondition.WARNING, artifact, "you have to stop now" );
        processor.addReturnValue( ReportCondition.WARNING, artifact, "all right... that does it!" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator warning = reporter.getArtifactWarningIterator();
        assertTrue( warning.hasNext() );
        int i;
        for ( i = 0; warning.hasNext(); i++ )
        {
            warning.next();
        }
        assertTrue( i == 3 );
        assertTrue( reporter.getSuccesses() == 0 );
        assertTrue( reporter.getFailures() == 0 );
        assertTrue( reporter.getWarnings() == 3 );
    }

    public void testWarningMessages()
    {
        processor.addReturnValue( ReportCondition.WARNING, artifact, "i'm warning you" );
        processor.addReturnValue( ReportCondition.WARNING, artifact, "you have to stop now" );
        processor.addReturnValue( ReportCondition.WARNING, artifact, "all right... that does it!" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator warning = reporter.getArtifactWarningIterator();
        assertTrue( "i'm warning you".equals( ((ArtifactResult) warning.next()).getReason() ) );
        assertTrue( "you have to stop now".equals( ((ArtifactResult) warning.next()).getReason() ) );
        assertTrue( "all right... that does it!".equals( ((ArtifactResult) warning.next()).getReason() ) );
    }

    protected void tearDown()
        throws Exception
    {
        model = null;
        versioning = null;
        processor.clearList();
        processor = null;
        artifactFactory = null;
        reporter = null;
        super.tearDown();
    }

}
