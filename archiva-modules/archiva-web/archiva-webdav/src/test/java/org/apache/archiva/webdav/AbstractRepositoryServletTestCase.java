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

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.archiva.webdav.util.MavenIndexerCleaner;
import org.apache.archiva.webdav.util.ReinitServlet;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.ContextLoaderListener;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * AbstractRepositoryServletTestCase
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/repository-servlet-simple.xml" } )
public abstract class AbstractRepositoryServletTestCase
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected static final String REPOID_LEGACY = "legacy";

    protected File repoRootInternal;

    protected File repoRootLegacy;


    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    protected ApplicationContext applicationContext;

    protected Logger log = LoggerFactory.getLogger( getClass() );


    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }

    protected Tomcat tomcat;

    protected static int port;

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

        applicationContext.getBean( MavenIndexerCleaner.class ).cleanupIndex();



    }

    StandardContext context;

    UnauthenticatedRepositoryServlet servlet;

    protected void startRepository() throws Exception
    {
        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        context = (StandardContext) tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );

        ApplicationParameter applicationParameter = new ApplicationParameter();
        applicationParameter.setName( "contextConfigLocation" );
        applicationParameter.setValue( getSpringConfigLocation() );
        context.addApplicationParameter( applicationParameter );

        context.addApplicationListener( ContextLoaderListener.class.getName() );

        context.addApplicationListener( MavenIndexerCleaner.class.getName() );

        servlet = new UnauthenticatedRepositoryServlet();

        Tomcat.addServlet( context, "repository", servlet );
        context.addServletMapping( "/repository/*", "repository" );


        Tomcat.addServlet( context, "reinitservlet", new ReinitServlet() );
        context.addServletMapping( "/reinit/*", "reinitservlet" );

        tomcat.start();

        this.port = tomcat.getConnector().getLocalPort();
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:/META-INF/spring-context.xml,classpath*:spring-context.xml";
    }




    /*
    protected ServletUnitClient getServletUnitClient()
        throws Exception
    {
        if ( servletUnitClient != null )
        {
            return servletUnitClient;
        }
        servletRunner = new ServletRunner( new File( "src/test/resources/WEB-INF/web.xml" ) );

        servletRunner.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );

        servletUnitClient = servletRunner.newClient();

        return servletUnitClient;
    }*/

    /*
    protected <P extends Page> P page(final String path) throws IOException {
        return newClient().getPage(base.toExternalForm() + "repository/" + path);
    }
    */

    protected static WebClient newClient()
    {
        final WebClient webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled( false );
        webClient.getOptions().setCssEnabled( false );
        webClient.getOptions().setAppletEnabled( false );
        webClient.getOptions().setThrowExceptionOnFailingStatusCode( false );
        webClient.setAjaxController( new NicelyResynchronizingAjaxController() );
        return webClient;
    }


    protected static WebResponse getWebResponse( String path )
        throws Exception
    {
        WebClient client = newClient();
        client.getPage( "http://localhost:" + port + "/reinit/reload" );
        return client.getPage( "http://localhost:" + port + path ).getWebResponse();
    }

    public static class GetMethodWebRequest
        extends WebRequest
    {
        String url;

        public GetMethodWebRequest( String url )
            throws Exception
        {
            super( new URL( url ) );
            this.url = url;

        }
    }

    public static class PutMethodWebRequest
        extends WebRequest
    {
        String url;

        public PutMethodWebRequest( String url, InputStream inputStream, String contentType )
            throws Exception
        {
            super( new URL( url ), HttpMethod.PUT );
            this.url = url;

        }


    }

    public static class ServletUnitClient
    {

        public ServletUnitClient()
        {

        }

        public WebResponse getResponse( WebRequest request )
            throws Exception
        {
            return getWebResponse( request.getUrl().getPath() );
        }

        public WebResponse getResource( WebRequest request )
            throws Exception
        {
            return getResponse( request );
        }
    }

    public ServletUnitClient getServletUnitClient()
    {
        return new ServletUnitClient();
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {

        if ( repoRootInternal.exists() )
        {
            FileUtils.deleteDirectory( repoRootInternal );
        }

        if ( repoRootLegacy.exists() )
        {
            FileUtils.deleteDirectory( repoRootLegacy );
        }

        if ( this.tomcat != null )
        {
            this.tomcat.stop();
        }

    }


    protected void assertFileContents( String expectedContents, File repoRoot, String path )
        throws IOException
    {
        File actualFile = new File( repoRoot, path );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should exist.", actualFile.exists() );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should be a file (not a dir/link/device/etc).",
                    actualFile.isFile() );

        String actualContents = FileUtils.readFileToString( actualFile, Charset.defaultCharset() );
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
                             response.getStatusCode() );
    }

    protected void assertResponseOK( WebResponse response, String path )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an OK response code for path: " + path, HttpServletResponse.SC_OK,
                             response.getStatusCode() );
    }

    protected void assertResponseNotFound( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 404/Not Found response code.", HttpServletResponse.SC_NOT_FOUND,
                             response.getStatusCode() );
    }

    protected void assertResponseInternalServerError( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 500/Internal Server Error response code.",
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatusCode() );
    }

    protected void assertResponseConflictError( WebResponse response )
    {
        assertNotNull( "Should have received a response", response );
        Assert.assertEquals( "Should have been a 409/Conflict response code.", HttpServletResponse.SC_CONFLICT,
                             response.getStatusCode() );
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
        FileUtils.writeStringToFile( destFile, contents, Charset.defaultCharset() );
        return destFile;
    }
}
