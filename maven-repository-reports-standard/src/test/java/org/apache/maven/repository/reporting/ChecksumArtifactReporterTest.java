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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Iterator;

public class ChecksumArtifactReporterTest
    extends AbstractChecksumArtifactReporterTest
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ArtifactReporter reporter = new MockArtifactReporter();

    public ChecksumArtifactReporterTest()
    {

    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "default" );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    public void testCreateChecksumFile()
    {
        assertTrue( createChecksumFile( "VALID" ) );
        assertTrue( createChecksumFile( "INVALID" ) );
    }

    /**
     * Test the ChecksumArtifactReporter when the checksum files are valid.
     */
    public void testChecksumArtifactReporterSuccess()
    {
        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "validArtifact", version, "compile", "jar", "",
                                                     handler );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", System.getProperty( "basedir" )
                + "/src/test/repository/", new DefaultRepositoryLayout() );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );

            Iterator iter = reporter.getArtifactSuccessIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            System.out.println( "Number of success --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Test the ChecksumArtifactReporter when the checksum files are invalid.
     */
    public void testChecksumArtifactReporterFailed()
    {

        try
        {
            ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            VersionRange version = VersionRange.createFromVersion( "1.0" );
            Artifact artifact = new DefaultArtifact( "checksumTest", "invalidArtifact", version, "compile", "jar", "",
                                                     handler );
            ArtifactRepository repository = new DefaultArtifactRepository( "repository", System.getProperty( "basedir" )
                + "/src/test/repository/", new DefaultRepositoryLayout() );

            artifactReportProcessor.processArtifact( null, artifact, reporter, repository );

            Iterator iter = reporter.getArtifactFailureIterator();
            int ctr = 0;
            while ( iter.hasNext() )
            {
                ArtifactResult result = (ArtifactResult) iter.next();
                ctr++;
            }
            System.out.println( "Number of failures --- " + ctr );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
