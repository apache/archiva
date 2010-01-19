package org.apache.maven.archiva.web.util;

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

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

/**
 * ContextUtils 
 *
 * @version $Id$
 */
public class ContextUtils
{
    private static final Map<String, Integer> defaultSchemePortMap;

    static
    {
        defaultSchemePortMap = new HashMap<String, Integer>();
        defaultSchemePortMap.put( "http", new Integer( 80 ) );
        defaultSchemePortMap.put( "https", new Integer( 443 ) );
    }

    /**
     * Using the page context, get the base url.
     * 
     * @param pageContext the page context to use
     * @return the base url with module name.
     */
    public static String getBaseURL( PageContext pageContext )
    {
        return getBaseURL( pageContext, null );
    }

    /**
     * Using the page context, get the base url and append an optional resource name to the end of the provided url.
     * 
     * @param pageContext the page context to use
     * @param resource the resource name (or null if no resource name specified)
     * @return the base url with resource name.
     */
    public static String getBaseURL( PageContext pageContext, String resource )
    {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        return getBaseURL( request, resource );
    }
    
    /**
     * Using the http servlet request, get the base url and append an optional resource name to the end of the url.
     * 
     * @param request the request to use
     * @param resource the resource name (or null if not resource name should be appended)
     * @return the base url with resource name.
     */
    public static String getBaseURL( HttpServletRequest request, String resource )
    {
        StringBuffer baseUrl = new StringBuffer();
        
        baseUrl.append( request.getScheme() ).append( "://" );
        baseUrl.append( getServerName( request ) );
        baseUrl.append( request.getContextPath() );

        if ( StringUtils.isNotBlank( resource ) )
        {
            if ( !baseUrl.toString().endsWith( "/" ) )
            {
                baseUrl.append( "/" );
            }

            baseUrl.append( resource );
        }

        return baseUrl.toString();
    }

    private static String getServerName( HttpServletRequest request )
    {
        String name = request.getHeader( "X-Forwarded-Host" );
        if ( name == null )
        {
            name = request.getServerName();
            int portnum = request.getServerPort();

            // Only add port if non-standard.
            Integer defaultPortnum = (Integer) defaultSchemePortMap.get( request.getScheme() );
            if ( ( defaultPortnum == null ) || ( defaultPortnum.intValue() != portnum ) )
            {
                name = name + ":" + String.valueOf( portnum );
            }
            return name;
        }
        else
        {
            // respect chains of proxies, return first one (as it's the outermost visible one)
            String[] hosts = name.split( "," );
            name = hosts[0].trim();
        }
        return name;
    }
}
