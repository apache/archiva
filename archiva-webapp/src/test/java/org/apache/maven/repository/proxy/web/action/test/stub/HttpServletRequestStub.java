package org.apache.maven.repository.proxy.web.action.test.stub;

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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Enumeration;

public class HttpServletRequestStub
    extends ServletRequestStub
    implements HttpServletRequest
{

    public String getAuthType()
    {
        return null;
    }

    public String getContextPath()
    {
        return "/location1/location2/location3";
    }

    public Cookie[] getCookies()
    {
        return null;
    }

    public long getDateHeader( String name )
    {
        return -1;
    }

    public String getHeader( String name )
    {
        return null;
    }

    public Enumeration getHeaderNames()
    {
        return null;
    }

    public Enumeration getHeaders( String name )
    {
        return null;
    }

    public int getIntHeader( String name )
    {
        return -1;
    }

    public String getMethod()
    {
        return null;
    }

    public String getPathInfo()
    {
        return null;
    }

    public String getPathTranslated()
    {
        return null;
    }

    public String getQueryString()
    {
        return null;
    }

    public String getRemoteUser()
    {
        return null;
    }

    public String getRequestedSessionId()
    {
        return null;
    }

    public String getRequestURI()
    {
        return "/projectname/repository/org/sometest/artifact-0.0.jar";
    }

    public StringBuffer getRequestURL()
    {
        return null;
    }

    public String getServletPath()
    {
        return "/repository/org/sometest/artifact-0.0.jar";
    }

    public HttpSession getSession()
    {
        return null;
    }

    public HttpSession getSession( boolean create )
    {
        return null;
    }

    public Principal getUserPrincipal()
    {
        return null;
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    public boolean isUserInRole( String role )
    {
        return false;
    }
}
