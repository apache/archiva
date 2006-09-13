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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Test the legacy artifact discoverer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id:LegacyArtifactDiscovererTest.java 437105 2006-08-26 17:22:22 +1000 (Sat, 26 Aug 2006) brett $
 */
public class LegacyArtifactDiscovererTest
    extends AbstractArtifactDiscovererTest
{
    private static final List JAVAX_SQL_BLACKLIST = Collections.singletonList( "javax.sql/**" );

    protected String getLayout()
    {
        return "legacy";
    }

    protected File getRepositoryFile()
    {
        return getTestFile( "src/test/legacy-repository" );
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

            if ( path.indexOf( ".svn" ) >= 0 )
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
        List artifacts = discoverArtifactsWithBlacklist();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "javax.sql/jars/jdbc-2.0.jar".equals( path.replace( '\\', '/' ) ) )
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
                assertEquals( "Check reason for kickout",
                              "Path does not match a legacy repository path for an artifact", dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not invalid-1.0.jar", "invalid-1.0.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithLongPath()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/jars/1.0/invalid-1.0.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout",
                              "Path does not match a legacy repository path for an artifact", dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not invalid-1.0.jar", "invalid-1.0.jar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithInvalidType()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/foo/invalid-1.0.foo".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path artifact type does not corresspond to an artifact type",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not invalid-1.0.foo", "invalid-1.0.foo".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithNoExtension()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/jars/no-extension".equals( path.replace( '\\', '/' ) ) )
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
            assertFalse( "Check not 'no-extension'", "no-extension".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithWrongExtension()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/jars/invalid-1.0.rar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path type does not match the extension",
                              dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid-1.0.rar'", "invalid-1.0.rar".equals( a.getFile().getName() ) );
        }
    }

    public void testKickoutWithNoVersion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            DiscovererPath dPath = (DiscovererPath) i.next();

            String path = dPath.getPath();

            if ( "invalid/jars/invalid.jar".equals( path.replace( '\\', '/' ) ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Path filename version is empty", dPath.getComment() );
            }
        }
        assertTrue( "Check kickout was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not 'invalid.jar'", "invalid.jar".equals( a.getFile().getName() ) );
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

    public void testTextualVersion()
        throws DiscovererException
    {
        List artifacts = discoverArtifactsWithSnapshots();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "UNKNOWN" ) ) );
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
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0-20050611.112233-1" ) ) );
    }

    public void testSnapshotExclusion()
        throws DiscovererException
    {
        List artifacts = discoverArtifacts();
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertFalse( "Check snapshot included",
                     artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0-20050611.112233-1" ) ) );
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

    public void testWrongArtifactPackaging()
        throws ComponentLookupException, DiscovererException
    {
        try
        {
            discoverer.buildArtifact( "org.apache.maven.test/jars/artifactId-1.0.jar.md5" );

            fail( "Artifact should be null for wrong package extension" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testNoArtifactId()
        throws DiscovererException
    {
        try
        {
            discoverer.buildArtifact( "groupId/jars/-1.0.jar" );

            fail( "Artifact should be null when artifactId is missing" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }

        try
        {
            discoverer.buildArtifact( "groupId/jars/1.0.jar" );

            fail( "Artifact should be null when artifactId is missing" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }
    }

    public void testNoType()
        throws ComponentLookupException, DiscovererException
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

    public void testSnapshot()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org.apache.maven.test/jars/maven-model-1.0-SNAPSHOT.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven.test", "maven-model", "1.0-SNAPSHOT" ), artifact );
    }

    public void testFinal()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org.apache.maven.test/jars/maven-model-1.0-final-20060606.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "org.apache.maven.test", "maven-model", "1.0-final-20060606" ), artifact );
    }

    public void testNormal()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "javax.sql/jars/jdbc-2.0.jar";

        Artifact artifact = discoverer.buildArtifact( testPath );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testJavadoc()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "javax.sql/javadoc.jars/jdbc-2.0-javadoc.jar";
    
        Artifact artifact = discoverer.buildArtifact( testPath );
    
        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0", "javadoc.jar", "javadoc" ), artifact );
    }

    public void testSources()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "javax.sql/java-sources/jdbc-2.0-sources.jar";
    
        Artifact artifact = discoverer.buildArtifact( testPath );
    
        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0", "java-source", "sources" ), artifact );
    }

    public void testPlugin()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "maven/plugins/maven-test-plugin-1.8.jar";
    
        Artifact artifact = discoverer.buildArtifact( testPath );
    
        assertEquals( createArtifact( "maven", "maven-test-plugin", "1.8", "plugin" ), artifact );
    }

    
    private List discoverArtifacts()
        throws DiscovererException
    {
        return discoverer.discoverArtifacts( repository, null, new SnapshotArtifactFilter() );
    }

    private List discoverArtifactsWithBlacklist()
        throws DiscovererException
    {
        return discoverer.discoverArtifacts( repository, JAVAX_SQL_BLACKLIST, new SnapshotArtifactFilter() );
    }

    private List discoverArtifactsWithSnapshots()
        throws DiscovererException
    {
        return discoverer.discoverArtifacts( repository, null, new AcceptAllArtifactFilter() );
    }
}
