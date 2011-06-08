package org.apache.maven.archiva.security;

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

import com.google.common.collect.Lists;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * DefaultUserRepositories
 *
 * @version $Id$
 *          plexus.component role="org.apache.maven.archiva.security.UserRepositories" role-hint="default"
 */
@Service( "userRepositories" )
public class DefaultUserRepositories
    implements UserRepositories
{
    /**
     * plexus.requirement
     */
    @Inject
    private SecuritySystem securitySystem;

    /**
     * plexus.requirement role-hint="default"
     */
    @Inject
    private RoleManager roleManager;

    /**
     * plexus.requirement
     */
    @Inject
    private ArchivaConfiguration archivaConfiguration;

    private Logger log = LoggerFactory.getLogger( DefaultUserRepositories.class );

    public List<String> getObservableRepositoryIds( String principal )
        throws PrincipalNotFoundException, AccessDeniedException, ArchivaSecurityException
    {
        String operation = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS;

        return getAccessibleRepositoryIds( principal, operation );
    }

    public List<String> getManagableRepositoryIds( String principal )
        throws PrincipalNotFoundException, AccessDeniedException, ArchivaSecurityException
    {
        String operation = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD;

        return getAccessibleRepositoryIds( principal, operation );
    }

    private List<String> getAccessibleRepositoryIds( String principal, String operation )
        throws ArchivaSecurityException, AccessDeniedException, PrincipalNotFoundException
    {
        SecuritySession securitySession = createSession( principal );

        List<String> repoIds = new ArrayList<String>();

        List<ManagedRepositoryConfiguration> repos = archivaConfiguration.getConfiguration().getManagedRepositories();

        for ( ManagedRepositoryConfiguration repo : repos )
        {
            try
            {
                String repoId = repo.getId();
                if ( securitySystem.isAuthorized( securitySession, operation, repoId ) )
                {
                    repoIds.add( repoId );
                }
            }
            catch ( AuthorizationException e )
            {
                // swallow.
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Not authorizing '{}' for repository '{}': {}",
                               Lists.<Object>newArrayList( principal, repo.getId(), e.getMessage() ) );
                }
            }
        }

        return repoIds;
    }

    private SecuritySession createSession( String principal )
        throws ArchivaSecurityException, AccessDeniedException
    {
        User user;
        try
        {
            user = securitySystem.getUserManager().findUser( principal );
            if ( user == null )
            {
                throw new ArchivaSecurityException(
                    "The security system had an internal error - please check your system logs" );
            }
        }
        catch ( UserNotFoundException e )
        {
            throw new PrincipalNotFoundException( "Unable to find principal " + principal + "" );
        }

        if ( user.isLocked() )
        {
            throw new AccessDeniedException( "User " + principal + "(" + user.getFullName() + ") is locked." );
        }

        AuthenticationResult authn = new AuthenticationResult( true, principal, null );
        return new DefaultSecuritySession( authn, user );
    }

    public void createMissingRepositoryRoles( String repoId )
        throws ArchivaSecurityException
    {
        try
        {
            if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
            {
                roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
            }

            if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
            {
                roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
            }
        }
        catch ( RoleManagerException e )
        {
            throw new ArchivaSecurityException( "Unable to create roles for configured repositories: " + e.getMessage(),
                                                e );
        }
    }

    public boolean isAuthorizedToUploadArtifacts( String principal, String repoId )
        throws PrincipalNotFoundException, ArchivaSecurityException
    {
        try
        {
            SecuritySession securitySession = createSession( principal );

            return securitySystem.isAuthorized( securitySession, ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD,
                                                repoId );

        }
        catch ( AuthorizationException e )
        {
            throw new ArchivaSecurityException( e.getMessage() );
        }
    }

    public boolean isAuthorizedToDeleteArtifacts( String principal, String repoId )
        throws AccessDeniedException, ArchivaSecurityException
    {
        try
        {
            SecuritySession securitySession = createSession( principal );

            return securitySystem.isAuthorized( securitySession, ArchivaRoleConstants.OPERATION_REPOSITORY_DELETE,
                                                repoId );

        }
        catch ( AuthorizationException e )
        {
            throw new ArchivaSecurityException( e.getMessage() );
        }
    }

    public SecuritySystem getSecuritySystem()
    {
        return securitySystem;
    }

    public void setSecuritySystem( SecuritySystem securitySystem )
    {
        this.securitySystem = securitySystem;
    }

    public RoleManager getRoleManager()
    {
        return roleManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }
}
