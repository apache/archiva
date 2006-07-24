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
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

/**
 * Test the legacy artifact discoverer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class LegacyArtifactDiscovererTest
    extends AbstractArtifactDiscovererTest
{
    protected String getLayout()
    {
        return "legacy";
    }

    protected File getRepositoryFile()
    {
        return getTestFile( "src/test/legacy-repository" );
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
        List artifacts = discoverer.discoverArtifacts( repository, "javax.sql/**", false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
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
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0" ) ) );
    }

    public void testTextualVersion()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included",
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "UNKNOWN" ) ) );
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
                    artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0-20050611.112233-1" ) ) );
    }

    public void testSnapshotExclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertFalse( "Check snapshot included",
                     artifacts.contains( createArtifact( "org.apache.maven", "testing", "1.0-20050611.112233-1" ) ) );
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

    public void testWrongArtifactPackaging()
        throws ComponentLookupException, DiscovererException
    {
        try
        {
            getArtifactFromPath( "org.apache.maven.test/jars/artifactId-1.0.jar.md5" );

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
            getArtifactFromPath( "groupId/jars/-1.0.jar" );

            fail( "Artifact should be null when artifactId is missing" );
        }
        catch ( DiscovererException e )
        {
            // excellent
        }

        try
        {
            getArtifactFromPath( "groupId/jars/1.0.jar" );

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
            getArtifactFromPath( "invalid/invalid/1/invalid-1" );

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

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with invalid snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven.test", "maven-model", "1.0-SNAPSHOT" ), artifact );
    }

    public void testFinal()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "org.apache.maven.test/jars/maven-model-1.0-final-20060606.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with invalid snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven.test", "maven-model", "1.0-final-20060606" ), artifact );
    }

    public void testNormal()
        throws ComponentLookupException, DiscovererException
    {
        String testPath = "javax.sql/jars/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    private Artifact getArtifactFromPath( String path )
        throws DiscovererException
    {
        return discoverer.buildArtifact( path );
    }
}
