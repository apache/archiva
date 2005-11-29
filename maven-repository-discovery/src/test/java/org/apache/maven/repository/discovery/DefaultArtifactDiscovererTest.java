package org.apache.maven.repository.discovery;

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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Test the default artifact discoverer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultArtifactDiscovererTest
    extends PlexusTestCase
{
    private ArtifactDiscoverer discoverer;

    private ArtifactFactory factory;

    private File repositoryLocation;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (ArtifactDiscoverer) lookup( ArtifactDiscoverer.ROLE, "default" );

        factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        repositoryLocation = getTestFile( "src/test/repository" );
    }

    public void testDefaultExcludes()
    {
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, null, false );
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
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, null, false );
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            String path = (String) i.next();

            found = path.equals( "KEYS" );
        }
        assertTrue( "Check exclusion was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not KEYS", a.getFile().getName().equals( "KEYS" ) );
        }
    }

    public void testBlacklistedExclude()
    {
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, "javax/**", false );
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext() && !found; )
        {
            String path = (String) i.next();

            found = path.replace( '\\', '/' ).equals( "javax/sql/jdbc/2.0/jdbc-2.0.jar" );
        }
        assertTrue( "Check exclusion was found", found );

        assertFalse( "Check jdbc not included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
    }

    public void testKickoutWithShortPath()
    {
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, null, false );
        assertNotNull( "Check artifacts not null", artifacts );
        boolean found = false;
        for ( Iterator i = discoverer.getKickedOutPathsIterator(); i.hasNext() && !found; )
        {
            String path = (String) i.next();

            found = path.replace( '\\', '/' ).equals( "invalid/invalid-1.0.jar" );
        }
        assertTrue( "Check exclusion was found", found );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            assertFalse( "Check not invalid-1.0.jar", a.getFile().getName().equals( "invalid-1.0.jar" ) );
        }
    }

    public void testSnapshotInclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertTrue( "Check snapshot included",
                    artifacts.contains( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ) ) );
    }

    public void testSnapshotExclusion()
    {
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, null, false );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check normal included", artifacts.contains( createArtifact( "javax.sql", "jdbc", "2.0" ) ) );
        assertFalse( "Check snapshot included",
                     artifacts.contains( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ) ) );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return factory.createArtifact( groupId, artifactId, version, null, "jar" );
    }

}
