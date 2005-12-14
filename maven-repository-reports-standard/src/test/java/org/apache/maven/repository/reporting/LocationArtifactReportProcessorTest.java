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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

/**
 * This class tests the LocationArtifactReportProcessor.
 *
 */
public class LocationArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactReporter reporter = new MockArtifactReporter();

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
    {
        //System.out.println("");
        //System.out.println("====================== PACKAGED POM TEST [SUCCESS] ========================");
        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "2.0" );
            Artifact artifact = new DefaultArtifact( "org.apache.maven", "maven-model", version, "compile", "jar", "",
                                                     handler );

            InputStream is = new FileInputStream( repository.getBasedir()
                + "org.apache.maven/maven-model/2.0/maven-model-2.0.pom" );
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            //System.out.println("PACKAGED POM SUCCESSES ---> " + reporter.getSuccesses());
            assertTrue( reporter.getSuccesses() == 1 );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the LocationArtifactReporter when the artifact is in the location specified in the
     * file system pom (but the jar file does not have a pom included in its package).
     */
    public void testLocationArtifactReporterSuccess()
    {
        //  System.out.println("");
        //   System.out.println("====================== FILE SYSTEM POM TEST [SUCCESS] ========================");

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0-alpha-1" );
            Artifact artifact = new DefaultArtifact( "groupId", "artifactId", version, "compile", "jar", "", handler );

            InputStream is = new FileInputStream( repository.getBasedir()
                + "groupId/artifactId/1.0-alpha-1/artifactId-1.0-alpha-1.pom" );
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            assertTrue( reporter.getSuccesses() == 1 );
            //    System.out.println("FILE SYSTEM POM SUCCESSES ---> " + reporter.getSuccesses());
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the LocationArtifactReporter when the artifact is not in the location specified
     * in the file system pom.  
     */
    public void testLocationArtifactReporterFailure()
    {
        //  System.out.println("");
        //  System.out.println("====================== FILE SYSTEM POM TEST [FAILURE] ========================");

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0-alpha-2" );
            Artifact artifact = new DefaultArtifact( "groupId", "artifactId", version, "compile", "jar", "", handler );

            InputStream is = new FileInputStream( repository.getBasedir()
                + "groupId/artifactId/1.0-alpha-2/artifactId-1.0-alpha-2.pom" );
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            assertTrue( reporter.getFailures() == 1 );
            //  System.out.println("FILE SYSTEM POM FAILURES ---> " + reporter.getFailures());
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match the
     * location in the file system pom but instead matches the specified location in the packaged pom.
     */
    public void testFsPomArtifactMatchFailure()
    {
        // System.out.println("");
        //  System.out.println("====================== FILE SYSTEM POM MATCH TEST [FAILURE] ========================");

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "2.0" );
            Artifact artifact = new DefaultArtifact( "org.apache.maven", "maven-archiver", version, "compile", "jar",
                                                     "", handler );

            InputStream is = new FileInputStream( repository.getBasedir()
                + "org.apache.maven/maven-archiver/2.0/maven-archiver-2.0.pom" );
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            assertTrue( reporter.getFailures() == 1 );
            //   System.out.println("FILE SYSTEM POM MATCH FAILURES ---> " + reporter.getFailures());
            //System.out.println("FILE SYSTEM POM MATCH SUCCESS ---> " + reporter.getSuccesses());
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match the
     * location specified in the packaged pom but matches the location specified in the file system pom.
     */
    public void testPkgPomArtifactMatchFailure()
    {
        //    System.out.println("");
        //    System.out.println("====================== PACKAGED POM MATCH TEST [FAILURE] ========================");

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "2.1" );
            Artifact artifact = new DefaultArtifact( "org.apache.maven", "maven-monitor", version, "compile", "jar",
                                                     "", handler );

            InputStream is = new FileInputStream( repository.getBasedir()
                + "org.apache.maven/maven-monitor/2.1/maven-monitor-2.1.pom" );
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            assertTrue( reporter.getFailures() == 1 );
            //     System.out.println("PACKAGED POM MATCH FAILURES ---> " + reporter.getFailures());
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the LocationArtifactReporter when the artifact's physical location does not match both the
     * location specified in the packaged pom and the location specified in the file system pom.
     */
    public void testBothPomArtifactMatchFailure()
    {
        //   System.out.println("");
        //  System.out.println("====================== BOTH POMS MATCH TEST [FAILURE] ========================");

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "2.1" );
            Artifact artifact = new DefaultArtifact( "org.apache.maven", "maven-project", version, "compile", "jar",
                                                     "", handler );

            InputStream is = new FileInputStream( repository.getBasedir()
                + "org.apache.maven/maven-project/2.1/maven-project-2.1.pom" );
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            assertTrue( reporter.getFailures() == 1 );
            //    System.out.println("BOTH POMS MATCH FAILURES ---> " + reporter.getFailures());
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the LocationArtifactReportProcessor when the artifact is located in the remote repository.
     */
  /*  public void testRemoteArtifactReportProcessorFailure()
    {

        ArtifactHandler handler = new DefaultArtifactHandler( remoteArtifactType );
        VersionRange version = VersionRange.createFromVersion( remoteArtifactVersion );
        Artifact artifact = new DefaultArtifact( remoteArtifactGroup, remoteArtifactId, version, remoteArtifactScope,
                                                 remoteArtifactType, "", handler );
        ArtifactRepository repository = new DefaultArtifactRepository( remoteRepoId, remoteRepoUrl,
                                                                       new DefaultRepositoryLayout() );
        try
        {
            URL url = new URL( remoteRepoUrl + remoteArtifactGroup + "/" + remoteArtifactId + "/"
                + remoteArtifactVersion + "/" + remoteArtifactId + "-" + remoteArtifactVersion + ".pom" );
            InputStream is = url.openStream();
            Reader reader = new InputStreamReader( is );
            Model model = pomReader.read( reader );

            artifactReportProcessor.processArtifact( model, artifact, reporter, repository );
            if ( reporter.getFailures() > 0 )
                assertTrue( reporter.getFailures() == 1 );

            if ( reporter.getSuccesses() > 0 )
                assertTrue( reporter.getSuccesses() == 1 );

            //    System.out.println("REMOTE ARTIFACT MATCH SUCCESSES ---> " + reporter.getSuccesses());

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
    */
}
