package org.apache.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * AbstractRepositoryServletTestCase
 *
 * @version $Id$
 */
@RunWith( SpringJUnit4ClassRunner.class )
//ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
@ContextConfiguration( locations = { "classpath*:/repository-servlet-simple.xml" } )
public abstract class AbstractRepositoryServletTestCase
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected static final String REPOID_LEGACY = "legacy";

    protected ServletUnitClient sc;

    protected File repoRootInternal;

    protected File repoRootLegacy;

    private ServletRunner sr;

    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    protected ApplicationContext applicationContext;

    protected Logger log = LoggerFactory.getLogger( getClass() );


    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }


    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        String appserverBase = new File( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );

        File testConf = new File( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        if ( testConfDest.exists() )
        {
            FileUtils.deleteQuietly( testConfDest );
        }
        FileUtils.copyFile( testConf, testConfDest );

        archivaConfiguration = applicationContext.getBean( ArchivaConfiguration.class );

        //archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        repoRootInternal = new File( appserverBase, "data/repositories/internal" );
        repoRootLegacy = new File( appserverBase, "data/repositories/legacy" );
        Configuration config = archivaConfiguration.getConfiguration();

        config.getManagedRepositories().clear();

        config.addManagedRepository(
            createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal, true ) );

        config.addManagedRepository(
            createManagedRepository( REPOID_LEGACY, "Legacy Format Test Repo", repoRootLegacy, "legacy", true ) );

        config.getProxyConnectors().clear();

        config.getRemoteRepositories().clear();

        saveConfiguration( archivaConfiguration );

        CacheManager.getInstance().clearAll();

        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        sr = new ServletRunner( new File( "src/test/resources/WEB-INF/web.xml" ) );

        sr.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );
        sc = sr.newClient();
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {
        if ( sc != null )
        {
            sc.clearContents();
        }

        if ( sr != null )
        {
            sr.shutDown();
        }

        if ( repoRootInternal.exists() )
        {
            FileUtils.deleteDirectory( repoRootInternal );
        }

        if ( repoRootLegacy.exists() )
        {
            FileUtils.deleteDirectory( repoRootLegacy );
        }

        super.tearDown();
    }


    protected void assertFileContents( String expectedContents, File repoRoot, String path )
        throws IOException
    {
        File actualFile = new File( repoRoot, path );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should exist.", actualFile.exists() );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should be a file (not a dir/link/device/etc).",
                    actualFile.isFile() );

        String actualContents = FileUtils.readFileToString( actualFile, null );
        assertEquals( "File Contents of <" + actualFile.getAbsolutePath() + ">", expectedContents, actualContents );
    }

    protected void assertRepositoryValid( RepositoryServlet servlet, String repoId )
        throws Exception
    {
        ManagedRepository repository = servlet.getRepository( repoId );
        assertNotNull( "Archiva Managed Repository id:<" + repoId + "> should exist.", repository );
        File repoRoot = new File( repository.getLocation() );
        assertTrue( "Archiva Managed Repository id:<" + repoId + "> should have a valid location on disk.",
                    repoRoot.exists() && repoRoot.isDirectory() );
    }

    protected void assertResponseOK( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an OK response code", HttpServletResponse.SC_OK,
                             response.getResponseCode() );
    }

    protected void assertResponseOK( WebResponse response, String path )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an OK response code for path: " + path, HttpServletResponse.SC_OK,
                             response.getResponseCode() );
    }

    protected void assertResponseNotFound( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 404/Not Found response code.", HttpServletResponse.SC_NOT_FOUND,
                             response.getResponseCode() );
    }

    protected void assertResponseInternalServerError( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 500/Internal Server Error response code.",
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getResponseCode() );
    }

    protected void assertResponseConflictError( WebResponse response )
    {
        assertNotNull( "Should have received a response", response );
        Assert.assertEquals( "Should have been a 409/Conflict response code.", HttpServletResponse.SC_CONFLICT,
                             response.getResponseCode() );
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location,
                                                                      boolean blockRedeployments )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        repo.setBlockRedeployments( blockRedeployments );

        return repo;
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location,
                                                                      String layout, boolean blockRedeployments )
    {
        ManagedRepositoryConfiguration repo = createManagedRepository( id, name, location, blockRedeployments );
        repo.setLayout( layout );
        return repo;
    }

    protected RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String url )
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        return repo;
    }

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
        throws Exception
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );
    }


    protected void setupCleanRepo( File repoRootDir )
        throws IOException
    {
        FileUtils.deleteDirectory( repoRootDir );
        if ( !repoRootDir.exists() )
        {
            repoRootDir.mkdirs();
        }
    }

    protected void assertManagedFileNotExists( File repoRootInternal, String resourcePath )
    {
        File repoFile = new File( repoRootInternal, resourcePath );
        assertFalse( "Managed Repository File <" + repoFile.getAbsolutePath() + "> should not exist.",
                     repoFile.exists() );
    }

    protected void setupCleanInternalRepo()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
    }

    protected File populateRepo( File repoRootManaged, String path, String contents )
        throws Exception
    {
        File destFile = new File( repoRootManaged, path );
        destFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( destFile, contents, null );
        return destFile;
    }
}
