package org.apache.archiva.redback.role;

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

import junit.framework.TestCase;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.role.model.ModelPermission;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * AbstractRoleManagerTest:
 * 
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public abstract class AbstractRoleManagerTest
    extends TestCase
{

    protected Logger logger = LoggerFactory.getLogger( getClass() );

    protected RBACManager rbacManager;

    protected RoleManager roleManager;

    public AbstractRoleManagerTest()
    {
        super();
    }

    @Test
    public void testLoading()
        throws Exception
    {

        List<Resource> all = rbacManager.getAllResources();


        assertTrue( rbacManager.resourceExists( "*" ) );
        assertTrue( rbacManager.operationExists( "Test Operation" ) );
        assertTrue( rbacManager.roleExists( "Test Role" ) );
        assertTrue( rbacManager.roleExists( "Test Role 1" ) );
        assertTrue( rbacManager.roleExists( "Test Role 2" ) );

        assertTrue( rbacManager.roleExists( "Role for cornflake eaters drinking milk in the bowl" ) );

        roleManager.createTemplatedRole( "test-template-2", "foo" );

        assertTrue( roleManager.templatedRoleExists( "test-template-2", "foo" ) );
        assertTrue( roleManager.templatedRoleExists( "test-template", "foo" ) );

        roleManager.updateRole( "test-template-2", "foo", "bar" );

        assertFalse( roleManager.templatedRoleExists( "test-template-2", "foo" ) );
        // TODO: bug - assertFalse( roleManager.templatedRoleExists( "test-template", "foo" ) );
        assertTrue( roleManager.templatedRoleExists( "test-template-2", "bar" ) );
        assertTrue( roleManager.templatedRoleExists( "test-template", "bar" ) );

        roleManager.createTemplatedRole( "test-template-2", "hot" );

        assertTrue( roleManager.templatedRoleExists( "test-template-2", "hot" ) );
    }

    @Test
    public void testUserAssignmentUpdate()
        throws Exception
    {
        String principal = "joe";

        roleManager.assignRole( "test-role", principal );
        roleManager.assignRoleByName( "Test Role 1", principal );
        roleManager.createTemplatedRole( "test-template-2", "cold" );
        roleManager.assignTemplatedRole( "test-template-2", "cold", principal );

        roleManager.updateRole( "test-template-2", "cold", "frigid" );

        assertTrue( roleManager.templatedRoleExists( "test-template-2", "frigid" ) );

        UserAssignment assignment = rbacManager.getUserAssignment( principal );

        List<String> assignments = assignment.getRoleNames();

        assertEquals( 3, assignments.size() );

        for ( String roleName : assignments )
        {
            logger.info( roleName );
            assertTrue( "Test Role".equals( roleName ) || "Foo 2 - frigid".equals( roleName ) || "Test Role 1".equals( roleName ) );
        }
    }

    @Test
    public void testVerifyTemplatedRole()
        throws Exception
    {
        roleManager.createTemplatedRole( "test-template-2", "first" );

        assertTrue( roleManager.templatedRoleExists( "test-template-2", "first" ) );
        Role role = rbacManager.getRole( "Foo 2 - first" );
        assertNotNull( role );
        assertTrue( hasPermissionOnOperation( role, "Eat Cornflakes", "first" ) );
        assertFalse( hasPermissionOnOperation( role, "Drink Milk" ) );
     
        ModelTemplate template = RoleModelUtils.getModelTemplate( roleManager.getModel(), "test-template-2" );
        ModelPermission p = new ModelPermission();
        p.setId( "new-permission" );
        p.setName( "New Permission" );
        p.setOperation( "drink-milk" );
        p.setResource( "${resource}" );
        template.addPermission( p );
        p = new ModelPermission();
        p.setId( "eat-cornflakes-permission-3" );
        p.setName( "Eat 3 Cornflakes" );
        p.setOperation( "eat-cornflakes" );
        p.setResource( "${resource}" );
        template.removePermission( p );
        
        roleManager.verifyTemplatedRole( "test-template-2", "first" );
        
        assertTrue( roleManager.templatedRoleExists( "test-template-2", "first" ) );
        role = rbacManager.getRole( "Foo 2 - first" );
        assertNotNull( role );
        assertFalse( hasPermissionOnOperation( role, "Eat Cornflakes", "first" ) );
        assertTrue( hasPermissionOnOperation( role, "Drink Milk", "first" ) );
    }

    private boolean hasPermissionOnOperation( Role role, String operation )
    {
        for ( Permission p : role.getPermissions() )
        {
            if ( p.getOperation().getName().equals( operation ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermissionOnOperation( Role role, String operation, String resource )
    {
        for ( Permission p : role.getPermissions() )
        {
            if ( p.getOperation().getName().equals( operation ) && p.getResource().getIdentifier().equals( resource ) )
            {
                return true;
            }
        }
        return false;
    }

    public void setRbacManager( RBACManager rbacManager )
    {
        this.rbacManager = rbacManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }
}