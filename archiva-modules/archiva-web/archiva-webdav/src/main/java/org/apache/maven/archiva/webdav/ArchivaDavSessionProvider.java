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
import org.apache.maven.archiva.security.ServletAuthenticator;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.xwork.filter.authentication.HttpAuthenticator;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class ArchivaDavSessionProvider implements DavSessionProvider
{
    private Logger log = LoggerFactory.getLogger(ArchivaDavSessionProvider.class);
    
    private ServletAuthenticator servletAuth;    
    
    private HttpAuthenticator httpAuth;
            
    public ArchivaDavSessionProvider(WebApplicationContext applicationContext)
    {
        servletAuth = (ServletAuthenticator) applicationContext.getBean( PlexusToSpringUtils.buildSpringId( ServletAuthenticator.class.getName() ) );
        httpAuth =
            (HttpAuthenticator) applicationContext.getBean( PlexusToSpringUtils.buildSpringId( HttpAuthenticator.ROLE, "basic" ) );
    }

    public boolean attachSession(WebdavRequest request) throws DavException
    {
        final String repositoryId = RepositoryPathUtil.getRepositoryName(removeContextPath(request));
        
        try
        {
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, null );
            SecuritySession securitySession = httpAuth.getSecuritySession();
            
            return servletAuth.isAuthenticated(request, result, repositoryId) && 
                servletAuth.isAuthorized(request, securitySession, repositoryId, WebdavMethodUtil.isWriteMethod( request.getMethod() ) );
        }
        catch ( AuthenticationException e )
        {
            log.error( "Cannot authenticate user.", e );
            throw new UnauthorizedDavException(repositoryId, "You are not authenticated");
        }
        catch ( MustChangePasswordException e )
        {
            log.error( "User must change password." );
            throw new UnauthorizedDavException(repositoryId, "You must change your password.");
        }
        catch ( AccountLockedException e )
        {
            log.error( "User account is locked." );
            throw new UnauthorizedDavException(repositoryId, "User account is locked.");
        }
        catch ( AuthorizationException e )
        {
            log.error( "Fatal Authorization Subsystem Error." );
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fatal Authorization Subsystem Error." );
        }
        catch ( UnauthorizedException e )
        {
            log.error( e.getMessage() );
            throw new UnauthorizedDavException(repositoryId, e.getMessage() );
        }
    }

    public void releaseSession(WebdavRequest webdavRequest)
    {
        
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
