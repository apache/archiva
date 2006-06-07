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

    private File repositoryLocation;

    /**
     *
     */
    public void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (MetadataDiscoverer) lookup( MetadataDiscoverer.ROLE,
                                                  "default" );
        repositoryLocation = getTestFile( "src/test/repository" );
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
        List metadataPaths = discoverer.discoverMetadata( repositoryLocation, null );
        assertNotNull( "Check metadata not null", metadataPaths );
        assertEquals( 3, metadataPaths.size() );
    }

    /**
     * Test if metadata file in wrong directory was added to the kickedOutPaths.
     */
    public void testKickoutWrongDirectory()
    {
        discoverer.discoverMetadata( repositoryLocation, null );
        Iterator iter = discoverer.getKickedOutPathsIterator();
        boolean found = false;
        while ( iter.hasNext() && !found )
        {
            String dir = (String) iter.next();
            String normalizedDir = dir.replace( '\\', '/' );
            if ( "javax/maven-metadata-repository.xml".equals( normalizedDir ) )
            {
                found = true;
            }
        }
        assertTrue( found );
    }

    /**
     * Test if blank metadata file was added to the kickedOutPaths.
     */
    public void testKickoutBlankMetadata()
    {
        discoverer.discoverMetadata( repositoryLocation, null );
        Iterator iter = discoverer.getKickedOutPathsIterator();
        boolean found = false;
        while ( iter.hasNext() && !found )
        {
            String dir = (String) iter.next();
            String normalizedDir = dir.replace( '\\', '/' );
            if ( "org/apache/maven/some-ejb/1.0/maven-metadata-repository.xml".equals( normalizedDir ) )
            {
                found = true;
            }
        }
        assertTrue( found );
    }

}
