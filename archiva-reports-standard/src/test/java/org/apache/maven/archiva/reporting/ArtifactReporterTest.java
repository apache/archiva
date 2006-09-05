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

    private Model model;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = (ArtifactReporter) lookup( ArtifactReporter.ROLE );
        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );
        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.setLastUpdated( "20050611.202020" );
        model = new Model();
    }

    public void testArtifactReporterSingleSuccess()
    {
        reporter.addSuccess( artifact );

        assertEquals( 1, reporter.getNumSuccesses() );

        Iterator success = reporter.getArtifactSuccessIterator();
        assertTrue( success.hasNext() );
        Artifact result = ( (ArtifactResult) success.next() ).getArtifact();
        assertEquals( "groupId", result.getGroupId() );
        assertEquals( "artifactId", result.getArtifactId() );
        assertEquals( "1.0-alpha-1", result.getVersion() );
        assertFalse( success.hasNext() );
    }

    public void testArtifactReporterMultipleSuccess()
    {
        reporter.addSuccess( artifact );
        reporter.addSuccess( artifact );
        reporter.addSuccess( artifact );
        Iterator success = reporter.getArtifactSuccessIterator();
        assertTrue( success.hasNext() );
        int i;
        for ( i = 0; success.hasNext(); i++ )
        {
            success.next();
        }
        assertEquals( 3, i );
        assertEquals( 3, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testArtifactReporterSingleFailure()
    {
        reporter.addFailure( artifact, "failed once" );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertTrue( failure.hasNext() );
        failure.next();
        assertFalse( failure.hasNext() );
        assertEquals( 0, reporter.getNumSuccesses() );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testArtifactReporterMultipleFailure()
    {
        reporter.addFailure( artifact, "failed once" );
        reporter.addFailure( artifact, "failed twice" );
        reporter.addFailure( artifact, "failed thrice" );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertTrue( failure.hasNext() );
        int i;
        for ( i = 0; failure.hasNext(); i++ )
        {
            failure.next();
        }
        assertEquals( 3, i );
        assertEquals( 0, reporter.getNumSuccesses() );
        assertEquals( 3, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testFailureMessages()
    {
        reporter.addFailure( artifact, "failed once" );
        reporter.addFailure( artifact, "failed twice" );
        reporter.addFailure( artifact, "failed thrice" );
        Iterator failure = reporter.getArtifactFailureIterator();
        assertEquals( "failed once", ( (ArtifactResult) failure.next() ).getReason() );
        assertEquals( "failed twice", ( (ArtifactResult) failure.next() ).getReason() );
        assertEquals( "failed thrice", ( (ArtifactResult) failure.next() ).getReason() );
    }

    public void testArtifactReporterSingleWarning()
    {
        reporter.addWarning( artifact, "you've been warned" );
        Iterator warning = reporter.getArtifactWarningIterator();
        assertTrue( warning.hasNext() );
        warning.next();
        assertFalse( warning.hasNext() );
        assertEquals( 0, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 1, reporter.getNumWarnings() );
    }

    public void testArtifactReporterMultipleWarning()
    {
        reporter.addWarning( artifact, "i'm warning you" );
        reporter.addWarning( artifact, "you have to stop now" );
        reporter.addWarning( artifact, "all right... that does it!" );

        Iterator warning = reporter.getArtifactWarningIterator();
        assertTrue( warning.hasNext() );
        int i;
        for ( i = 0; warning.hasNext(); i++ )
        {
            warning.next();
        }
        assertEquals( 3, i );
        assertEquals( 0, reporter.getNumSuccesses() );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 3, reporter.getNumWarnings() );
    }

    public void testWarningMessages()
    {
        reporter.addWarning( artifact, "i'm warning you" );
        reporter.addWarning( artifact, "you have to stop now" );
        reporter.addWarning( artifact, "all right... that does it!" );

        Iterator warning = reporter.getArtifactWarningIterator();
        assertEquals( "i'm warning you", ( (ArtifactResult) warning.next() ).getReason() );
        assertEquals( "you have to stop now", ( (ArtifactResult) warning.next() ).getReason() );
        assertEquals( "all right... that does it!", ( (ArtifactResult) warning.next() ).getReason() );
    }
}
