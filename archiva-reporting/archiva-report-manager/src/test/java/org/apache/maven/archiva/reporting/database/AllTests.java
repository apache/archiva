package org.apache.maven.archiva.reporting.database;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Test for org.apache.maven.archiva.reporting.database" );
        //$JUnit-BEGIN$
        suite.addTestSuite( ArtifactResultsDatabaseTest.class );
        suite.addTestSuite( MetadataResultsDatabaseTest.class );
        suite.addTestSuite( ReportingDatabaseTest.class );
        //$JUnit-END$
        return suite;
    }

}
