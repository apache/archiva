package org.apache.archiva.redback.integration.filter.authorization;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.integration.filter.SpringServletFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SimpleAuthorizationFilter
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class SimpleAuthorizationFilter
    extends SpringServletFilter
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private String permission;

    private String resource;

    private String accessDeniedLocation;

    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        super.init( filterConfig );

        permission = filterConfig.getInitParameter( "permission" );
        resource = filterConfig.getInitParameter( "resource" );
        accessDeniedLocation = filterConfig.getInitParameter( "accessDeniedLocation" );

        if ( StringUtils.isEmpty( accessDeniedLocation ) )
        {
            throw new ServletException(
                "Missing parameter 'accessDeniedLocation' from " + SimpleAuthorizationFilter.class.getName()
                    + " configuration." );
        }
    }

    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws IOException, ServletException
    {
        SecuritySession securitySession = getApplicationContext().getBean( "securitySession", SecuritySession.class );

        if ( securitySession == null )
        {
            logger.warn( "Security Session is null." );
            return;
        }

        SecuritySystem securitySystem = getApplicationContext().getBean( "securitySystem", SecuritySystem.class );

        boolean isAuthorized = false;

        try
        {
            if ( StringUtils.isEmpty( resource ) )
            {
                isAuthorized = securitySystem.isAuthorized( securitySession, permission );
            }
            else
            {
                isAuthorized = securitySystem.isAuthorized( securitySession, permission, resource );
            }
            if ( isAuthorized )
            {
                chain.doFilter( request, response );
            }
            else
            {
                accessDenied( response );
            }
        }
        catch ( AuthorizationException e )
        {
            accessDenied( response );
        }
    }

    protected void accessDenied( ServletResponse response )
        throws IOException
    {
        String newlocation = accessDeniedLocation;

        if ( newlocation.indexOf( '?' ) == ( -1 ) )
        {
            newlocation += "?";
        }
        else
        {
            newlocation += "&";
        }
        newlocation += "resource=" + resource;

        ( (HttpServletResponse) response ).sendRedirect( newlocation );
    }

}
