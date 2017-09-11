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

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.webdav.util.RepositoryPathUtil;
import org.apache.archiva.webdav.util.WebdavMethodUtil;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

/**
 */
public class ArchivaDavSessionProvider
    implements DavSessionProvider
{
    private ServletAuthenticator servletAuth;

    private HttpAuthenticator httpAuth;

    public ArchivaDavSessionProvider( ServletAuthenticator servletAuth, HttpAuthenticator httpAuth )
    {
        this.servletAuth = servletAuth;
        this.httpAuth = httpAuth;
    }

    @Override
    public boolean attachSession( WebdavRequest request )
        throws DavException
    {
        final String repositoryId = RepositoryPathUtil.getRepositoryName( removeContextPath( request ) );

        try
        {
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, null );

            //Create a dav session
            request.setDavSession( new ArchivaDavSession() );

            return servletAuth.isAuthenticated( request, result );
        }
        catch ( AuthenticationException e )
        {
            // safety check for MRM-911            
            String guest = UserManager.GUEST_USERNAME;
            try
            {
                if ( servletAuth.isAuthorized( guest,
                                               ( (ArchivaDavResourceLocator) request.getRequestLocator() ).getRepositoryId(),
                                               WebdavMethodUtil.getMethodPermission( request.getMethod() ) ) )
                {
                    request.setDavSession( new ArchivaDavSession() );
                    return true;
                }
            }
            catch ( UnauthorizedException ae )
            {
                throw new UnauthorizedDavException( repositoryId,
                                                    "You are not authenticated and authorized to access any repository." );
            }

            throw new UnauthorizedDavException( repositoryId, "You are not authenticated." );
        }
        catch ( MustChangePasswordException e )
        {
            throw new UnauthorizedDavException( repositoryId, "You must change your password." );
        }
        catch ( AccountLockedException e )
        {
            throw new UnauthorizedDavException( repositoryId, "User account is locked." );
        }
    }

    @Override
    public void releaseSession( WebdavRequest request )
    {
        request.setDavSession( null );
    }

    private String removeContextPath( final DavServletRequest request )
    {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if ( path.startsWith( ctx ) )
        {
            path = path.substring( ctx.length() );
        }
        return path;
    }
}
