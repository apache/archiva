package org.apache.maven.archiva.web.servlet.repository;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.archiva.web.servlet.PlexusServlet;
import org.codehaus.plexus.PlexusTestCase;

/**
 * RepositoryAccessTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryAccessTest
    extends PlexusTestCase
{
    private void assertRequestPath( String expectedId, String expectedPath, String rawpath )
        throws Exception
    {
        RepositoryAccess repoaccess = (RepositoryAccess) lookup( PlexusServlet.ROLE, "repositoryAccess" );

        RepositoryAccess.RequestPath requestPath = repoaccess.getRepositoryPath( rawpath );

        if ( expectedId == null )
        {
            // special case, should be null.
            assertNull( requestPath );
            return;
        }

        assertNotNull( requestPath );

        assertEquals( expectedId, requestPath.repoName );
        assertEquals( expectedPath, requestPath.path );
    }

    public void testGetRepoPath() throws Exception
    {
        // Test for paths with no id.
        assertRequestPath( null, null, null );
        assertRequestPath( null, null, "" );
        assertRequestPath( null, null, "/" );
        
        // Test for paths with root browse
        assertRequestPath( "central", "/", "/central" );
        assertRequestPath( "central", "/", "/central/" );
        assertRequestPath( "snapshots", "/", "/snapshots/" );
        
        // Test for paths deep within repository.
        assertRequestPath( "central", "/org/apache/maven/", "/central/org/apache/maven/" );
        assertRequestPath( "snapshots", "/org/codehaus/mojo", "/snapshots/org/codehaus/mojo" );
        
        assertRequestPath( "central", "/org/apache/maven/archiva/metadata.xml", 
                              "/central/org/apache/maven/archiva/metadata.xml" );
        assertRequestPath( "sandbox", "/org/company/experiment/1.0/experiment-1.0.jar.pom", 
                              "/sandbox/org/company/experiment/1.0/experiment-1.0.jar.pom" );

        // Test for paths with "/../" nastyness
        assertRequestPath( "central", "/", "/central/.." );
        assertRequestPath( "central", "/", "/central/../../../" );
        assertRequestPath( "central", "/", "/central/org/../../etc/passwd" );
        assertRequestPath( "central", "/etc/passwd", "/central//etc/passwd" );
        assertRequestPath( "central", "/org/codehaus/mojo", "/central/org/apache/../codehaus/mojo" );
    }
}
