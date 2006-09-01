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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * This class tests the DefaultMetadataDiscoverer class.
 */
public class DefaultMetadataDiscovererTest
    extends PlexusTestCase
{
    private MetadataDiscoverer discoverer;

    private static final String TEST_OPERATION = "test";

    private ArtifactRepository repository;

    private ArtifactFactory factory;

    /**
     *
     */
    public void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (MetadataDiscoverer) lookup( MetadataDiscoverer.ROLE, "default" );

        factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        repository = getRepository();

        removeTimestampMetadata();
    }

    protected ArtifactRepository getRepository()
        throws Exception
    {
        File basedir = getTestFile( "src/test/repository" );

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        return factory.createArtifactRepository( "discoveryRepo", "file://" + basedir, layout, null, null );
    }

    /**
     *
     */
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        discoverer = null;
    }

    /**
     * Test if metadata file in wrong directory was added to the kickedOutPaths.
     */
    public void testKickoutWrongDirectory()
        throws DiscovererException
    {
        discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        Iterator iter = discoverer.getKickedOutPathsIterator();
        boolean found = false;
        while ( iter.hasNext() && !found )
        {
            DiscovererPath dPath = (DiscovererPath) iter.next();
            String dir = dPath.getPath();

            String normalizedDir = dir.replace( '\\', '/' );
            if ( "javax/maven-metadata.xml".equals( normalizedDir ) )
            {
                found = true;
                assertEquals( "Check reason for kickout", "Unable to build a repository metadata from path",
                              dPath.getComment() );
            }
        }
        assertTrue( found );
    }

    /**
     * Test if blank metadata file was added to the kickedOutPaths.
     */
    public void testKickoutBlankMetadata()
        throws DiscovererException
    {
        discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        Iterator iter = discoverer.getKickedOutPathsIterator();
        boolean found = false;
        while ( iter.hasNext() && !found )
        {
            DiscovererPath dPath = (DiscovererPath) iter.next();
            String dir = dPath.getPath();

            String normalizedDir = dir.replace( '\\', '/' );
            if ( "org/apache/maven/some-ejb/1.0/maven-metadata.xml".equals( normalizedDir ) )
            {
                found = true;
                assertTrue( "Check reason for kickout", dPath.getComment().matches(
                    "Error reading metadata file '(.*)': input contained no data" ) );
            }
        }
        assertTrue( found );
    }

    private void removeTimestampMetadata()
        throws IOException
    {
        // remove the metadata that tracks time
        File file = new File( repository.getBasedir(), "maven-metadata.xml" );
        System.gc(); // for Windows
        file.delete();
        assertFalse( file.exists() );
    }

    public void testDiscoverMetadata()
        throws DiscovererException
    {
        List metadataPaths = discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.testgroup", "discovery" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );

        metadata =
            new SnapshotArtifactRepositoryMetadata( createArtifact( "org.apache.testgroup", "discovery", "1.0" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );

        metadata = new GroupRepositoryMetadata( "org.apache.maven" );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );
    }

    public void testUpdatedInRepository()
        throws ComponentLookupException, DiscovererException, ParseException, IOException
    {
        // Set repository time to 1-1-2000, a time in the distant past so definitely updated
        discoverer.setLastCheckedTime( repository, "update",
                                       new SimpleDateFormat( "yyyy-MM-dd", Locale.US ).parse( "2000-01-01" ) );

        List metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.maven.update", "test-updated" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );

        // try again with the updated timestamp
        metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        assertFalse( "Check not included", containsMetadata( metadataPaths, metadata ) );
    }

    private boolean containsMetadata( List metadataPaths, RepositoryMetadata metadata )
    {
        for ( Iterator i = metadataPaths.iterator(); i.hasNext(); )
        {
            RepositoryMetadata m = (RepositoryMetadata) i.next();

            if ( m.getGroupId().equals( metadata.getGroupId() ) )
            {
                if ( m.getArtifactId() == null && metadata.getArtifactId() == null )
                {
                    return true;
                }
                else if ( m.getArtifactId() != null && m.getArtifactId().equals( metadata.getArtifactId() ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void testNotUpdatedInRepository()
        throws ComponentLookupException, DiscovererException, IOException
    {
        // Set repository time to now, which is after any artifacts, so definitely not updated
        discoverer.setLastCheckedTime( repository, "update", new Date() );

        List metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.maven.update", "test-not-updated" ) );
        assertFalse( "Check not included", containsMetadata( metadataPaths, metadata ) );
    }

    public void testNotUpdatedInRepositoryForcedDiscovery()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.resetLastCheckedTime( repository, "update" );

        List metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.maven.update", "test-not-updated" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );

        // try again with the updated timestamp
        metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        assertFalse( "Check not included", containsMetadata( metadataPaths, metadata ) );
    }

    public void testNotUpdatedInRepositoryForcedDiscoveryMetadataAlreadyExists()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.setLastCheckedTime( repository, "update", new Date() );

        discoverer.resetLastCheckedTime( repository, "update" );

        List metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.maven.update", "test-not-updated" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );

        // try again with the updated timestamp
        metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        assertFalse( "Check not included", containsMetadata( metadataPaths, metadata ) );
    }

    public void testNotUpdatedInRepositoryForcedDiscoveryOtherMetadataAlreadyExists()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.setLastCheckedTime( repository, "test", new Date() );

        discoverer.resetLastCheckedTime( repository, "update" );

        List metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.maven.update", "test-not-updated" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );

        // try again with the updated timestamp
        metadataPaths = discoverer.discoverMetadata( repository, "update", null );
        assertNotNull( "Check metadata not null", metadataPaths );

        assertFalse( "Check not included", containsMetadata( metadataPaths, metadata ) );
    }

    public void testNoRepositoryMetadata()
        throws ComponentLookupException, DiscovererException, ParseException, IOException
    {
        removeTimestampMetadata();

        // should find all
        List metadataPaths = discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        assertNotNull( "Check metadata not null", metadataPaths );

        RepositoryMetadata metadata =
            new ArtifactRepositoryMetadata( createArtifact( "org.apache.maven.update", "test-updated" ) );
        assertTrue( "Check included", containsMetadata( metadataPaths, metadata ) );
    }

    protected Artifact createArtifact( String groupId, String artifactId )
    {
        return createArtifact( groupId, artifactId, "1.0" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return factory.createArtifact( groupId, artifactId, version, null, "jar" );
    }
}
