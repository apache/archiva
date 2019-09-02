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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * UnauthenticatedRepositoryServlet
 *
 *
 */
public class UnauthenticatedRepositoryServlet
    extends RepositoryServlet
{
    @Override
    public void initServers( ServletConfig servletConfig )
    {
        rwLock.writeLock().lock();
        try {
            super.initServers(servletConfig);

            UnauthenticatedDavSessionProvider sessionProvider = new UnauthenticatedDavSessionProvider();
            setDavSessionProvider(sessionProvider);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    protected void service( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        String userAgent = request.getHeader( "User-Agent" );

        if ( StringUtils.isEmpty( userAgent ))
        {
            throw new ServletException( "User-Agent is not configured" );
        }

        super.service( request, response );
    }
}
