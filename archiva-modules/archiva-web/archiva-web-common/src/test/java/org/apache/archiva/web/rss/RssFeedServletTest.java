package org.apache.archiva.web.rss;

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


import junit.framework.TestCase;
import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.codec.Encoder;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration(
    locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context-test-common.xml",
        "classpath*:/spring-context-rss-servlet.xml" } )
public class RssFeedServletTest
    extends TestCase
{
    private RssFeedServlet rssFeedServlet = new RssFeedServlet();

    static String PREVIOUS_ARCHIVA_PATH;

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    protected RepositoryRegistry repositoryRegistry;

    @BeforeClass
    public static void initConfigurationPath()
        throws Exception
    {
        PREVIOUS_ARCHIVA_PATH = System.getProperty(ArchivaConfiguration.USER_CONFIG_PROPERTY);
        System.setProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY,
                            System.getProperty( "test.resources.path" ) + "/empty-archiva.xml" );
    }


    @AfterClass
    public static void restoreConfigurationPath()
        throws Exception
    {
        System.setProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY, PREVIOUS_ARCHIVA_PATH );
    }

    @Before
    @Override
    public void setUp()
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

        repositoryRegistry.reload();
        repositoryRegistry.putRepository( new BasicManagedRepository( "internal", "internal",
            new FilesystemStorage( Paths.get( "target/appserver-base/repositories/internal" ), new DefaultFileLockManager( ) ) ) );
        rssFeedServlet.init( mockServletConfig );
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
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
        public <T> T getBean( Class<T> aClass, Object... objects )
            throws BeansException
        {
            return applicationContext.getBean( aClass, objects );
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
        public Resource getResource( String s )
        {
            return applicationContext.getResource( s );
        }

        @Override
        public ClassLoader getClassLoader()
        {
            return applicationContext.getClassLoader();
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
    }


    @Test
    public void testRequestNewArtifactsInRepo()
        throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( "/feeds/test-repo" );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( "GET" );

        Base64 encoder = new Base64( 0, new byte[0] );
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encodeToString( userPass.getBytes() );
        request.addHeader( "Authorization", "BASIC " + encodedUserPass );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        rssFeedServlet.doGet( request, mockHttpServletResponse );

        assertEquals( RssFeedServlet.MIME_TYPE, mockHttpServletResponse.getHeader( "CONTENT-TYPE" ) );
        assertNotNull( "Should have recieved a response", mockHttpServletResponse.getContentAsString() );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK,
                      mockHttpServletResponse.getStatus() );

    }

    @Test
    public void testRequestNewVersionsOfArtifact()
        throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( "/feeds/org/apache/archiva/artifact-two" );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( "GET" );

        //WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/org/apache/archiva/artifact-two" );

        Base64 encoder = new Base64( 0, new byte[0] );
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encodeToString( userPass.getBytes() );
        request.addHeader( "Authorization", "BASIC " + encodedUserPass );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        rssFeedServlet.doGet( request, mockHttpServletResponse );

        assertEquals( RssFeedServlet.MIME_TYPE, mockHttpServletResponse.getHeader( "CONTENT-TYPE" ) );
        assertNotNull( "Should have recieved a response", mockHttpServletResponse.getContentAsString() );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK,
                      mockHttpServletResponse.getStatus() );
    }

    @Test
    public void testInvalidRequest()
        throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( "/feeds?invalid_param=xxx" );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( "GET" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        rssFeedServlet.doGet( request, mockHttpServletResponse );

        assertEquals( HttpServletResponse.SC_BAD_REQUEST, mockHttpServletResponse.getStatus() );

    }

    @Test
    public void testInvalidAuthenticationRequest()
        throws Exception
    {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( "/feeds/unauthorized-repo" );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( "GET" );

        Encoder encoder = new Base64();
        String userPass = "unauthUser:unauthPass";
        String encodedUserPass = new String( (byte[]) encoder.encode( userPass.getBytes() ) );
        request.addHeader( "Authorization", "BASIC " + encodedUserPass );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        rssFeedServlet.doGet( request, mockHttpServletResponse );

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );

    }

    @Test
    public void testUnauthorizedRequest()
        throws Exception
    {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( "/feeds/unauthorized-repo" );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( "GET" );

        Base64 encoder = new Base64( 0, new byte[0] );
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encodeToString( userPass.getBytes() );
        request.addHeader( "Authorization", "BASIC " + encodedUserPass );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        rssFeedServlet.doGet( request, mockHttpServletResponse );

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );

    }


}
