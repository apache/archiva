package org.apache.maven.archiva.discoverer.consumers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.discoverer.DiscovererConsumer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.DiscovererStatistics;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * GenericArtifactConsumerTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GenericArtifactConsumerTest
    extends AbstractConsumerTestCase
{
    private MockArtifactConsumer getMockArtifactConsumer() throws Exception
    {
        return (MockArtifactConsumer) lookup(DiscovererConsumer.ROLE, "mock-artifact");
    }
    
    public void testScanLegacy()
        throws Exception
    {
        ArtifactRepository repository = getLegacyRepository();
        List consumers = new ArrayList();

        MockArtifactConsumer mockConsumer = getMockArtifactConsumer(); 

        consumers.add( mockConsumer );

        DiscovererStatistics stats = discoverer.scanRepository( repository, consumers, true );

        assertNotNull( stats );

        assertNotNull( consumers );

        Iterator it = mockConsumer.getFailureMap().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String path = (String) entry.getKey();
            String msg = (String) entry.getValue();
            System.out.println( "Failure: " + path + " -> " + msg );
        }

        assertEquals( 3, mockConsumer.getFailureMap().size() );

        assertEquals( "Path does not match a legacy repository path for an artifact", mockConsumer.getFailureMap()
            .get( "invalid/invalid-1.0.jar" ) );
        assertEquals( "Path filename version is empty", mockConsumer.getFailureMap().get( "invalid/jars/invalid.jar" ) );
        assertEquals( "Path does not match a legacy repository path for an artifact", mockConsumer.getFailureMap()
            .get( "invalid/jars/1.0/invalid-1.0.jar" ) );

        assertEquals( 10, mockConsumer.getArtifactMap().size() );
    }

    public void testScanDefault()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();

        MockArtifactConsumer mockConsumer = getMockArtifactConsumer();

        consumers.add( mockConsumer );

        DiscovererStatistics stats = discoverer.scanRepository( repository, consumers, true );

        // Test Statistics

        assertNotNull( stats );

        assertEquals( 31, stats.getFilesConsumed() );
        assertEquals( 0, stats.getFilesSkipped() );
        assertEquals( 31, stats.getFilesIncluded() );
        assertTrue( stats.getElapsedMilliseconds() > 0 );
        assertTrue( stats.getTimestampFinished() >= stats.getTimestampStarted() );
        assertTrue( stats.getTimestampStarted() > 0 );

        // Test gathered information from Mock consumer.

        Iterator it;

        assertNotNull( consumers );

        it = mockConsumer.getFailureMap().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String path = (String) entry.getKey();
            String msg = (String) entry.getValue();
            System.out.println( "Failure: " + path + " -> " + msg );
        }

        assertEquals( 6, mockConsumer.getFailureMap().size() );

        assertEquals( "Failed to create a snapshot artifact: invalid:invalid:jar:1.0:runtime", mockConsumer
            .getFailureMap().get( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar" ) );
        assertEquals( "Path is too short to build an artifact from.", mockConsumer.getFailureMap()
            .get( "invalid/invalid-1.0.jar" ) );
        assertEquals( "Built artifact version does not match path version", mockConsumer.getFailureMap()
            .get( "invalid/invalid/1.0/invalid-2.0.jar" ) );

        assertEquals( 25, mockConsumer.getArtifactMap().size() );

        // Test for known include artifacts

        Collection artifacts = mockConsumer.getArtifactMap().values();
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "jar", null, artifacts );
        assertHasArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client", artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources", artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "java-source", "test-sources", artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip", null, artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz", null, artifacts );
        assertHasArtifact( "javax.sql", "jdbc", "2.0", "jar", null, artifacts );
        assertHasArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", null, artifacts );
        assertHasArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc", artifacts );

        // Test for known excluded files and dirs to validate exclusions.

        it = mockConsumer.getArtifactMap().values().iterator();
        while ( it.hasNext() )
        {
            Artifact a = (Artifact) it.next();
            assertTrue( "Artifact " + a + " should have it's .getFile() set.", a.getFile() != null );
            assertTrue( "Artifact " + a + " should have it's .getRepository() set.", a.getRepository() != null );
            assertTrue( "Artifact " + a + " should have non-null repository url.", a.getRepository().getUrl() != null );
            assertFalse( "Check not CVS", a.getFile().getPath().indexOf( "CVS" ) >= 0 );
            assertFalse( "Check not .svn", a.getFile().getPath().indexOf( ".svn" ) >= 0 );
        }
    }

    private void assertHasArtifact( String groupId, String artifactId, String version, String type, String classifier,
                                    Collection collection )
    {
        Artifact artifact = createArtifact( groupId, artifactId, version, type, classifier );
        assertTrue( "Contains " + artifact, collection.contains( artifact ) );
    }

    /*  This relies on File.setLastModified(long) which does not work reliably on all platforms.
     *  Notably linux and various early flavors of OSX.
     *    - Joakim
     *    
     *  TODO: Research alternative way to test this.
     */
    public void disabledTestScanDefaultUpdatesOnly()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();

        // Set all files in repository to August 22 1972 (old date)
        DiscovererStatistics stats;
        makeRepositoryOld( repository );
        makeFileNew( repository, "org/apache/maven/update/test-updated/1.0/test-updated-1.0.pom" );
        makeFileNew( repository, "org/apache/maven/update/test-updated/1.0/test-updated-1.0.jar" );

        // Now do the normal thing.

        List consumers = new ArrayList();

        MockArtifactConsumer mockConsumer = getMockArtifactConsumer();

        consumers.add( mockConsumer );

        stats = discoverer.scanRepository( repository, consumers, true );

        // Test Statistics

        assertNotNull( stats );

        assertEquals( 2, stats.getFilesConsumed() );
        assertEquals( 23, stats.getFilesSkipped() );
        assertEquals( 2, stats.getFilesIncluded() );
        assertTrue( stats.getElapsedMilliseconds() > 0 );
        assertTrue( stats.getTimestampFinished() >= stats.getTimestampStarted() );
        assertTrue( stats.getTimestampStarted() > 0 );

        // Test gathered information from Mock consumer.

        Iterator it;

        assertNotNull( consumers );

        it = mockConsumer.getFailureMap().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String path = (String) entry.getKey();
            String msg = (String) entry.getValue();
            System.out.println( "Failure: " + path + " -> " + msg );
        }

        assertEquals( 6, mockConsumer.getFailureMap().size() );

        assertEquals( "Failed to create a snapshot artifact: invalid:invalid:jar:1.0:runtime", mockConsumer
            .getFailureMap().get( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar" ) );
        assertEquals( "Path is too short to build an artifact from.", mockConsumer.getFailureMap()
            .get( "invalid/invalid-1.0.jar" ) );
        assertEquals( "Built artifact version does not match path version", mockConsumer.getFailureMap()
            .get( "invalid/invalid/1.0/invalid-2.0.jar" ) );

        assertEquals( 25, mockConsumer.getArtifactMap().size() );

        // Test for known excluded files and dirs to validate exclusions.

        it = mockConsumer.getArtifactMap().values().iterator();
        while ( it.hasNext() )
        {
            Artifact a = (Artifact) it.next();
            assertFalse( "Check not CVS", a.getFile().getPath().indexOf( "CVS" ) >= 0 );
            assertFalse( "Check not .svn", a.getFile().getPath().indexOf( ".svn" ) >= 0 );
        }
    }

    private void makeFileNew( ArtifactRepository repository, String path )
    {
        File file = new File( repository.getBasedir(), path );
        file.setLastModified( System.currentTimeMillis() );
    }

    private void makeRepositoryOld( ArtifactRepository repository )
        throws DiscovererException
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set( 1972, Calendar.AUGUST, 22, 1, 1, 1 );
        long oldTime = cal.getTimeInMillis();

        DiscovererStatistics stats = new DiscovererStatistics( repository );
        stats.setTimestampFinished( oldTime + 5000 );
        stats.save();

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repository.getBasedir() );
        scanner.addDefaultExcludes();
        scanner.setIncludes( new String[] { "**/*" } );
        scanner.scan();
        String files[] = scanner.getIncludedFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            File file = new File( files[i] );

            if ( !file.setLastModified( oldTime ) )
            {
                fail( "Your platform apparently does not support the File.setLastModified(long) method." );
            }
        }
    }
}
