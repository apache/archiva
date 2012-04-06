package org.codehaus.redback.rest.services;
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

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.codehaus.redback.rest.api.model.ApplicationRoles;
import org.codehaus.redback.rest.api.model.Role;
import org.codehaus.redback.rest.api.model.User;
import org.codehaus.redback.rest.api.services.RoleManagementService;
import org.codehaus.redback.rest.api.services.UserService;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class RoleManagementServiceTest
    extends AbstractRestServicesTest
{


    @Test
    public void roleExist()
        throws Exception
    {
        assertTrue( getRoleManagementService( authorizationHeader ).roleExists( "guest" ) );
        assertFalse( getRoleManagementService( authorizationHeader ).roleExists( "foo" ) );
    }

    @Test( expected = ServerWebApplicationException.class )
    public void roleExistBadAuthz()
        throws Exception
    {
        try
        {
            assertTrue( getRoleManagementService( null ).roleExists( "guest" ) );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;
        }
    }

    @Test
    public void createUserThenAssignRole()
        throws Exception
    {
        try
        {
            User user = new User( "toto", "toto the king", "toto@toto.fr", false, false );
            user.setPassword( "foo123" );
            UserService userService = getUserService( authorizationHeader );
            userService.createUser( user );
            user = userService.getUser( "toto" );
            user.setPasswordChangeRequired( false );
            userService.updateUser( user );
            assertNotNull( user );
            assertEquals( "toto the king", user.getFullName() );
            assertEquals( "toto@toto.fr", user.getEmail() );

            // should fail toto doesn't have karma
            try
            {
                getUserService( encode( "toto", "foo123" ) ).getUsers();
                fail( "should fail with 403" );
            }
            catch ( ServerWebApplicationException e )
            {
                assertEquals( 403, e.getStatus() );

            }

            // assign the role and retry
            getRoleManagementService( authorizationHeader ).assignRole( "user-administrator", "toto" );

            userService.removeFromCache( "toto" );

            getUserService( encode( "toto", "foo123" ) ).getUsers();

            List<Role> roles = getRoleManagementService( authorizationHeader ).getEffectivelyAssignedRoles( "toto" );

            log.info( "toto roles:" + roles );

            assertTrue( roles.contains( new Role( "User Administrator" ) ) );
        }
        finally
        {
            getUserService( authorizationHeader ).deleteUser( "toto" );
            getUserService( authorizationHeader ).removeFromCache( "toto" );
            assertNull( getUserService( authorizationHeader ).getUser( "toto" ) );
        }

    }

    @Test
    public void allRoles()
        throws Exception
    {
        List<Role> roles = getRoleManagementService( authorizationHeader ).getAllRoles();

        log.info( "all roles" );

        for ( Role role : roles )
        {
            log.info( "role:" + role );
        }
    }

    @Test
    public void getRole()
        throws Exception
    {
        Role role = getRoleManagementService( authorizationHeader ).getRole( "User Administrator" );

        log.info( "role:" + role );

    }

    @Test
    public void updateRoleDescription()
        throws Exception
    {
        String name = "User Administrator";
        Role role = getRoleManagementService( authorizationHeader ).getRole( name );
        assertTrue( StringUtils.isEmpty( role.getDescription() ) );

        getRoleManagementService( authorizationHeader ).updateRoleDescription( name, "foo" );

        role = getRoleManagementService( authorizationHeader ).getRole( name );

        assertEquals( "foo", role.getDescription() );

        getRoleManagementService( authorizationHeader ).updateRoleDescription( name, null );

        role = getRoleManagementService( authorizationHeader ).getRole( name );

        assertTrue( StringUtils.isEmpty( role.getDescription() ) );

    }

    @Test
    public void updateRoleUsers()
        throws Exception
    {
        String name = "User Administrator";
        Role role = getRoleManagementService( authorizationHeader ).getRole( name );

        assertEquals( 0, role.getUsers().size() );

        role.setUsers( Arrays.asList( getUserService( authorizationHeader ).getUser( "admin" ) ) );

        getRoleManagementService( authorizationHeader ).updateRoleUsers( role );

        role = getRoleManagementService( authorizationHeader ).getRole( name );

        assertEquals( 1, role.getUsers().size() );

        role.setRemovedUsers( Arrays.asList( getUserService( authorizationHeader ).getUser( "admin" ) ) );
        role.setUsers( Collections.<User>emptyList() );

        getRoleManagementService( authorizationHeader ).updateRoleUsers( role );

        role = getRoleManagementService( authorizationHeader ).getRole( name );

        assertEquals( 0, role.getUsers().size() );

    }

    @Test
    public void applicationRoles()
        throws Exception
    {
        RoleManagementService roleManagementService = getRoleManagementService( authorizationHeader );


        List<Role> allRoles = roleManagementService.getAllRoles();

        assertNotNull( allRoles );

        int initialSize = allRoles.size();

        roleManagementService.createTemplatedRole( "archiva-repository-observer", "internal" );

        allRoles = roleManagementService.getAllRoles();

        assertNotNull( allRoles );

        assertEquals( initialSize + 1, allRoles.size() );

        assertRoleExist( "Repository Observer - internal", allRoles );

        roleManagementService.createTemplatedRole( "archiva-repository-manager", "internal" );

        allRoles = roleManagementService.getAllRoles();

        assertNotNull( allRoles );

        assertEquals( initialSize + 2, allRoles.size() );

        assertRoleExist( "Repository Manager - internal", allRoles );

        roleManagementService.createTemplatedRole( "archiva-repository-observer", "snapshots" );

        allRoles = roleManagementService.getAllRoles();

        assertNotNull( allRoles );

        assertEquals( initialSize + 3, allRoles.size() );

        assertRoleExist( "Repository Observer - snapshots", allRoles );

        roleManagementService.createTemplatedRole( "archiva-repository-manager", "snapshots" );

        allRoles = roleManagementService.getAllRoles();

        assertNotNull( allRoles );

        assertEquals( initialSize + 4, allRoles.size() );

        assertRoleExist( "Repository Manager - snapshots", allRoles );

        List<ApplicationRoles> applicationRoleList = roleManagementService.getApplicationRoles( "guest" );

        assertNotNull( applicationRoleList );

        for ( ApplicationRoles applicationRoles : applicationRoleList )
        {
            log.info( "applicationRoles:" + applicationRoles );
        }
    }

    private void assertRoleExist( String roleName, List<Role> allRoles )
    {
        for ( Role role : allRoles )
        {
            if ( StringUtils.equals( roleName, role.getName() ) )
            {
                return;
            }
        }
        fail( "role " + roleName + " not exists" );
    }
}
