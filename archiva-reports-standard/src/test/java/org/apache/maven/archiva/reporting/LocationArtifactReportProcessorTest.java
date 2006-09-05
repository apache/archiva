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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
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

    private ArtifactReporter reporter = new DefaultArtifactReporter();

    private MavenXpp3Reader pomReader;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "artifact-location" );
        pomReader = new MavenXpp3Reader();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
        artifactReportProcessor = null;
        pomReader = null;
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location matches the location specified
     * both in the file system pom and in the pom included in the package.
     */
    public void testPackagedPomLocationArtifactReporterSuccess()
        throws ReportProcessorException, IOException, XmlPullParserException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "2.0" );
        Artifact artifact =
            new DefaultArtifact( "org.apache.maven", "maven-model", version, "compile", "jar", "", handler );

        String path = "org/apache/maven/maven-model/2.0/maven-model-2.0.pom";
        Model model = readPom( path );

        artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom (but the jar file does not have a pom included in its package).
     */
    public void testLocationArtifactReporterSuccess()
        throws ReportProcessorException, IOException, XmlPullParserException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "1.0-alpha-1" );
        Artifact artifact = new DefaultArtifact( "groupId", "artifactId", version, "compile", "jar", "", handler );

        String path = "groupId/artifactId/1.0-alpha-1/artifactId-1.0-alpha-1.pom";
        Model model = readPom( path );

        artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumSuccesses() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact is not in the location specified
     * in the file system pom.
     */
    public void testLocationArtifactReporterFailure()
        throws IOException, XmlPullParserException, ReportProcessorException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "1.0-alpha-2" );
        Artifact artifact = new DefaultArtifact( "groupId", "artifactId", version, "compile", "jar", "", handler );

        String path = "groupId/artifactId/1.0-alpha-2/artifactId-1.0-alpha-2.pom";
        Model model = readPom( path );

        artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumFailures() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match the
     * location in the file system pom but instead matches the specified location in the packaged pom.
     */
    public void testFsPomArtifactMatchFailure()
        throws IOException, ReportProcessorException, XmlPullParserException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "2.0" );
        Artifact artifact =
            new DefaultArtifact( "org.apache.maven", "maven-archiver", version, "compile", "jar", "", handler );

        String path = "org/apache/maven/maven-archiver/2.0/maven-archiver-2.0.pom";
        Model model = readPom( path );

        artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumFailures() );
    }

    private Model readPom( String path )
        throws IOException, XmlPullParserException
    {
        Reader reader = new FileReader( new File( repository.getBasedir(), path ) );
        Model model = pomReader.read( reader );
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
        throws IOException, XmlPullParserException, ReportProcessorException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "2.1" );
        Artifact artifact =
            new DefaultArtifact( "org.apache.maven", "maven-monitor", version, "compile", "jar", "", handler );

        String path = "org/apache/maven/maven-monitor/2.1/maven-monitor-2.1.pom";
        Model model = readPom( path );

        artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumFailures() );
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match both the
     * location specified in the packaged pom and the location specified in the file system pom.
     */
    public void testBothPomArtifactMatchFailure()
        throws IOException, XmlPullParserException, ReportProcessorException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
        VersionRange version = VersionRange.createFromVersion( "2.1" );
        Artifact artifact =
            new DefaultArtifact( "org.apache.maven", "maven-project", version, "compile", "jar", "", handler );

        String path = "org/apache/maven/maven-project/2.1/maven-project-2.1.pom";
        Model model = readPom( path );

        artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
        assertEquals( 1, reporter.getNumFailures() );
    }
}
