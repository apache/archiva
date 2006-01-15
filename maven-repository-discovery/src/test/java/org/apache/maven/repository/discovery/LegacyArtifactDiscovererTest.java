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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Iterator;
import java.util.List;
import java.net.MalformedURLException;
import java.io.File;

/**
 * Test the legacy artifact discoverer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo share as much as possible with default via abstract test case
 */
public class LegacyArtifactDiscovererTest
    extends PlexusTestCase
{
    private ArtifactDiscoverer discoverer;

    private ArtifactFactory factory;

    private ArtifactRepository repository;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (ArtifactDiscoverer) lookup( ArtifactDiscoverer.ROLE, "legacy" );

        factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        File basedir = getTestFile( "src/test/legacy-repository" );

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );
        repository = factory.createArtifactRepository( "discoveryRepo", "file://" + basedir, layout, null, null );
    }

    public void testDefaultExcludes()
    {
        List artifacts = discoverer.discoverArtifacts( repository, null, false );
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            String path = (String) i.next();

            found = path.indexOf( ".svn" ) >= 0;
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
            String path = (String) i.next();

            found = "KEYS".equals( path );
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
            String path = (String) i.next();

            found = "javax.sql/jars/jdbc-2.0.jar".equals( path.replace( '\\', '/' ) );
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
            String path = (String) i.next();

            found = "invalid/invalid-1.0.jar".equals( path.replace( '\\', '/' ) );
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
            String path = (String) i.next();

            found = "invalid/jars/1.0/invalid-1.0.jar".equals( path.replace( '\\', '/' ) );
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
            String path = (String) i.next();

            found = "invalid/foo/invalid-1.0.foo".equals( path.replace( '\\', '/' ) );
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
            String path = (String) i.next();

            found = "invalid/jars/no-extension".equals( path.replace( '\\', '/' ) );
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
            String path = (String) i.next();

            found = "invalid/jars/invalid-1.0.rar".equals( path.replace( '\\', '/' ) );
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
            String path = (String) i.next();

            found = "invalid/jars/invalid.jar".equals( path.replace( '\\', '/' ) );
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

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return factory.createArtifact( groupId, artifactId, version, null, "jar" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        return factory.createArtifact( groupId, artifactId, version, null, type );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type, String classifier )
    {
        return factory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

}
