package org.apache.maven.archiva.web.tags;

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

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;

/**
 * PlexusTagUtil 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PlexusTagUtil
{
    public static Object lookup( PageContext pageContext, Class clazz )
        throws ComponentLookupException
    {
        return getContainer( pageContext ).lookup( clazz );
    }

    public static Object lookup( PageContext pageContext, String role )
        throws ComponentLookupException
    {
        return getContainer( pageContext ).lookup( role );
    }

    public static Object lookup( PageContext pageContext, Class clazz, String hint )
        throws ComponentLookupException
    {
        return getContainer( pageContext ).lookup( clazz, hint );
    }

    public static Object lookup( PageContext pageContext, String role, String hint )
        throws ComponentLookupException
    {
        return getContainer( pageContext ).lookup( role, hint );
    }

    public static PlexusContainer getContainer( PageContext pageContext )
        throws ComponentLookupException
    {
        ServletContext servletContext = pageContext.getServletContext();

        PlexusContainer xworkContainer = (PlexusContainer) servletContext.getAttribute( "webwork.plexus.container" );

        if ( xworkContainer != null )
        {
            servletContext.setAttribute( PlexusConstants.PLEXUS_KEY, xworkContainer );

            return xworkContainer;
        }

        PlexusContainer container = (PlexusContainer) servletContext.getAttribute( PlexusConstants.PLEXUS_KEY );
        if ( container == null )
        {
            throw new ComponentLookupException( "PlexusContainer is null." );
        }
        return container;
    }
}
