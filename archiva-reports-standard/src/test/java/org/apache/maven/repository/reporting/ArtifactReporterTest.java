package org.apache.maven.repository.reporting;

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
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Model;

import java.util.Iterator;

/**
 * 
 */
public class ArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReporter reporter;

    private Artifact artifact;

    private MockArtifactReportProcessor processor;

    private Model model;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = new DefaultArtifactReporter();
        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );
        processor = new MockArtifactReportProcessor();
        Versioning versioning = new Versioning();
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
        assertEquals( 1, reporter.getSuccesses() );
        Artifact result = ( (ArtifactResult) success.next() ).getArtifact();
        assertEquals( "groupId", result.getGroupId() );
        assertEquals( "artifactId", result.getArtifactId() );
        assertEquals( "1.0-alpha-1", result.getVersion() );
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
        assertEquals( 3, i );
        assertEquals( 3, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
    }

    public void testArtifactReporterSingleFailure()
    {
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed once" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertTrue( failure.hasNext() );
        failure.next();
        assertFalse( failure.hasNext() );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 1, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
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
        assertEquals( 3, i );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 3, reporter.getFailures() );
        assertEquals( 0, reporter.getWarnings() );
    }

    public void testFailureMessages()
    {
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed once" );
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed twice" );
        processor.addReturnValue( ReportCondition.FAILURE, artifact, "failed thrice" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertEquals( "failed once", ( (ArtifactResult) failure.next() ).getReason() );
        assertEquals( "failed twice", ( (ArtifactResult) failure.next() ).getReason() );
        assertEquals( "failed thrice", ( (ArtifactResult) failure.next() ).getReason() );
    }

    public void testArtifactReporterSingleWarning()
    {
        processor.addReturnValue( ReportCondition.WARNING, artifact, "you've been warned" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator warning = reporter.getArtifactWarningIterator();
        assertTrue( warning.hasNext() );
        warning.next();
        assertFalse( warning.hasNext() );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 1, reporter.getWarnings() );
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
        assertEquals( 3, i );
        assertEquals( 0, reporter.getSuccesses() );
        assertEquals( 0, reporter.getFailures() );
        assertEquals( 3, reporter.getWarnings() );
    }

    public void testWarningMessages()
    {
        processor.addReturnValue( ReportCondition.WARNING, artifact, "i'm warning you" );
        processor.addReturnValue( ReportCondition.WARNING, artifact, "you have to stop now" );
        processor.addReturnValue( ReportCondition.WARNING, artifact, "all right... that does it!" );
        processor.processArtifact( model, artifact, reporter, null );
        Iterator warning = reporter.getArtifactWarningIterator();
        assertEquals( "i'm warning you", ( (ArtifactResult) warning.next() ).getReason() );
        assertEquals( "you have to stop now", ( (ArtifactResult) warning.next() ).getReason() );
        assertEquals( "all right... that does it!", ( (ArtifactResult) warning.next() ).getReason() );
    }

    protected void tearDown()
        throws Exception
    {
        model = null;
        processor.clearList();
        processor = null;
        reporter = null;
        super.tearDown();
    }

}
