package org.apache.archiva.redback.authorization.ldap;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authorization.AuthorizationDataSource;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.authorization.Authorizer;
import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapper;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service( "authorizer#ldap" )
public class LdapAuthorizer
    implements Authorizer
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "rbacManager#cached" )
    private RBACManager rbacManager;

    @Inject
    private LdapRoleMapper ldapRoleMapper;


    public String getId()
    {
        return "ldap";
    }

    public AuthorizationResult isAuthorized( AuthorizationDataSource source )
        throws AuthorizationException
    {

        String userName = StringUtils.isEmpty( source.getPrincipal() ) ? "guest" : source.getPrincipal();
        String operation = source.getPermission();
        String resource = source.getResource();
        try
        {
            List<String> ldapGroups = ldapRoleMapper.getGroups( userName );

            List<String> roles = mapLdapGroups( ldapGroups );

            Map<String, List<Permission>> permissionMap = getAssignedPermissionMap( roles );

            if ( permissionMap.keySet().contains( operation ) )
            {
                for ( Permission permission : permissionMap.get( operation ) )
                {

                    log.debug( "checking permission {} for operation {} resource {}",
                               ( permission != null ? permission.getName() : "null" ), operation, resource );

                    if ( evaluate( permission, operation, resource, userName ) )
                    {
                        return new AuthorizationResult( true, permission, null );
                    }
                }

                log.debug( "no permission found for operation {} resource {}", operation, resource );
            }
            else
            {
                log.debug( "permission map does not contain operation: {}", operation );
            }

        }
        catch ( MappingException e )
        {
            log.info( "skip MappingException trying to find LDAP roles for user: '{}", userName );
        }
        catch ( RbacManagerException e )
        {
            log.info( "skip RbacManagerException trying to find LDAP roles for user: '{}", userName );
        }
        return null;

    }

    protected List<String> mapLdapGroups( List<String> groups )
        throws MappingException
    {
        List<String> roles = new ArrayList<String>();

        Map<String, String> mapping = ldapRoleMapper.getLdapGroupMappings();

        for ( String group : groups )
        {
            String role = mapping.get( group );
            if ( role != null )
            {
                roles.add( role );
            }
        }

        return roles;
    }

    public Map<String, List<Permission>> getAssignedPermissionMap( List<String> roles )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return getPermissionMapByOperation( getAssignedPermissions( roles ) );
    }

    public Set<Permission> getAssignedPermissions( List<String> roles )
        throws RbacObjectNotFoundException, RbacManagerException
    {

        Set<Permission> permissionSet = new HashSet<Permission>();

        boolean childRoleNamesUpdated = false;

        Iterator<String> it = roles.iterator();
        while ( it.hasNext() )
        {
            String roleName = it.next();
            try
            {
                Role role = rbacManager.getRole( roleName );
                gatherUniquePermissions( role, permissionSet );
            }
            catch ( RbacObjectNotFoundException e )
            {
                // Found a bad role name. remove it!
                it.remove();
                childRoleNamesUpdated = true;
            }
        }

        return permissionSet;
    }

    private void gatherUniquePermissions( Role role, Collection<Permission> coll )
        throws RbacManagerException
    {
        if ( role.getPermissions() != null )
        {
            for ( Permission permission : role.getPermissions() )
            {
                if ( !coll.contains( permission ) )
                {
                    coll.add( permission );
                }
            }
        }

        if ( role.hasChildRoles() )
        {
            Map<String, Role> childRoles = getChildRoles( role );
            Iterator<Role> it = childRoles.values().iterator();
            while ( it.hasNext() )
            {
                Role child = it.next();
                gatherUniquePermissions( child, coll );
            }
        }
    }

    public Map<String, Role> getChildRoles( Role role )
        throws RbacManagerException
    {
        Map<String, Role> childRoles = new HashMap<String, Role>();

        boolean childRoleNamesUpdated = false;

        Iterator<String> it = role.getChildRoleNames().listIterator();
        while ( it.hasNext() )
        {
            String roleName = it.next();
            try
            {
                Role child = rbacManager.getRole( roleName );
                childRoles.put( child.getName(), child );
            }
            catch ( RbacObjectNotFoundException e )
            {
                // Found a bad roleName! - remove it.
                it.remove();
                childRoleNamesUpdated = true;
            }
        }

        return childRoles;
    }


    private Map<String, List<Permission>> getPermissionMapByOperation( Collection<Permission> permissions )
    {
        Map<String, List<Permission>> userPermMap = new HashMap<String, List<Permission>>();

        for ( Permission permission : permissions )
        {
            List<Permission> permList = userPermMap.get( permission.getOperation().getName() );

            if ( permList != null )
            {
                permList.add( permission );
            }
            else
            {
                List<Permission> newPermList = new ArrayList<Permission>( permissions.size() );
                newPermList.add( permission );
                userPermMap.put( permission.getOperation().getName(), newPermList );
            }
        }

        return userPermMap;
    }

    public boolean evaluate( Permission permission, String operation, String resource, String principal )
    {
        String permissionResource = permission.getResource().getIdentifier();

        // expression evaluation checking
        /*if ( permissionResource.startsWith( "${" ) )
        {
            String tempStr = permissionResource.substring( 2, permissionResource.indexOf( '}' ) );

            if ( "username".equals( tempStr ) )
            {
                try
                {
                    permissionResource = userManager.findUser( principal ).getUsername();
                }
                catch ( UserNotFoundException e )
                {
                    throw new PermissionEvaluationException( "unable to locate user to retrieve username", e );
                }
                catch ( UserManagerException e )
                {
                    throw new PermissionEvaluationException( "trouble finding user: " + e.getMessage(), e );
                }
            }
        }*/

        // check if this permission applies to the operation at all
        if ( permission.getOperation().getName().equals( operation ) )
        {
            // check if it is a global resource, if it is then since the operations match we return true
            if ( Resource.GLOBAL.equals( permission.getResource().getIdentifier() ) )
            {
                return true;
            }

            // if we are not checking a specific resource, the operation is enough
            if ( resource == null )
            {
                return true;
            }

            // check if the resource identifier of the permission matches the resource we are checking against
            // if it does then return true
            if ( permissionResource.equals( resource ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean isFinalImplementation()
    {
        return true;
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.authorizer.ldap";
    }
}
