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

import org.apache.maven.archiva.reporting.model.ArtifactResults;
import org.apache.maven.archiva.reporting.model.Result;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.util.Iterator;

/**
 *
 */
public class ArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    private ReportingDatabase reporter;

    private Artifact artifact;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = new ReportingDatabase();
        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );
        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.setLastUpdated( "20050611.202020" );
    }

    public void testArtifactReporterSingleFailure()
    {
        reporter.addFailure( artifact, "failed once" );

        Iterator artifactIterator = reporter.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertFalse( artifactIterator.hasNext() );

        int count = 0;
        for ( Iterator i = results.getFailures().iterator(); i.hasNext(); count++ )
        {
            i.next();
        }
        assertEquals( 1, count );
        assertEquals( 1, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testArtifactReporterMultipleFailure()
    {
        reporter.addFailure( artifact, "failed once" );
        reporter.addFailure( artifact, "failed twice" );
        reporter.addFailure( artifact, "failed thrice" );

        Iterator artifactIterator = reporter.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertFalse( artifactIterator.hasNext() );

        int count = 0;
        for ( Iterator i = results.getFailures().iterator(); i.hasNext(); count++ )
        {
            i.next();
        }
        assertEquals( 3, count );
        assertEquals( 3, reporter.getNumFailures() );
        assertEquals( 0, reporter.getNumWarnings() );
    }

    public void testFailureMessages()
    {
        reporter.addFailure( artifact, "failed once" );
        reporter.addFailure( artifact, "failed twice" );
        reporter.addFailure( artifact, "failed thrice" );
        Iterator artifactIterator = reporter.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertFalse( artifactIterator.hasNext() );
        Iterator failure = results.getFailures().iterator();
        assertEquals( "failed once", ( (Result) failure.next() ).getReason() );
        assertEquals( "failed twice", ( (Result) failure.next() ).getReason() );
        assertEquals( "failed thrice", ( (Result) failure.next() ).getReason() );
    }

    public void testArtifactReporterSingleWarning()
    {
        reporter.addWarning( artifact, "you've been warned" );
        Iterator artifactIterator = reporter.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertFalse( artifactIterator.hasNext() );

        int count = 0;
        for ( Iterator i = results.getWarnings().iterator(); i.hasNext(); count++ )
        {
            i.next();
        }
        assertEquals( 1, count );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 1, reporter.getNumWarnings() );
    }

    public void testArtifactReporterMultipleWarning()
    {
        reporter.addWarning( artifact, "i'm warning you" );
        reporter.addWarning( artifact, "you have to stop now" );
        reporter.addWarning( artifact, "all right... that does it!" );

        Iterator artifactIterator = reporter.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertFalse( artifactIterator.hasNext() );

        int count = 0;
        for ( Iterator i = results.getWarnings().iterator(); i.hasNext(); count++ )
        {
            i.next();
        }
        assertEquals( 3, count );
        assertEquals( 0, reporter.getNumFailures() );
        assertEquals( 3, reporter.getNumWarnings() );
    }

    public void testWarningMessages()
    {
        reporter.addWarning( artifact, "i'm warning you" );
        reporter.addWarning( artifact, "you have to stop now" );
        reporter.addWarning( artifact, "all right... that does it!" );

        Iterator artifactIterator = reporter.getArtifactIterator();
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertFalse( artifactIterator.hasNext() );
        Iterator warning = results.getWarnings().iterator();
        assertEquals( "i'm warning you", ( (Result) warning.next() ).getReason() );
        assertEquals( "you have to stop now", ( (Result) warning.next() ).getReason() );
        assertEquals( "all right... that does it!", ( (Result) warning.next() ).getReason() );
    }
}
