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

import junit.framework.TestCase;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class ArchivaDavSessionProviderTest
    extends TestCase
{
    private DavSessionProvider sessionProvider;

    private WebdavRequest request;

    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        sessionProvider = new ArchivaDavSessionProvider( new ServletAuthenticatorMock(), new HttpAuthenticatorMock() );
        request = new WebdavRequestImpl( new MockHttpServletRequest(), null );
    }

    @Test
    public void testAttachSession()
        throws Exception
    {
        assertNull( request.getDavSession() );
        sessionProvider.attachSession( request );
        assertNotNull( request.getDavSession() );
    }

    @Test
    public void testReleaseSession()
        throws Exception
    {
        assertNull( request.getDavSession() );
        sessionProvider.attachSession( request );
        assertNotNull( request.getDavSession() );

        sessionProvider.releaseSession( request );
        assertNull( request.getDavSession() );
    }

    @SuppressWarnings( "unchecked" )
    /*
    private class HttpServletRequestMock
        implements HttpServletRequest
    {

        @Override
        public long getContentLengthLong()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String changeSessionId()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade( Class<T> handlerClass )
            throws IOException, ServletException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean authenticate( HttpServletResponse httpServletResponse )
            throws IOException, ServletException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public void login( String s, String s1 )
            throws ServletException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public void logout()
            throws ServletException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Collection<Part> getParts()
            throws IOException, ServletException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Part getPart( String s )
            throws IOException, ServletException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public ServletContext getServletContext()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public AsyncContext startAsync()
            throws IllegalStateException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public AsyncContext startAsync( ServletRequest servletRequest, ServletResponse servletResponse )
            throws IllegalStateException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isAsyncStarted()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isAsyncSupported()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public AsyncContext getAsyncContext()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public DispatcherType getDispatcherType()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Object getAttribute( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Enumeration getAttributeNames()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getCharacterEncoding()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public int getContentLength()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getContentType()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public ServletInputStream getInputStream()
            throws IOException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getLocalAddr()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getLocalName()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public int getLocalPort()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Locale getLocale()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Enumeration getLocales()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getParameter( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Map getParameterMap()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Enumeration getParameterNames()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String[] getParameterValues( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getProtocol()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public BufferedReader getReader()
            throws IOException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getRealPath( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getRemoteAddr()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getRemoteHost()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public int getRemotePort()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public RequestDispatcher getRequestDispatcher( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getScheme()
        {
            return "";
        }

        @Override
        public String getServerName()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public int getServerPort()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isSecure()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public void removeAttribute( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public void setAttribute( String arg0, Object arg1 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public void setCharacterEncoding( String arg0 )
            throws UnsupportedEncodingException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }


        @Override
        public String getAuthType()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getContextPath()
        {
            return "/";
        }

        @Override
        public Cookie[] getCookies()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public long getDateHeader( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getHeader( String arg0 )
        {
            return "";
        }

        @Override
        public Enumeration getHeaderNames()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Enumeration getHeaders( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public int getIntHeader( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getMethod()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getPathInfo()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getPathTranslated()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getQueryString()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getRemoteUser()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getRequestURI()
        {
            return "/";
        }

        @Override
        public StringBuffer getRequestURL()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getRequestedSessionId()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getServletPath()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public HttpSession getSession( boolean arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public HttpSession getSession()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Principal getUserPrincipal()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isRequestedSessionIdFromCookie()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isRequestedSessionIdFromURL()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isRequestedSessionIdFromUrl()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isRequestedSessionIdValid()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isUserInRole( String arg0 )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }

    */
    private class ServletAuthenticatorMock
        implements ServletAuthenticator
    {
        @Override
        public boolean isAuthenticated( HttpServletRequest arg0, AuthenticationResult arg1 )
            throws AuthenticationException, AccountLockedException, MustChangePasswordException
        {
            return true;
        }

        @Override
        public boolean isAuthorized( HttpServletRequest request, SecuritySession securitySession, String repositoryId,
                                     String permission )
            throws AuthorizationException, UnauthorizedException
        {
            return true;
        }

        @Override
        public boolean isAuthorized( String principal, String repoId, String permission )
            throws UnauthorizedException
        {
            return true;
        }
    }

    private class HttpAuthenticatorMock
        extends HttpAuthenticator
    {
        @Override
        public void challenge( HttpServletRequest arg0, HttpServletResponse arg1, String arg2,
                               AuthenticationException arg3 )
            throws IOException
        {
            //Do nothing
        }

        @Override
        public AuthenticationResult getAuthenticationResult( HttpServletRequest arg0, HttpServletResponse arg1 )
            throws AuthenticationException, AccountLockedException, MustChangePasswordException
        {
            return new AuthenticationResult();
        }


        @Override
        public AuthenticationResult authenticate( AuthenticationDataSource arg0, HttpSession httpSession )
            throws AuthenticationException, AccountLockedException, MustChangePasswordException
        {
            return new AuthenticationResult();
        }

        @Override
        public void authenticate( HttpServletRequest arg0, HttpServletResponse arg1 )
            throws AuthenticationException
        {
            //Do nothing
        }

        @Override
        public SecuritySession getSecuritySession( HttpSession httpSession )
        {
            return super.getSecuritySession( httpSession );
        }

        @Override
        public User getSessionUser( HttpSession httpSession )
        {
            return super.getSessionUser( httpSession );
        }

        @Override
        public boolean isAlreadyAuthenticated( HttpSession httpSession )
        {
            return super.isAlreadyAuthenticated( httpSession );
        }
    }
}
