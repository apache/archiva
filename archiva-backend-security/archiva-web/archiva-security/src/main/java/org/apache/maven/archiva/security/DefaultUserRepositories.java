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

import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DefaultUserRepositories 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.security.UserRepositories"
 *                   role-hint="default"
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

            Map<String, List<Permission>> permissionMap = rbacManager.getAssignedPermissionMap( principal );
            
            List<String> repoIds = new ArrayList<String>();
            
            for( Entry<String,List<Permission>> entry: permissionMap.entrySet() )
            {
                List<Permission> perms = entry.getValue();
                
                for( Permission perm: perms )
                {
                    System.out.println( "Principal[" + principal + "] : Permission[" + entry.getKey() + "]:" + perm.getName() + " - Operation:"
                        + perm.getOperation().getName() + " - Resource:" + perm.getResource().getIdentifier() );
                }
            }
            
            System.out.println("-");
            
            return repoIds;
        }
        catch ( UserNotFoundException e )
        {
            throw new PrincipalNotFoundException( "Unable to find principal " + principal + "" );
        }
        catch ( RbacObjectNotFoundException e )
        {
            throw new PrincipalNotFoundException( "Unable to find user role assignments for user " + principal, e );
        }
        catch ( RbacManagerException e )
        {
            throw new ArchivaSecurityException( "Unable to initialize underlying security framework: " + e.getMessage(),
                                                e );
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
            throw new ArchivaSecurityException( "Unable to create roles for configured repositories: " + e.getMessage(),
                                                e );
        }
    }
}
