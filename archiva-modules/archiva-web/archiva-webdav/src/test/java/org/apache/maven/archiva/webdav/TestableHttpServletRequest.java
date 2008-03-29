/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.archiva.webdav;

import org.apache.commons.lang.NotImplementedException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * TestableHttpServletRequest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: TestableHttpServletRequest.java 6940 2007-10-16 01:02:02Z joakime $
 */
public class TestableHttpServletRequest
    implements HttpServletRequest
{

    public TestableHttpServletRequest()
    {
        setDefaults();
    }

    public void setDefaults()
    {
        authType = null;
        scheme = "http";
        protocol = "HTTP/1.1";
        serverName = "localhost";
        serverPort = 80;
        remoteHost = "localhost";
    }

    private String authType;

    private String characterEncoding;

    private int contentLength;

    private String contentType;

    private String contextPath;

    private Locale locale;

    private String method;

    private String pathInfo;

    private String pathTranslated;

    private String protocol;

    private String queryString;

    private String remoteAddr;

    private String remoteHost;

    private String remoteUser;

    private String requestedSessionId;

    private boolean requestedSessionIdFromCookie;

    private boolean requestedSessionIdFromUrl;

    private boolean requestedSessionIdValid;

    private StringBuffer requestURL;

    private String scheme;

    private boolean secure;

    private String serverName;

    private int serverPort;

    private String servletPath;

    public Object getAttribute( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getAttribute(String)" ) );
    }

    public Enumeration getAttributeNames()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getAttributeNames()" ) );
    }

    public String getAuthType()
    {
        return authType;
    }

    public String getCharacterEncoding()
    {
        return characterEncoding;
    }

    public int getContentLength()
    {
        return contentLength;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public Cookie[] getCookies()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getCookies()" ) );
    }

    public long getDateHeader( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getDateHeader(String)" ) );
    }

    public String getHeader( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getHeader(String)" ) );
    }

    private Map headers = new HashMap();

    public Enumeration getHeaderNames()
    {
        return new IterEnumeration( headers.keySet().iterator() );
    }

    public Enumeration getHeaders( String name )
    {
        throw new NotImplementedException( notImplemented( ".getHeaders(String)" ) );
    }

    public ServletInputStream getInputStream()
        throws IOException
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getInputStream()" ) );
    }

    public int getIntHeader( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getIntHeader(String)" ) );
    }

    public Locale getLocale()
    {
        return locale;
    }

    public Enumeration getLocales()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getLocales()" ) );
    }

    public String getMethod()
    {
        return method;
    }

    public String getParameter( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getParameter(String)" ) );
    }

    public Map getParameterMap()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getParameterMap()" ) );
    }

    public Enumeration getParameterNames()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getParameterNames()" ) );
    }

    public String[] getParameterValues( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getParameterValues(String)" ) );
    }

    public String getPathInfo()
    {
        return pathInfo;
    }

    public String getPathTranslated()
    {
        return pathTranslated;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public BufferedReader getReader()
        throws IOException
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getReader()" ) );
    }

    public String getRealPath( String path )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getRealPath(String)" ) );
    }

    public String getRemoteAddr()
    {
        return remoteAddr;
    }

    public String getRemoteHost()
    {
        return remoteHost;
    }

    public String getRemoteUser()
    {
        return remoteUser;
    }

    public RequestDispatcher getRequestDispatcher( String path )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getRequestDispatcher(String)" ) );
    }

    public String getRequestedSessionId()
    {
        return requestedSessionId;
    }

    public String getRequestURI()
    {
        return requestURL.toString();
    }

    public StringBuffer getRequestURL()
    {
        return requestURL;
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getServerName()
    {
        return serverName;
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public String getServletPath()
    {
        return servletPath;
    }

    public HttpSession getSession()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getSession()" ) );
    }

    public HttpSession getSession( boolean create )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getSession(boolean)" ) );
    }

    public Principal getUserPrincipal()
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".getUserPrincipal()" ) );
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return requestedSessionIdFromCookie;
    }

    public boolean isRequestedSessionIdFromUrl()
    {
        return requestedSessionIdFromUrl;
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return requestedSessionIdFromUrl;
    }

    public boolean isRequestedSessionIdValid()
    {
        return requestedSessionIdValid;
    }

    public boolean isSecure()
    {
        return secure;
    }

    public boolean isUserInRole( String role )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".isUserInRole(String)" ) );
    }

    public void removeAttribute( String name )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".removeAttribute(String)" ) );
    }

    public void setAttribute( String name, Object o )
    {
        // TODO: Implement if needed.
        throw new NotImplementedException( notImplemented( ".setAttribute(String, Object)" ) );
    }

    public void setCharacterEncoding( String encoding )
        throws UnsupportedEncodingException
    {
        this.characterEncoding = encoding;
    }

    public void setContentLength( int contentLength )
    {
        this.contentLength = contentLength;
    }

    public void setContentType( String contentType )
    {
        this.contentType = contentType;
    }

    public void setContextPath( String contextPath )
    {
        this.contextPath = contextPath;
    }

    public void setMethod( String method )
    {
        this.method = method;
    }

    public void setPathInfo( String pathInfo )
    {
        this.pathInfo = pathInfo;
    }

    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    }

    public void setQueryString( String queryString )
    {
        this.queryString = queryString;
    }

    public void setScheme( String scheme )
    {
        this.scheme = scheme;
    }

    public void setSecure( boolean secure )
    {
        this.secure = secure;
    }

    public void setServerName( String serverName )
    {
        this.serverName = serverName;
    }

    public void setServerPort( int serverPort )
    {
        this.serverPort = serverPort;
    }

    public void setServletPath( String servletPath )
    {
        this.servletPath = servletPath;
    }

    public void setUrl( String urlString )
        throws MalformedURLException
    {
        URL url = new URL( urlString );
        this.queryString = url.getQuery();
        this.scheme = url.getProtocol();
        this.serverName = url.getHost();
        this.serverPort = url.getPort();

        String path = url.getPath();
        if ( !path.startsWith( this.servletPath ) )
        {
            throw new MalformedURLException( "Unable to operate on request path [" + path
                + "] outside of servletPath [" + this.servletPath + "]." );
        }

        this.pathInfo = path.substring( this.servletPath.length() );
        this.requestURL = new StringBuffer( this.pathInfo );
    }

    private String notImplemented( String msg )
    {
        return msg + " is not implemented in " + this.getClass().getName();
    }

    class IterEnumeration
        implements Enumeration
    {
        private Iterator iter;

        public IterEnumeration( Iterator it )
        {
            this.iter = it;
        }

        public boolean hasMoreElements()
        {
            return this.iter.hasNext();
        }

        public Object nextElement()
        {
            return this.iter.next();
        }
    }
}
