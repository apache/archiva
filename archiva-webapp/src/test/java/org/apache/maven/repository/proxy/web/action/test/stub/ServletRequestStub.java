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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ServletRequestStub
    implements ServletRequest
{

    public Object getAttribute( String key )
    {
        return null;
    }

    public Enumeration getAttributeNames()
    {
        return null;
    }

    public String getCharacterEncoding()
    {
        return null;
    }

    public int getContentLength()
    {
        return -1;
    }

    public int getRemotePort()
    {
        return -1;
    }

    public int getLocalPort()
    {
        return -1;
    }

    public String getLocalAddr()
    {
        return null;
    }

    public String getLocalName()
    {
        return null;
    }

    public String getContentType()
    {
        return null;
    }

    public ServletInputStream getInputStream()
    {
        return null;
    }

    public Locale getLocale()
    {
        return null;
    }

    public Enumeration getLocales()
    {
        return null;
    }

    public String getParameter( String name )
    {
        return null;
    }

    public Map getParameterMap()
    {
        HashMap parameterMap = new HashMap();

        parameterMap.put( "key1", "value1" );
        parameterMap.put( "key2", "value2" );

        return parameterMap;
    }

    public Enumeration getParameterNames()
    {
        return null;
    }

    public String[] getParameterValues( String name )
    {
        return null;
    }

    public String getProtocol()
    {
        return null;
    }

    public BufferedReader getReader()
    {
        return null;
    }

    public String getRealPath( String path )
    {
        return null;
    }

    public String getRemoteAddr()
    {
        return null;
    }

    public String getRemoteHost()
    {
        return null;
    }

    public RequestDispatcher getRequestDispatcher( String path )
    {
        return null;
    }

    public String getScheme()
    {
        return null;
    }

    public String getServerName()
    {
        return null;
    }

    public int getServerPort()
    {
        return -1;
    }

    public boolean isSecure()
    {
        return false;
    }

    public void removeAttribute( String name )
    {

    }

    public void setAttribute( String name, Object value )
    {

    }

    public void setCharacterEncoding( String env )
    {

    }
}
