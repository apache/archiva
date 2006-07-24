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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * This class tests the DefaultMetadataDiscoverer class.
 */
public class DefaultMetadataDiscovererTest
    extends PlexusTestCase
{
    private MetadataDiscoverer discoverer;

    private static final String TEST_OPERATION = "test";

    private ArtifactRepository repository;

    /**
     *
     */
    public void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (MetadataDiscoverer) lookup( MetadataDiscoverer.ROLE, "default" );

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
     * Test DefaultMetadataDiscoverer when the all metadata paths are valid.
     */
    public void testMetadataDiscovererSuccess()
    {
        List metadataPaths = discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        assertNotNull( "Check metadata not null", metadataPaths );
        assertEquals( 3, metadataPaths.size() );
    }

    /**
     * Test if metadata file in wrong directory was added to the kickedOutPaths.
     */
    public void testKickoutWrongDirectory()
    {
        discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        Iterator iter = discoverer.getKickedOutPathsIterator();
        boolean found = false;
        while ( iter.hasNext() && !found )
        {
            DiscovererPath dPath = (DiscovererPath) iter.next();
            String dir = dPath.getPath();

            String normalizedDir = dir.replace( '\\', '/' );
            if ( "javax/maven-metadata-repository.xml".equals( normalizedDir ) )
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
    {
        discoverer.discoverMetadata( repository, TEST_OPERATION, null );
        Iterator iter = discoverer.getKickedOutPathsIterator();
        boolean found = false;
        while ( iter.hasNext() && !found )
        {
            DiscovererPath dPath = (DiscovererPath) iter.next();
            String dir = dPath.getPath();

            String normalizedDir = dir.replace( '\\', '/' );
            if ( "org/apache/maven/some-ejb/1.0/maven-metadata-repository.xml".equals( normalizedDir ) )
            {
                found = true;
                assertTrue( "Check reason for kickout", dPath.getComment().matches(
                    "Error reading metadata file '(.*)': input contained no data" ) );
            }
        }
        assertTrue( found );
    }

    private void removeTimestampMetadata()
    {
        // remove the metadata that tracks time
        File file = new File( repository.getBasedir(), "maven-metadata.xml" );
        file.delete();
        assertFalse( file.exists() );
    }
}
