package org.apache.maven.archiva.reporting.processor;

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
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor;
import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.artifact.Artifact;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * This class tests the OldArtifactReportProcessor.
 */
public class OldSnapshotArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ReportingDatabase reportDatabase;

    private File tempRepository;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor =
            (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "old-snapshot-artifact" );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "old-artifact" );
        reportDatabase = new ReportingDatabase( reportGroup );
        tempRepository = getTestFile( "target/test-repository" );
        FileUtils.deleteDirectory( tempRepository );
    }

    public void testOldSnapshotArtifact()
    {
        Artifact artifact = createArtifact( "groupId", "snapshot-artifact", "1.0-alpha-1-20050611.202024-1", "pom" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check notices", 1, reportDatabase.getNumNotices() );
        Iterator artifactIterator = reportDatabase.getArtifactIterator();
        assertArtifactResults( artifactIterator, artifact );
    }

    private static void assertArtifactResults( Iterator artifactIterator, Artifact artifact )
    {
        ArtifactResults results = (ArtifactResults) artifactIterator.next();
        assertEquals( artifact.getArtifactId(), results.getArtifactId() );
        assertEquals( artifact.getGroupId(), results.getGroupId() );
        assertEquals( artifact.getVersion(), results.getVersion() );
        assertFalse( artifact.getVersion().indexOf( "SNAPSHOT" ) >= 0 );
        assertEquals( 1, results.getNotices().size() );
        Iterator i = results.getNotices().iterator();
        Result result = (Result) i.next();
        assertEquals( "old-snapshot-artifact", result.getProcessor() );
    }

    public void testSNAPSHOTArtifact()
    {
        Artifact artifact = createArtifact( "groupId", "snapshot-artifact", "1.0-alpha-1-SNAPSHOT", "pom" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    public void testNonSnapshotArtifact()
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    public void testNewSnapshotArtifact()
        throws Exception
    {
        File repository = getTestFile( "target/test-repository" );

        File dir = new File( repository, "groupId/artifactId/1.0-alpha-1-SNAPSHOT" );
        dir.mkdirs();

        String date = new SimpleDateFormat( "yyyyMMdd.HHmmss" ).format( new Date() );
        FileUtils.writeStringToFile( new File( dir, "artifactId-1.0-alpha-1-" + date + "-1.jar" ), "foo", null );

        Artifact artifact =
            createArtifactFromRepository( repository, "groupId", "artifactId", "1.0-alpha-1-" + date + "-1" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    public void testTooManySnapshotArtifact()
        throws Exception
    {
        File dir = new File( tempRepository, "groupId/artifactId/1.0-alpha-1-SNAPSHOT" );
        dir.mkdirs();

        String date = new SimpleDateFormat( "yyyyMMdd.HHmmss" ).format( new Date() );
        for ( int i = 1; i <= 5; i++ )
        {
            FileUtils.writeStringToFile( new File( dir, "artifactId-1.0-alpha-1-" + date + "-" + i + ".jar" ),
                                 "foo", null );
        }

        for ( int i = 1; i <= 5; i++ )
        {
            Artifact artifact = createArtifactFromRepository( tempRepository, "groupId", "artifactId",
                                                              "1.0-alpha-1-" + date + "-" + i );
            artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        }

        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check notices", 3, reportDatabase.getNumNotices() );
        Iterator artifactIterator = reportDatabase.getArtifactIterator();
        for ( int i = 1; i <= 3; i++ )
        {
            String version = "1.0-alpha-1-" + date + "-" + i;
            Artifact artifact = createArtifactFromRepository( tempRepository, "groupId", "artifactId", version );
            assertArtifactResults( artifactIterator, artifact );
        }
    }

    public void testMissingArtifact()
        throws Exception
    {
        Artifact artifact = createArtifact( "foo", "bar", "XP" );

        try
        {
            artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
            fail( "Should not have passed" );
        }
        catch ( IllegalStateException e )
        {
            assertTrue( true );
        }
    }
}
