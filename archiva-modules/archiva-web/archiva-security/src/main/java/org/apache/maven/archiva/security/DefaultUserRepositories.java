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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;

/**
 * DefaultUserRepositories
 * 
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.security.UserRepositories" role-hint="default"
 */
public class DefaultUserRepositories
    implements UserRepositories
{
    /**
     * @plexus.requirement
     */
    private SecuritySystem securitySystem;

    /**
     * @plexus.requirement role-hint="cached"
     */
    private RBACManager rbacManager;

    /**
     * @plexus.requirement role-hint="default"
     */
    private RoleManager roleManager;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    public List<String> getObservableRepositoryIds( String principal )
        throws PrincipalNotFoundException, AccessDeniedException, ArchivaSecurityException
    {

        try
        {
            User user = securitySystem.getUserManager().findUser( principal );

            if ( user.isLocked() )
            {
                throw new AccessDeniedException( "User " + principal + "(" + user.getFullName() + ") is locked." );
            }

            AuthenticationResult authn = new AuthenticationResult( true, principal, null );
            SecuritySession securitySession = new DefaultSecuritySession( authn, user );

            List<String> repoIds = new ArrayList<String>();

            List<ManagedRepositoryConfiguration> repos =
                archivaConfiguration.getConfiguration().getManagedRepositories();

            for ( ManagedRepositoryConfiguration repo : repos )
            {
                try
                {
                    String repoId = repo.getId();
                    if ( securitySystem.isAuthorized( securitySession,
                                                      ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS, repoId ) )
                    {
                        repoIds.add( repoId );
                    }
                }
                catch ( AuthorizationException e )
                {
                    // swallow.
                }
            }

            return repoIds;
        }
        catch ( UserNotFoundException e )
        {
            throw new PrincipalNotFoundException( "Unable to find principal " + principal + "" );
        }
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
            throw new ArchivaSecurityException(
                                                "Unable to create roles for configured repositories: " + e.getMessage(),
                                                e );
        }
    }

    public boolean isAuthorizedToUploadArtifacts( String principal, String repoId )
        throws PrincipalNotFoundException, ArchivaSecurityException
    {
        try
        {
            User user = securitySystem.getUserManager().findUser( principal );

            if ( user.isLocked() )
            {
                throw new AccessDeniedException( "User " + principal + "(" + user.getFullName() + ") is locked." );
            }

            AuthenticationResult authn = new AuthenticationResult( true, principal, null );
            SecuritySession securitySession = new DefaultSecuritySession( authn, user );

            return securitySystem.isAuthorized( securitySession, ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD,
                                                repoId );

        }
        catch ( UserNotFoundException e )
        {
            throw new PrincipalNotFoundException( "Unable to find principal " + principal + "" );
        }
        catch ( AuthorizationException e )
        {
            throw new ArchivaSecurityException( e.getMessage() );
        }
    }
}
