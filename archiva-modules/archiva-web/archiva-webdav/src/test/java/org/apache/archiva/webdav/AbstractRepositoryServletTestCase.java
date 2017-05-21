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
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.archiva.webdav.httpunit.MkColMethodWebRequest;
import org.apache.archiva.webdav.util.MavenIndexerCleaner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * AbstractRepositoryServletTestCase
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:spring-context.xml",
    "classpath*:/repository-servlet-simple.xml" } )
public abstract class AbstractRepositoryServletTestCase
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected File repoRootInternal;

    protected File repoRootLegacy;

    @Inject
    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    protected ManagedRepositoryAdmin managedRepositoryAdmin;

    protected Logger log = LoggerFactory.getLogger( getClass() );


    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }

    @Before
    @Override
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

        repoRootInternal = new File( appserverBase, "data/repositories/internal" );
        repoRootLegacy = new File( appserverBase, "data/repositories/legacy" );
        Configuration config = archivaConfiguration.getConfiguration();

        config.getManagedRepositories().clear();

        config.addManagedRepository(
            createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal, true ) );

        managedRepositoryAdmin.createIndexContext( managedRepositoryAdmin.getManagedRepository( REPOID_INTERNAL ) );

        config.getProxyConnectors().clear();

        config.getRemoteRepositories().clear();

        saveConfiguration( archivaConfiguration );

        CacheManager.getInstance().clearAll();

        applicationContext.getBean( MavenIndexerCleaner.class ).cleanupIndex();


    }

    protected UnauthenticatedRepositoryServlet unauthenticatedRepositoryServlet =
        new UnauthenticatedRepositoryServlet();

    protected void startRepository()
        throws Exception
    {

        final MockServletContext mockServletContext = new MockServletContext();

        WebApplicationContext webApplicationContext =
            new TestWebapplicationContext( applicationContext, mockServletContext );

        mockServletContext.setAttribute( WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                                         webApplicationContext );

        MockServletConfig mockServletConfig = new MockServletConfig()
        {
            @Override
            public ServletContext getServletContext()
            {
                return mockServletContext;
            }
        };

        unauthenticatedRepositoryServlet.init( mockServletConfig );

    }

    protected String createVersionMetadata(String groupId, String artifactId, String version) {
        return createVersionMetadata(groupId, artifactId, version, null, null, null);
    }

    protected String createVersionMetadata(String groupId, String artifactId, String version, String timestamp, String buildNumber, String lastUpdated) {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        buf.append("<metadata>\n");
        buf.append("  <groupId>").append(groupId).append("</groupId>\n");
        buf.append("  <artifactId>").append(artifactId).append("</artifactId>\n");
        buf.append("  <version>").append(version).append("</version>\n");
        boolean hasSnapshot = StringUtils.isNotBlank(timestamp) || StringUtils.isNotBlank(buildNumber);
        boolean hasLastUpdated = StringUtils.isNotBlank(lastUpdated);
        if (hasSnapshot || hasLastUpdated) {
            buf.append("  <versioning>\n");
            if (hasSnapshot) {
                buf.append("    <snapshot>\n");
                buf.append("      <buildNumber>").append(buildNumber).append("</buildNumber>\n");
                buf.append("      <timestamp>").append(timestamp).append("</timestamp>\n");
                buf.append("    </snapshot>\n");
            }
            if (hasLastUpdated) {
                buf.append("    <lastUpdated>").append(lastUpdated).append("</lastUpdated>\n");
            }
            buf.append("  </versioning>\n");
        }
        buf.append("</metadata>");
        return buf.toString();
    }


    public static class TestWebapplicationContext
        implements WebApplicationContext
    {
        private ApplicationContext applicationContext;

        private ServletContext servletContext;

        TestWebapplicationContext( ApplicationContext applicationContext, ServletContext servletContext )
        {
            this.applicationContext = applicationContext;
        }

        @Override
        public ServletContext getServletContext()
        {
            return servletContext;
        }

        @Override
        public String getId()
        {
            return applicationContext.getId();
        }

        @Override
        public String getApplicationName()
        {
            return applicationContext.getApplicationName();
        }

        @Override
        public String getDisplayName()
        {
            return applicationContext.getDisplayName();
        }

        @Override
        public long getStartupDate()
        {
            return applicationContext.getStartupDate();
        }

        @Override
        public ApplicationContext getParent()
        {
            return applicationContext.getParent();
        }

        @Override
        public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
            throws IllegalStateException
        {
            return applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        public void publishEvent( ApplicationEvent applicationEvent )
        {
            applicationContext.publishEvent( applicationEvent );
        }

        @Override
        public Environment getEnvironment()
        {
            return applicationContext.getEnvironment();
        }

        @Override
        public BeanFactory getParentBeanFactory()
        {
            return applicationContext.getParentBeanFactory();
        }

        @Override
        public boolean containsLocalBean( String s )
        {
            return applicationContext.containsLocalBean( s );
        }

        @Override
        public boolean containsBeanDefinition( String s )
        {
            return applicationContext.containsBeanDefinition( s );
        }

        @Override
        public int getBeanDefinitionCount()
        {
            return applicationContext.getBeanDefinitionCount();
        }

        @Override
        public String[] getBeanDefinitionNames()
        {
            return applicationContext.getBeanDefinitionNames();
        }

        @Override
        public String[] getBeanNamesForType( Class<?> aClass )
        {
            return applicationContext.getBeanNamesForType( aClass );
        }

        @Override
        public String[] getBeanNamesForType( Class<?> aClass, boolean b, boolean b2 )
        {
            return applicationContext.getBeanNamesForType( aClass, b, b2 );
        }

        @Override
        public <T> Map<String, T> getBeansOfType( Class<T> tClass )
            throws BeansException
        {
            return applicationContext.getBeansOfType( tClass );
        }

        @Override
        public <T> Map<String, T> getBeansOfType( Class<T> tClass, boolean b, boolean b2 )
            throws BeansException
        {
            return applicationContext.getBeansOfType( tClass, b, b2 );
        }

        @Override
        public String[] getBeanNamesForAnnotation( Class<? extends Annotation> aClass )
        {
            return applicationContext.getBeanNamesForAnnotation( aClass );
        }

        @Override
        public Map<String, Object> getBeansWithAnnotation( Class<? extends Annotation> aClass )
            throws BeansException
        {
            return applicationContext.getBeansWithAnnotation( aClass );
        }

        @Override
        public <A extends Annotation> A findAnnotationOnBean( String s, Class<A> aClass )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.findAnnotationOnBean( s, aClass );
        }

        @Override
        public <T> T getBean( Class<T> aClass, Object... objects )
            throws BeansException
        {
            return applicationContext.getBean( aClass, objects );
        }

        @Override
        public Object getBean( String s )
            throws BeansException
        {
            return applicationContext.getBean( s );
        }

        @Override
        public <T> T getBean( String s, Class<T> tClass )
            throws BeansException
        {
            return applicationContext.getBean( s, tClass );
        }

        @Override
        public <T> T getBean( Class<T> tClass )
            throws BeansException
        {
            return applicationContext.getBean( tClass );
        }

        @Override
        public Object getBean( String s, Object... objects )
            throws BeansException
        {
            return applicationContext.getBean( s, objects );
        }

        @Override
        public boolean containsBean( String s )
        {
            return applicationContext.containsBean( s );
        }

        @Override
        public boolean isSingleton( String s )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.isSingleton( s );
        }

        @Override
        public boolean isPrototype( String s )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.isPrototype( s );
        }

        @Override
        public boolean isTypeMatch( String s, Class<?> aClass )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.isTypeMatch( s, aClass );
        }

        @Override
        public Class<?> getType( String s )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.getType( s );
        }

        @Override
        public String[] getAliases( String s )
        {
            return applicationContext.getAliases( s );
        }

        @Override
        public String getMessage( String s, Object[] objects, String s2, Locale locale )
        {
            return applicationContext.getMessage( s, objects, s2, locale );
        }

        @Override
        public String getMessage( String s, Object[] objects, Locale locale )
            throws NoSuchMessageException
        {
            return applicationContext.getMessage( s, objects, locale );
        }

        @Override
        public String getMessage( MessageSourceResolvable messageSourceResolvable, Locale locale )
            throws NoSuchMessageException
        {
            return applicationContext.getMessage( messageSourceResolvable, locale );
        }

        @Override
        public Resource[] getResources( String s )
            throws IOException
        {
            return applicationContext.getResources( s );
        }

        @Override
        public void publishEvent( Object o )
        {
            // no op
        }

        @Override
        public String[] getBeanNamesForType( ResolvableType resolvableType )
        {
            return new String[0];
        }

        @Override
        public boolean isTypeMatch( String s, ResolvableType resolvableType )
            throws NoSuchBeanDefinitionException
        {
            return false;
        }

        @Override
        public Resource getResource( String s )
        {
            return applicationContext.getResource( s );
        }

        @Override
        public ClassLoader getClassLoader()
        {
            return applicationContext.getClassLoader();
        }
    }

    protected Servlet findServlet( String name )
        throws Exception
    {
        return unauthenticatedRepositoryServlet;

    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:/META-INF/spring-context.xml,classpath*:spring-context.xml";
    }


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


    protected WebResponse getWebResponse( String path )
        throws Exception
    {
        return getWebResponse( new GetMethodWebRequest( "http://localhost" + path ) );//, false );
    }

    protected WebResponse getWebResponse( WebRequest webRequest ) //, boolean followRedirect )
        throws Exception
    {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( webRequest.getUrl().getPath() );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );

        request.setMethod( webRequest.getHttpMethod().name() );

        if ( webRequest.getHttpMethod() == HttpMethod.PUT )
        {
            PutMethodWebRequest putRequest = PutMethodWebRequest.class.cast( webRequest );
            request.setContentType( putRequest.contentType );
            request.setContent( IOUtils.toByteArray( putRequest.inputStream ) );
        }

        if ( webRequest instanceof MkColMethodWebRequest )
        {
            request.setMethod( "MKCOL" );
        }

        final MockHttpServletResponse response = execute( request );

        if ( response.getStatus() == HttpServletResponse.SC_MOVED_PERMANENTLY
            || response.getStatus() == HttpServletResponse.SC_MOVED_TEMPORARILY )
        {
            String location = response.getHeader( "Location" );
            log.debug( "follow redirect to {}", location );
            return getWebResponse( new GetMethodWebRequest( location ) );
        }

        return new WebResponse( null, null, 1 )
        {
            @Override
            public String getContentAsString()
            {
                try
                {
                    return response.getContentAsString();
                }
                catch ( UnsupportedEncodingException e )
                {
                    throw new RuntimeException( e.getMessage(), e );
                }
            }

            @Override
            public int getStatusCode()
            {
                return response.getStatus();
            }

            @Override
            public String getResponseHeaderValue( String headerName )
            {
                return response.getHeader( headerName );
            }
        };
    }

    protected MockHttpServletResponse execute( HttpServletRequest request )
        throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse()
        {
            @Override
            public String getContentAsString()
                throws UnsupportedEncodingException
            {
                String errorMessage = getErrorMessage();
                return ( errorMessage != null ) ? errorMessage : super.getContentAsString();
            }
        };
        this.unauthenticatedRepositoryServlet.service( request, response );
        return response;
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

        InputStream inputStream;

        String contentType;

        public PutMethodWebRequest( String url, InputStream inputStream, String contentType )
            throws Exception
        {
            super( new URL( url ), HttpMethod.PUT );
            this.url = url;
            this.inputStream = inputStream;
            this.contentType = contentType;
        }


    }

    public static class ServletUnitClient
    {

        AbstractRepositoryServletTestCase abstractRepositoryServletTestCase;

        public ServletUnitClient( AbstractRepositoryServletTestCase abstractRepositoryServletTestCase )
        {
            this.abstractRepositoryServletTestCase = abstractRepositoryServletTestCase;
        }

        public WebResponse getResponse( WebRequest request )
            throws Exception
        {
            return getResponse( request, false );
        }

        public WebResponse getResponse( WebRequest request, boolean followRedirect )
            throws Exception
        {
            // alwasy following redirect as it's normal
            return abstractRepositoryServletTestCase.getWebResponse( request );//, followRedirect );
        }

        public WebResponse getResource( WebRequest request )
            throws Exception
        {
            return getResponse( request );
        }
    }

    public ServletUnitClient getServletUnitClient()
    {
        return new ServletUnitClient( this );
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
        Assert.assertEquals( "Should have been an OK response code", //
                             HttpServletResponse.SC_OK, //
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
