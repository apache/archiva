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
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Iterator;

/**
 * This class tests the OldArtifactReportProcessor.
 */
public class OldArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ReportingDatabase reportDatabase;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "old-artifact" );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "old-artifact" );
        reportDatabase = new ReportingDatabase( reportGroup );
    }

    public void testOldArtifact()
    {
        Artifact artifact = createArtifact( "org.apache.maven", "maven-model", "2.0" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check notices", 1, reportDatabase.getNumNotices() );
        ArtifactResults results = (ArtifactResults) reportDatabase.getArtifactIterator().next();
        assertEquals( artifact.getArtifactId(), results.getArtifactId() );
        assertEquals( artifact.getGroupId(), results.getGroupId() );
        assertEquals( artifact.getVersion(), results.getVersion() );
        assertEquals( 1, results.getNotices().size() );
        Iterator i = results.getNotices().iterator();
        Result result = (Result) i.next();
        assertEquals( "old-artifact", result.getProcessor() );
    }

    public void testNewArtifact()
        throws Exception
    {
        File repository = getTestFile( "target/test-repository" );

        FileUtils.copyDirectoryStructure( getTestFile( "src/test/repository/groupId" ),
                                          new File( repository, "groupId" ) );

        Artifact artifact = createArtifactFromRepository( repository, "groupId", "artifactId", "1.0-alpha-1" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
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
