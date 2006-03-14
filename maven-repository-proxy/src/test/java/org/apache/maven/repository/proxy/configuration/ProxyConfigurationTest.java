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

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProxyConfigurationTest
    extends PlexusTestCase
{
    private ProxyConfiguration config;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        config = (ProxyConfiguration) container.lookup( ProxyConfiguration.ROLE );
    }

    public void testRepositoryCache()
    {
        File cacheFile = new File( "target/proxy-cache" );
        config.setRepositoryCachePath( cacheFile.getAbsolutePath() );
        assertEquals( config.getRepositoryCachePath(), cacheFile.getAbsolutePath() );
    }

    public void testRepositories()
    {
        ArtifactRepositoryLayout defLayout = new DefaultRepositoryLayout();
        ProxyRepository repo1 = new ProxyRepository( "repo1", "http://www.ibiblio.org/maven2", defLayout );
        repo1.setCacheFailures( true );
        repo1.setCachePeriod( 0 );
        repo1.setHardfail( true );
        config.addRepository( repo1 );
        assertEquals( 1, config.getRepositories().size() );

        ArtifactRepositoryLayout legacyLayout = new LegacyRepositoryLayout();
        ProxyRepository repo2 = new ProxyRepository( "repo2", "http://www.ibiblio.org/maven", legacyLayout );
        repo2.setCacheFailures( false );
        repo2.setCachePeriod( 3600 );
        repo2.setProxy( "some.local.proxy", 80, "username", "password" );
        config.addRepository( repo2 );
        assertEquals( 2, config.getRepositories().size() );

        List repositories = config.getRepositories();
        ProxyRepository repo = (ProxyRepository) repositories.get( 0 );
        assertEquals( "repo1", repo.getId() );
        assertEquals( "http://www.ibiblio.org/maven2", repo.getUrl() );
        assertTrue( repo.isCacheFailures() );
        assertTrue( repo.isHardfail() );
        assertEquals( 0, repo.getCachePeriod() );
        assertEquals( repo1, repo );

        repo = (ProxyRepository) repositories.get( 1 );
        assertEquals( "repo2", repo.getId() );
        assertEquals( "http://www.ibiblio.org/maven", repo.getUrl() );
        assertFalse( repo.isCacheFailures() );
        assertFalse( repo.isHardfail() );
        assertEquals( 3600, repo.getCachePeriod() );
        assertEquals( repo2, repo );
        assertTrue( repo.isProxied() );

        ProxyInfo proxyInfo = repo.getProxy();
        assertNotNull( proxyInfo );
        assertEquals( "some.local.proxy", proxyInfo.getHost() );
        assertEquals( 80, proxyInfo.getPort() );
        assertEquals( "username", proxyInfo.getUserName() );
        assertEquals( "password", proxyInfo.getPassword() );

        try
        {
            repositories.add( new ProxyRepository( "repo", "url", defLayout ) );
            fail( "Expected UnsupportedOperationException not thrown." );
        }
        catch ( java.lang.UnsupportedOperationException e )
        {
            assertTrue( true );
        }

        repositories = new ArrayList();
        repositories.add( repo1 );
        repositories.add( repo2 );
        config.setRepositories( repositories );
        assertEquals( repositories, config.getRepositories() );
    }

//    public void testLoadValidMavenProxyConfiguration()
//        throws ValidationException, IOException
//    {
//        //must create the test directory bec configuration is using relative path which varies
//        FileUtils.mkdir( "target/remote-repo1" );
//
//        try
//        {
//            File confFile = getTestFile( "src/test/conf/maven-proxy-complete.conf" );
//
//            config.loadMavenProxyConfiguration( confFile );
//
//            assertTrue( "cache path changed", config.getRepositoryCachePath().endsWith( "target" ) );
//
//            assertEquals( "Count repositories", 4, config.getRepositories().size() );
//
//            int idx = 0;
//            for ( Iterator repos = config.getRepositories().iterator(); repos.hasNext(); )
//            {
//                idx++;
//
//                ProxyRepository repo = (ProxyRepository) repos.next();
//
//                //switch is made to check for ordering
//                switch ( idx )
//                {
//                    case 1:
//                        assertEquals( "Repository name not as expected", "local-repo", repo.getKey() );
//                        assertEquals( "Repository url does not match its name", "file:///./target/remote-repo1",
//                                      repo.getUrl() );
//                        assertEquals( "Repository cache period check failed", 0, repo.getCachePeriod() );
//                        assertFalse( "Repository failure caching check failed", repo.isCacheFailures() );
//                        break;
//                    case 2:
//                        assertEquals( "Repository name not as expected", "www-ibiblio-org", repo.getKey() );
//                        assertEquals( "Repository url does not match its name", "http://www.ibiblio.org/maven2",
//                                      repo.getUrl() );
//                        assertEquals( "Repository cache period check failed", 3600, repo.getCachePeriod() );
//                        assertTrue( "Repository failure caching check failed", repo.isCacheFailures() );
//                        break;
//                    case 3:
//                        assertEquals( "Repository name not as expected", "dist-codehaus-org", repo.getKey() );
//                        assertEquals( "Repository url does not match its name", "http://dist.codehaus.org",
//                                      repo.getUrl() );
//                        assertEquals( "Repository cache period check failed", 3600, repo.getCachePeriod() );
//                        assertTrue( "Repository failure caching check failed", repo.isCacheFailures() );
//                        break;
//                    case 4:
//                        assertEquals( "Repository name not as expected", "private-example-com", repo.getKey() );
//                        assertEquals( "Repository url does not match its name", "http://private.example.com/internal",
//                                      repo.getUrl() );
//                        assertEquals( "Repository cache period check failed", 3600, repo.getCachePeriod() );
//                        assertFalse( "Repository failure caching check failed", repo.isCacheFailures() );
//                        break;
//                    default:
//                        fail( "Unexpected order count" );
//                }
//            }
//        }
//        //make sure to delete the test directory after tests
//        finally
//        {
//            FileUtils.deleteDirectory( "target/remote-repo1" );
//        }
//    }
//
    protected void tearDown()
        throws Exception
    {
        config = null;

        super.tearDown();
    }
}