package org.apache.maven.repository.discovery;

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
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Test the default artifact discoverer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo test location of poms, checksums
 */
public class DefaultArtifactDiscovererTest
    extends AbstractArtifactDiscovererTest
{
    protected String getLayout()
    {
        return "default";
    }

    protected File getRepositoryFile()
    {
        return getTestFile( "src/test/repository" );
    }

    public void testDefaultExcludes()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            boolean b = path.indexOf( ".svn" ) >= 0;
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
            assertFalse( "Check not .svn", a.getFile().getPath().indexOf( ".svn" ) >= 0 );
        }
    }

    public void testStandardExcludes()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, "javax/**", false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0" ) ) );
    }

    public void testArtifactWithClassifier()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client" ) ) );
    }

    public void testJavaSourcesInclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains(
            createArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources" ) ) );
    }

    public void testDistributionInclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check zip included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip" ) ) );

        assertTrue( "Check tar.gz included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz" ) ) );
    }

    public void testSnapshotInclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertTrue( "Check snapshot included",
                    artifacts.contains( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1" ) ) );
    }

    public void testSnapshotInclusionWithClassifier()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check snapshot included", artifacts.contains(
            createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc" ) ) );
    }

    public void testSnapshotExclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertFalse( "Check snapshot included",
                     artifacts.contains( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ) ) );
    }

    public void testFileSet()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            assertNotNull( "Check file is set", artifact.getFile() );
        }
    }

    public void testRepositorySet()
        throws MalformedURLException
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
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
    {
        List models = discoverer.discoverStandalonePoms( repository, null, false );
        assertEquals( 4, models.size() );

        // Define order we expect
        Collections.sort( models, new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                Model m1 = (Model) o1;
                Model m2 = (Model) o2;

                int result = m1.getGroupId().compareTo( m2.getGroupId() );
                if ( result == 0 )
                {
                    result = m1.getArtifactId().compareTo( m2.getArtifactId() );
                }
                if ( result == 0 )
                {
                    result = m1.getVersion().compareTo( m2.getVersion() );
                }
                return result;
            }
        } );

        Iterator itr = models.iterator();
        Model model = (Model) itr.next();
        assertEquals( "org.apache.maven", model.getGroupId() );
        assertEquals( "B", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
        model = (Model) itr.next();
        assertEquals( "org.apache.maven", model.getGroupId() );
        assertEquals( "B", model.getArtifactId() );
        assertEquals( "2.0", model.getVersion() );
        model = (Model) itr.next();
        assertEquals( "org.apache.maven", model.getGroupId() );
        assertEquals( "discovery", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
        model = (Model) itr.next();
        assertEquals( "org.apache.testgroup", model.getGroupId() );
        assertEquals( "discovery", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
    }

    public void testShortPath()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid-1.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for short paths", artifact );
    }

    public void testWrongArtifactId()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong ArtifactId", artifact );
    }

    public void testNoType()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1/invalid-1";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for no type", artifact );
    }

    public void testWrongVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0/invalid-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong version", artifact );
    }

    public void testLongVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0/invalid-1.0b.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for long version", artifact );
    }

    public void testWrongSnapshotVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong snapshot version", artifact );
    }

    public void testSnapshotBaseVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for snapshot base version", artifact );
    }

    public void testPathWithClassifier()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/some-ejb/1.0/some-ejb-1.0-client.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with classifier error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client" ), artifact );
    }

    public void testWithJavaSourceInclusion()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/testing/1.0/testing-1.0-sources.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with java source inclusion error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources" ), artifact );
    }

    public void testDistributionArtifacts()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/testing/1.0/testing-1.0.tar.gz";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "tar.gz distribution artifact error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz" ), artifact );

        testPath = "org/apache/maven/testing/1.0/testing-1.0.zip";

        artifact = getArtifactFromPath( testPath );

        assertNotNull( "zip distribution artifact error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip" ), artifact );
    }

    public void testSnapshot()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with invalid snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ), artifact );

        testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1.jar";

        artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1" ), artifact );
    }

    public void testNormal()
        throws ComponentLookupException
    {
        String testPath = "javax/sql/jdbc/2.0/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testUpdatedInRepository()
        throws ComponentLookupException
    {
        String testPath = "javax/sql/jdbc/2.0/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testNotUpdatedInRepository()
        throws ComponentLookupException
    {
        String testPath = "javax/sql/jdbc/2.0/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testNotUpdatedInRepositoryForcedDiscovery()
        throws ComponentLookupException
    {
        String testPath = "javax/sql/jdbc/2.0/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testSnapshotWithClassifier()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1-javadoc.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with snapshot and classifier error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc" ),
                      artifact );
    }

    private Artifact getArtifactFromPath( String path )
    {
        try
        {
            return discoverer.buildArtifact( path );
        }
        catch ( DiscovererException e )
        {
            return null;
        }
    }
}
