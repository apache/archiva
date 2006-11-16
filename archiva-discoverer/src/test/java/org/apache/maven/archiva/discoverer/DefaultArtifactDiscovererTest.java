package org.apache.maven.archiva.discoverer;

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

import org.apache.maven.archiva.discoverer.filter.AcceptAllArtifactFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test the default artifact discoverer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id:DefaultArtifactDiscovererTest.java 437105 2006-08-26 17:22:22 +1000 (Sat, 26 Aug 2006) brett $
 */
public class DefaultArtifactDiscovererTest
    extends AbstractArtifactDiscovererTest
{
    private static final List JAVAX_BLACKLIST = Collections.singletonList( "javax/**" );

    protected String getLayout()
    {
        return "default";
    }

    protected File getRepositoryFile()
    {
        return getTestFile( "src/test/repository" );
    }

    public void testDefaultExcludes()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            boolean b = path.indexOf( "CVS" ) >= 0;
            if ( b )
            {
                found = true;
                assertEquals( "Check comment", "Artifact was in the specified list of exclusions", dPath.getComment() );
            }
        }
        assertTrue( "Check exclusion was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not CVS", a.getFile().getPath().indexOf( "CVS" ) >= 0 );
            assertFalse( "Check not .svn", a.getFile().getPath().indexOf( ".svn" ) >= 0 );
        }
    }

    public void testStandardExcludes()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "KEYS".equals( path ) )
            {
                found = true;
                assertEquals( "Check comment", "Artifact was in the specified list of exclusions", dPath.getComment() );
            }
        }
        assertTrue( "Check exclusion was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not KEYS", "KEYS".equals( a.getFile().getName() ) );
        }
    }

    public void testBlacklistedExclude()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithBlacklist( JAVAX_BLACKLIST );
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "javax/sql/jdbc/2.0/jdbc-2.0.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check comment is about blacklisting", "Artifact was in the specified list of exclusions",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check exclusion was found", found );

        assertFalse( "Check jdbc not included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
    }

    public void testKickoutWithShortPath()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/invalid-1.0.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path is too short to build an artifact from",
                              dPath.getComment() );

            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not invalid-1.0.jar", "invalid-1.0.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithWrongArtifactId()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar".equals(
                path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path filename does not correspond to an artifact",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not wrong jar",
                         "wrong-artifactId-1.0-20050611.112233-1.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithNoType()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/invalid/1/invalid-1".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path filename does not have an extension",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid-1'", "invalid-1".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithWrongVersion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/invalid/1.0/invalid-2.0.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Built artifact version does not match path version",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid-2.0.jar'", "invalid-2.0.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithLongerVersion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/invalid/1.0/invalid-1.0b.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path version does not corresspond to an artifact version",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid-1.0b.jar'", "invalid-1.0b.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithWrongSnapshotVersion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout",
                              "Failed to create a snapshot artifact: invalid:invalid:jar:1.0:runtime",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid-1.0.jar'", "invalid-1.0.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithSnapshotBaseVersion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar".equals(
                path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout",
                              "Built snapshot artifact base version does not match path version: invalid:invalid:jar:1.0-SNAPSHOT:runtime; should have been version: 1.0-20050611.123456-1",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid-1.0-20050611-123456-1.jar'",
                         "invalid-1.0-20050611.123456-1.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testInclusion()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0" ) ) );
    }

    public void testArtifactWithClassifier()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client" ) ) );
    }

    public void testJavaSourcesInclusion()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains(
            createArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources" ) ) );
    }

    public void testTestSourcesInclusion()
    throws DiscovererException
{
    List artifacts = discoverArtifactsWithSnapshots();
    assertNotNull( "Check artifacts not null", artifacts );

    assertTrue( "Check normal included", artifacts.contains(
        createArtifact( "org.apache.maven", "testing", "1.0", "java-source", "test-sources" ) ) );
}

    public void testDistributionInclusion()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check zip included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip" ) ) );

        assertTrue( "Check tar.gz included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz" ) ) );
    }

    public void testSnapshotInclusion()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertTrue( "Check snapshot included",
                    artifacts.contains( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1" ) ) );
    }

    public void testSnapshotInclusionWithClassifier()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check snapshot included", artifacts.contains(
            createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc" ) ) );
    }

    public void testSnapshotExclusion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertFalse( "Check snapshot included",
                     artifacts.contains( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ) ) );
    }

    public void testFileSet()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            assertNotNull( "Check file is set", artifact.getFile() );
        }
    }

    public void testRepositorySet()
        throws MalformedURLException, DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        String url = repository.getUrl();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            assertNotNull( "Check repository set", artifact.getRepository() );
            assertEquals( "Check repository url is correct", url, artifact.getRepository().getUrl() );
        }
    }

    public void testStandalonePoms()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();

        // cull down to actual artifacts (only standalone poms will have type = pom)
        Map keyedArtifacts = new HashMap();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            String key = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();
            if ( !"pom".equals( a.getType() ) || !keyedArtifacts.containsKey( key ) )
            {
                keyedArtifacts.put( key, a );
            }
        }

        List models = new ArrayList();

        for ( Iterator i = keyedArtifacts.values().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( "pom".equals( a.getType() ) )
            {
                models.add( a );
            }
        }

        assertEquals( 4, models.size() );

        // Define order we expect
        Collections.sort( models );

        Iterator itr = models.iterator();
        Artifact model = (Artifact) itr.next();
        assertEquals( "org.apache.maven", model.getGroupId() );
        assertEquals( "B", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
        model = (Artifact) itr.next();
        assertEquals( "org.apache.maven", model.getGroupId() );
        assertEquals( "B", model.getArtifactId() );
        assertEquals( "2.0", model.getVersion() );
        model = (Artifact) itr.next();
        assertEquals( "org.apache.maven", model.getGroupId() );
        assertEquals( "discovery", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
        model = (Artifact) itr.next();
        assertEquals( "org.apache.testgroup", model.getGroupId() );
        assertEquals( "discovery", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
    }

    public void testShortPath()
        throws ComponentLookupException
    {
        try
        {
            discoverer.buildArtifact( "invalid/invalid-1.0.jar" );

            fail( "Artifact should be null for short paths" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testWrongArtifactId()
        throws ComponentLookupException
    {

        try
        {
            discoverer.buildArtifact( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar" );

            fail( "Artifact should be null for wrong ArtifactId" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testNoType()
        throws ComponentLookupException
    {
        try
        {
            discoverer.buildArtifact( "invalid/invalid/1/invalid-1" );

            fail( "Artifact should be null for no type" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testWrongVersion()
        throws ComponentLookupException
    {
        try
        {
            discoverer.buildArtifact( "invalid/invalid/1.0/invalid-2.0.jar" );

            fail( "Artifact should be null for wrong version" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testLongVersion()
        throws ComponentLookupException
    {
        try
        {
            discoverer.buildArtifact( "invalid/invalid/1.0/invalid-1.0b.jar" );

            fail( "Artifact should be null for long version" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testWrongSnapshotVersion()
        throws ComponentLookupException
    {
        try
        {
            discoverer.buildArtifact( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar" );

            fail( "Artifact should be null for wrong snapshot version" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testSnapshotBaseVersion()
        throws ComponentLookupException
    {
        try
        {
            discoverer.buildArtifact( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar" );

            fail( "Artifact should be null for snapshot base version" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testPathWithClassifier()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org/apache/maven/some-ejb/1.0/some-ejb-1.0-client.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client" ), artifact );
    }

    public void testWithJavaSourceInclusion()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org/apache/maven/testing/1.0/testing-1.0-sources.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources" ), artifact );
    }

    public void testDistributionArtifacts()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org/apache/maven/testing/1.0/testing-1.0.tar.gz";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz" ), artifact );

        testPath = "org/apache/maven/testing/1.0/testing-1.0.zip";

        artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip" ), artifact );
    }

    public void testSnapshot()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ), artifact );

        testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1.jar";

        artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1" ), artifact );
    }

    public void testNormal()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "javax/sql/jdbc/2.0/jdbc-2.0.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testSnapshotWithClassifier()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1-javadoc.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc" ),
                      artifact );
    }

    private List discoverArtifactsWithSnapshots()
        throws DiscovererException
    {
        return discoverer.discoverArtifacts( repository, null, new AcceptAllArtifactFilter() );
    }

    private List discoverArtifactsWithBlacklist( List list )
        throws DiscovererException
    {
        return discoverer.discoverArtifacts( repository, list, new SnapshotArtifactFilter() );
    }

    private List discoverArtifacts()
        throws DiscovererException
    {
        return discoverer.discoverArtifacts( repository, null, new SnapshotArtifactFilter() );
    }
}
