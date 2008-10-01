package org.apache.maven.archiva.webdav;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import junit.framework.TestCase;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.maven.archiva.security.ServletAuthenticator;
import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.struts2.filter.authentication.HttpAuthenticator;

public class ArchivaDavSessionProviderTest extends TestCase
{
    private DavSessionProvider sessionProvider;
    
    private WebdavRequest request;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        sessionProvider = new ArchivaDavSessionProvider(new ServletAuthenticatorMock(), new HttpAuthenticatorMock(), null);
        request = new WebdavRequestImpl(new HttpServletRequestMock(), null);
    }
    
    public void testAttachSession()
        throws Exception
    {
        assertNull(request.getDavSession());
        sessionProvider.attachSession(request);
        assertNotNull(request.getDavSession());
    }
    
    public void testReleaseSession()
        throws Exception
    {
        assertNull(request.getDavSession());
        sessionProvider.attachSession(request);
        assertNotNull(request.getDavSession());
        
        sessionProvider.releaseSession(request);
        assertNull(request.getDavSession());
    }
    
    private class HttpServletRequestMock implements HttpServletRequest
    {
        public Object getAttribute(String arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Enumeration getAttributeNames() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getCharacterEncoding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getContentLength() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getContentType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ServletInputStream getInputStream()
            throws IOException
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getLocalAddr() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getLocalName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getLocalPort() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Locale getLocale() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Enumeration getLocales()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getParameter(String arg0)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Map getParameterMap()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Enumeration getParameterNames()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String[] getParameterValues(String arg0)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getProtocol() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public BufferedReader getReader()
            throws IOException 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRealPath(String arg0)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRemoteAddr()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRemoteHost()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getRemotePort()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public RequestDispatcher getRequestDispatcher(String arg0)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getScheme()
        {
            return "";
        }

        public String getServerName()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getServerPort()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isSecure()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void removeAttribute(String arg0)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setAttribute(String arg0, Object arg1)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setCharacterEncoding(String arg0)
            throws UnsupportedEncodingException
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        

        public String getAuthType()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getContextPath()
        {
            return "/";
        }

        public Cookie[] getCookies()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public long getDateHeader(String arg0) 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getHeader(String arg0) {
            return "";
        }

        public Enumeration getHeaderNames()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Enumeration getHeaders(String arg0) 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getIntHeader(String arg0) 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getMethod()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getPathInfo()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getPathTranslated()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getQueryString()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRemoteUser()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRequestURI()
        {
            return "/";
        }

        public StringBuffer getRequestURL() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getRequestedSessionId() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getServletPath() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public HttpSession getSession(boolean arg0) 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public HttpSession getSession() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Principal getUserPrincipal() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isRequestedSessionIdFromCookie() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isRequestedSessionIdFromURL() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isRequestedSessionIdFromUrl() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isRequestedSessionIdValid() 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isUserInRole(String arg0) 
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    private class ServletAuthenticatorMock implements ServletAuthenticator
    {
        public boolean isAuthenticated(HttpServletRequest arg0, AuthenticationResult arg1)
            throws AuthenticationException, AccountLockedException, MustChangePasswordException
        {
            return true;
        }

        public boolean isAuthorized(HttpServletRequest arg0, SecuritySession arg1, String arg2, boolean arg3)
            throws AuthorizationException, UnauthorizedException
        {
            return true;
        }

        public boolean isAuthorized(String arg0, String arg1)
            throws UnauthorizedException
        {
            return true;
        }   
    }
    
    private class HttpAuthenticatorMock extends HttpAuthenticator
    {
        @Override
        public void challenge(HttpServletRequest arg0, HttpServletResponse arg1, String arg2, AuthenticationException arg3)
            throws IOException
        {
            //Do nothing
        }

        @Override
        public AuthenticationResult getAuthenticationResult(HttpServletRequest arg0, HttpServletResponse arg1)
            throws AuthenticationException, AccountLockedException, MustChangePasswordException
        {
            return new AuthenticationResult();
        }
        

        @Override
        public AuthenticationResult authenticate(AuthenticationDataSource arg0)
            throws AuthenticationException, AccountLockedException, MustChangePasswordException
        {
            return new AuthenticationResult();
        }

        @Override
        public void authenticate(HttpServletRequest arg0, HttpServletResponse arg1)
            throws AuthenticationException
        {
            //Do nothing
        }

        @Override
        public Map getContextSession()
        {
            return super.getContextSession();
        }

        @Override
        public SecuritySession getSecuritySession()
        {
            return super.getSecuritySession();
        }

        @Override
        public User getSessionUser()
        {
            return super.getSessionUser();
        }

        @Override
        public boolean isAlreadyAuthenticated()
        {
            return super.isAlreadyAuthenticated();
        }

        @Override
        public void setSecuritySession(SecuritySession session)
        {
            super.setSecuritySession(session);
        }

        @Override
        public void setSessionUser(User user) {
            super.setSessionUser(user);
        }

        @Override
        public String storeDefaultUser(String user) {
            return super.storeDefaultUser(user);
        }
        
    }
}
