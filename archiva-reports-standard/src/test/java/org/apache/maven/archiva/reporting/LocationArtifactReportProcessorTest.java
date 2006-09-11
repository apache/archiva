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
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This class tests the LocationArtifactReportProcessor.
 */
public class LocationArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ReportingDatabase reportDatabase;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "artifact-location" );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "health" );
        reportDatabase = new ReportingDatabase( reportGroup );
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location matches the location specified
     * both in the file system pom and in the pom included in the package.
     */
    public void testPackagedPomLocationArtifactReporterSuccess()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "org.apache.maven", "maven-model", "2.0" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom (but the jar file does not have a pom included in its package).
     */
    public void testLocationArtifactReporterSuccess()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1" );
        Artifact pomArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "pom" );

        Model model = readPom( repository.pathOf( pomArtifact ) );
        artifactReportProcessor.processArtifact( artifact, model, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom, but the pom itself is passed in.
     */
    public void testLocationArtifactReporterSuccessPom()
        throws IOException, XmlPullParserException
    {
        Artifact pomArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "pom" );

        Model model = readPom( repository.pathOf( pomArtifact ) );
        artifactReportProcessor.processArtifact( pomArtifact, model, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom, with a classifier.
     */
    public void testLocationArtifactReporterSuccessClassifier()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "java-source" );
        Artifact pomArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "pom" );

        Model model = readPom( repository.pathOf( pomArtifact ) );
        artifactReportProcessor.processArtifact( artifact, model, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom, with a classifier.
     */
    public void testLocationArtifactReporterSuccessZip()
        throws IOException, XmlPullParserException
    {
        Artifact artifact =
            createArtifactWithClassifier( "groupId", "artifactId", "1.0-alpha-1", "distribution-zip", "src" );
        Artifact pomArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "pom" );

        Model model = readPom( repository.pathOf( pomArtifact ) );
        artifactReportProcessor.processArtifact( artifact, model, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom, with a classifier.
     */
    public void testLocationArtifactReporterSuccessTgz()
        throws IOException, XmlPullParserException
    {
        Artifact artifact =
            createArtifactWithClassifier( "groupId", "artifactId", "1.0-alpha-1", "distribution-tgz", "src" );
        Artifact pomArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "pom" );

        Model model = readPom( repository.pathOf( pomArtifact ) );
        artifactReportProcessor.processArtifact( artifact, model, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
        assertEquals( "Check no notices", 0, reportDatabase.getNumNotices() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is not in the location specified
     * in the file system pom.
     */
    public void testLocationArtifactReporterFailure()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-2" );
        Artifact pomArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-2", "pom" );

        try
        {
            Model model = readPom( repository.pathOf( pomArtifact ) );
            artifactReportProcessor.processArtifact( artifact, model, reportDatabase );
            fail( "Should not have passed the artifact" );
        }
        catch ( IllegalStateException e )
        {
            // correct!
        }
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match the
     * location in the file system pom but instead matches the specified location in the packaged pom.
     */
    public void testFsPomArtifactMatchFailure()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "org.apache.maven", "maven-archiver", "2.0" );

        Artifact pomArtifact = createArtifact( "org.apache.maven", "maven-archiver", "2.0", "pom" );
        Model model = readPom( repository.pathOf( pomArtifact ) );
        artifactReportProcessor.processArtifact( artifact, model, reportDatabase );
        assertEquals( 1, reportDatabase.getNumFailures() );
    }

    private Model readPom( String path )
        throws IOException, XmlPullParserException
    {
        Reader reader = new FileReader( new File( repository.getBasedir(), path ) );
        Model model = new MavenXpp3Reader().read( reader );
        // hokey inheritence to avoid some errors right now
        if ( model.getGroupId() == null )
        {
            model.setGroupId( model.getParent().getGroupId() );
        }
        if ( model.getVersion() == null )
        {
            model.setVersion( model.getParent().getVersion() );
        }
        return model;
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match the
     * location specified in the packaged pom but matches the location specified in the file system pom.
     */
    public void testPkgPomArtifactMatchFailure()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "org.apache.maven", "maven-monitor", "2.1" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 1, reportDatabase.getNumFailures() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match both the
     * location specified in the packaged pom and the location specified in the file system pom.
     */
    public void testBothPomArtifactMatchFailure()
        throws IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "org.apache.maven", "maven-project", "2.1" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 1, reportDatabase.getNumFailures() );
    }

}
