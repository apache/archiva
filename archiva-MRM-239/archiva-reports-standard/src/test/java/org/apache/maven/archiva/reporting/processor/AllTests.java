package org.apache.maven.archiva.reporting.processor;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Test for org.apache.maven.archiva.reporting.processor" );
        //$JUnit-BEGIN$
        suite.addTestSuite( LocationArtifactReportProcessorTest.class );
        suite.addTestSuite( DuplicateArtifactFileReportProcessorTest.class );
        suite.addTestSuite( OldSnapshotArtifactReportProcessorTest.class );
        suite.addTestSuite( DependencyArtifactReportProcessorTest.class );
        suite.addTestSuite( OldArtifactReportProcessorTest.class );
        suite.addTestSuite( InvalidPomArtifactReportProcessorTest.class );
        suite.addTestSuite( BadMetadataReportProcessorTest.class );
        //$JUnit-END$
        return suite;
    }

}
