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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;

/**
 * PlexusTagUtil
 *
 * @version $Id$
 */
public class PlexusTagUtil
{
    public static Object lookup( PageContext pageContext, Class<?> clazz )
        throws PlexusSisuBridgeException
    {
        return getContainer( pageContext ).lookup( clazz );
    }

    private static PlexusSisuBridge getContainer( PageContext pageContext )
    {
        ServletContext servletContext = pageContext.getServletContext();

        WebApplicationContext webApplicationContext =
            WebApplicationContextUtils.getRequiredWebApplicationContext( pageContext.getServletContext() );

        return webApplicationContext.getBean( PlexusSisuBridge.class );


    }
}
