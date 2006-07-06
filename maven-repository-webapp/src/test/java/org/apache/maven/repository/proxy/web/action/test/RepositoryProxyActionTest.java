package org.apache.maven.repository.proxy.web.action.test;

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

import org.apache.maven.repository.proxy.web.action.RepositoryProxyAction;
import org.apache.maven.repository.proxy.web.action.test.stub.ProxyManagerStub;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class RepositoryProxyActionTest
    extends PlexusTestCase
{

    /**
     * test basic proxy operation
     *
     * @throws Exception
     */
    public void testProxy()
        throws Exception
    {
        String testDir = getBasedir() + "/target/test-classes/unit/proxy-test";
        RepositoryProxyAction action = new RepositoryProxyAction();
        ProxyManagerStub proxyManager = new ProxyManagerStub( testDir );
        File cachedFile = proxyManager.get( "dummyFile" );

        if ( !cachedFile.getParentFile().exists() )
        {
            assertTrue( "can not create test file", cachedFile.getParentFile().mkdirs() );
        }

        if ( !cachedFile.exists() )
        {
            assertTrue( "can not create test file", cachedFile.createNewFile() );
        }

        File tmpDir = getTestFile( "target/tmp-repo" );
        tmpDir.mkdirs();

        // TODO: configure manually, test the property loader elsewhere
        Properties properties = new Properties();
        properties.load( getClass().getResourceAsStream( "/unit/proxy-test/maven-proxy-complete.conf" ) );
        properties.setProperty( "repo.local.store", tmpDir.getAbsolutePath() );
        File tempFile = File.createTempFile( "test", "tmp" );
        tempFile.deleteOnExit();
        properties.store( new FileOutputStream( tempFile ), "" );

        action.setConfigFile( tempFile.getAbsolutePath() );
        action.setProxyManager( proxyManager );

        String result = action.execute();
        FileInputStream fileStream = action.getArtifactStream();

        assertEquals( "proxy error", action.SUCCESS, result );
        assertNotNull( "inputstream not set", fileStream );
        assertNotNull( "cached file not set", action.getCachedFile() );
        assertTrue( "proxy error", cachedFile.getPath().equals( action.getCachedFile().getPath() ) );
    }
}
