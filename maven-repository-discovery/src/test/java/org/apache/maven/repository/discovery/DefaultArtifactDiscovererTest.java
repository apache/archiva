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

    private File repositoryLocation;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (ArtifactDiscoverer) lookup( ArtifactDiscoverer.ROLE, "default" );

        repositoryLocation = getTestFile( "src/test/repository" );
    }

    public void testDefaultExcludes()
    {
        List artifacts = discoverer.discoverArtifacts( repositoryLocation, null, false );
        assertNotNull( "Check artifacts returned", artifacts );
        assertTrue( "Check no artifacts returned", artifacts.isEmpty() );
        boolean found = false;
        for ( Iterator i = discoverer.getExcludedPathsIterator(); i.hasNext(); )
        {
            String path = (String) i.next();

            if ( !path.startsWith( ".svn" ) )
            {
                assertEquals( "Check the excluded path", "KEYS", path );
                if ( found )
                {
                    fail( "KEYS entry found twice" );
                }
                found = true;
            }
        }
        assertTrue( "Check exclusion was found", found );
    }
}
