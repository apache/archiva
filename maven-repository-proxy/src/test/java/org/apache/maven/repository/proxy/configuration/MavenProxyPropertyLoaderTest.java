package org.apache.maven.repository.proxy.configuration;

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

import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;


/**
 * @author Edwin Punzalan
 */
public class MavenProxyPropertyLoaderTest
    extends PlexusTestCase
{
    public void testLoadValidMavenProxyConfiguration()
        throws ValidationException, IOException
    {
        MavenProxyPropertyLoader loader = new MavenProxyPropertyLoader();

        //must create the test directory bec configuration is using relative path which varies
        FileUtils.mkdir( "target/remote-repo1" );

        try
        {
            File confFile = getTestFile( "src/test/conf/maven-proxy-complete.conf" );

            ProxyConfiguration config = loader.load( new FileInputStream( confFile ) );

            assertTrue( "cache path changed", config.getRepositoryCachePath().endsWith( "target" ) );

            assertEquals( "Count repositories", 4, config.getRepositories().size() );

            int idx = 0;
            for ( Iterator repos = config.getRepositories().iterator(); repos.hasNext(); )
            {
                idx++;

                ProxyRepository repo = (ProxyRepository) repos.next();

                //switch is made to check for ordering
                switch ( idx )
                {
                    case 1:
                        assertEquals( "Repository name not as expected", "local-repo", repo.getKey() );
                        assertEquals( "Repository url does not match its name", "file://target", repo.getUrl() );
                        assertEquals( "Repository cache period check failed", 0, repo.getCachePeriod() );
                        assertFalse( "Repository failure caching check failed", repo.isCacheFailures() );
                        break;
                    case 2:
                        assertEquals( "Repository name not as expected", "www-ibiblio-org", repo.getKey() );
                        assertEquals( "Repository url does not match its name", "http://www.ibiblio.org/maven2",
                                      repo.getUrl() );
                        assertEquals( "Repository cache period check failed", 3600, repo.getCachePeriod() );
                        assertTrue( "Repository failure caching check failed", repo.isCacheFailures() );
                        break;
                    case 3:
                        assertEquals( "Repository name not as expected", "dist-codehaus-org", repo.getKey() );
                        assertEquals( "Repository url does not match its name", "http://dist.codehaus.org",
                                      repo.getUrl() );
                        assertEquals( "Repository cache period check failed", 3600, repo.getCachePeriod() );
                        assertTrue( "Repository failure caching check failed", repo.isCacheFailures() );
                        break;
                    case 4:
                        assertEquals( "Repository name not as expected", "private-example-com", repo.getKey() );
                        assertEquals( "Repository url does not match its name", "http://private.example.com/internal",
                                      repo.getUrl() );
                        assertEquals( "Repository cache period check failed", 3600, repo.getCachePeriod() );
                        assertFalse( "Repository failure caching check failed", repo.isCacheFailures() );
                        break;
                    default:
                        fail( "Unexpected order count" );
                }
            }
        }
        //make sure to delete the test directory after tests
        finally
        {
            FileUtils.deleteDirectory( "target/remote-repo1" );
        }
    }

}
