package org.apache.maven.archiva.web.servlet;

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

import org.codehaus.plexus.logging.AbstractLogEnabled;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * AbstractPlexusServlet
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractPlexusServlet
    extends AbstractLogEnabled
    implements PlexusServlet
{
    private ServletConfig servletConfig;

    private ServletContext servletContext;

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    public ServletContext getServletContext()
    {
        return servletContext;
    }

    public void servletDestroy()
    {
        // Do Nothing Here.
    }

    public void setServletConfig( ServletConfig config )
        throws ServletException
    {
        servletConfig = config;
    }

    public void setServletContext( ServletContext servletContext )
    {
        this.servletContext = servletContext;
    }
}
