package org.apache.maven.archiva.webdav;

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

import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.maven.archiva.webdav.util.WebdavMethodUtil;
import org.apache.maven.archiva.webdav.util.RepositoryPathUtil;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.xwork.filter.authentication.HttpAuthenticator;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class ArchivaDavSessionProvider implements DavSessionProvider
{
    private Logger log = LoggerFactory.getLogger(ArchivaDavSessionProvider.class);

    private SecuritySystem securitySystem;

    private HttpAuthenticator httpAuth;

    public ArchivaDavSessionProvider(WebApplicationContext applicationContext)
    {
        securitySystem = (SecuritySystem) applicationContext.getBean( PlexusToSpringUtils.buildSpringId( SecuritySystem.ROLE ) );
        httpAuth = (HttpAuthenticator) applicationContext.getBean( PlexusToSpringUtils.buildSpringId( HttpAuthenticator.ROLE, "basic" ) );
    }

    public boolean attachSession(WebdavRequest request) throws DavException
    {
        final String repositoryId = RepositoryPathUtil.getRepositoryName(removeContextPath(request));
        return isAuthenticated(request, repositoryId) && isAuthorized(request, repositoryId);
    }

    public void releaseSession(WebdavRequest webdavRequest)
    {
    }

    protected boolean isAuthenticated( WebdavRequest request, String repositoryId )
        throws DavException
    {
        // Authentication Tests.
        try
        {
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, null );

            if ( result != null && !result.isAuthenticated() )
            {
                throw new UnauthorizedDavException(repositoryId, "User Credentials Invalid");
            }
        }
        catch ( AuthenticationException e )
        {
            throw new UnauthorizedDavException(repositoryId, "You are not authenticated");
        }
        catch ( AccountLockedException e )
        {
            throw new UnauthorizedDavException(repositoryId, "User account is locked.");
        }
        catch ( MustChangePasswordException e )
        {
            throw new UnauthorizedDavException(repositoryId, "You must change your password.");
        }

        return true;
    }

    protected boolean isAuthorized( WebdavRequest request, String repositoryId )
        throws DavException
    {
        // Authorization Tests.
        final boolean isWriteRequest = WebdavMethodUtil.isWriteMethod( request.getMethod() );

        SecuritySession securitySession = httpAuth.getSecuritySession();
        try
        {
            String permission = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS;

            if ( isWriteRequest )
            {
                permission = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD;
            }

            //DavServletRequestInfo requestInfo = new DavServletRequestInfo(request);

            AuthorizationResult authzResult =
                securitySystem.authorize( securitySession, permission, repositoryId);

            if ( !authzResult.isAuthorized() )
            {
                if ( authzResult.getException() != null )
                {
                    log.info( "Authorization Denied [ip=" + request.getRemoteAddr() + ",isWriteRequest=" + isWriteRequest +
                        ",permission=" + permission + ",repo=" + repositoryId + "] : " +
                        authzResult.getException().getMessage() );
                }
                throw new UnauthorizedDavException(repositoryId, "Access denied for repository " + repositoryId);
            }
        }
        catch ( AuthorizationException e )
        {
            throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fatal Authorization Subsystem Error." );
        }

        return true;
    }

    private String removeContextPath(final DavServletRequest request)
    {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        return path;
    }
}
