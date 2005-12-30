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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * This class tests the InvalidPomArtifactReportProcessor class.
 */
public class InvalidPomArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactReporter reporter = new MockArtifactReporter();

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "invalid-pom" );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is an invalid pom.
     */
    public void testInvalidPomArtifactReportProcessorFailure()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "pom" );
            VersionRange version = VersionRange.createFromVersion( "1.0-alpha-3" );
            Artifact artifact =
                new DefaultArtifact( "org.apache.maven", "artifactId", version, "compile", "pom", "", handler );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            assertTrue( reporter.getFailures() == 1 );
            //System.out.println("INVALID POM ARTIFACT FAILURES --->> " + reporter.getFailures());

        }
        catch ( Exception e )
        {

        }
    }


    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is a valid pom.
     */
    public void testInvalidPomArtifactReportProcessorSuccess()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "pom" );
            VersionRange version = VersionRange.createFromVersion( "1.0-alpha-2" );
            Artifact artifact = new DefaultArtifact( "groupId", "artifactId", version, "compile", "pom", "", handler );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            assertTrue( reporter.getSuccesses() == 1 );
            //System.out.println("VALID POM ARTIFACT SUCCESS --->> " + reporter.getSuccesses());

        }
        catch ( Exception e )
        {

        }
    }


    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is not a pom.
     */
    public void testNotAPomArtifactReportProcessorSuccess()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0-alpha-1" );
            Artifact artifact = new DefaultArtifact( "groupId", "artifactId", version, "compile", "jar", "", handler );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );
            assertTrue( reporter.getWarnings() == 1 );
            //System.out.println("NOT A POM ARTIFACT WARNINGS --->> " + reporter.getWarnings());

        }
        catch ( Exception e )
        {

        }
    }

    /**
     * Test the InvalidPomArtifactReportProcessor when the pom is located in 
     * a remote repository.
     */
    /* public void testRemotePomArtifactReportProcessorSuccess(){
        try{
            ArtifactHandler handler = new DefaultArtifactHandler( "pom" );
            VersionRange version = VersionRange.createFromVersion( remoteArtifactVersion );
            Artifact artifact = new DefaultArtifact( remoteArtifactGroup, remoteArtifactId, version, remoteArtifactScope,
                                                     "pom", "", handler );
            ArtifactRepository repository = new DefaultArtifactRepository( remoteRepoId, remoteRepoUrl,
                                                                           new DefaultRepositoryLayout() );
        
            artifactReportProcessor.processArtifact(null, artifact, reporter, repository);
            if(reporter.getSuccesses() == 1)
                assertTrue(reporter.getSuccesses() == 1);
                        
            //System.out.println("Remote pom SUCCESS --> " + reporter.getSuccesses());
        }catch(Exception e){
            
        }
    }
    */
}
