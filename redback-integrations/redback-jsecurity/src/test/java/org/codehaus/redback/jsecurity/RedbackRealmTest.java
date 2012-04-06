package org.codehaus.redback.jsecurity;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.rbac.Operation;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.jsecurity.authc.IncorrectCredentialsException;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.mgt.DefaultSecurityManager;
import org.jsecurity.subject.PrincipalCollection;
import org.jsecurity.subject.SimplePrincipalCollection;
import org.jsecurity.subject.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;


@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RedbackRealmTest
    extends TestCase
{
    private DefaultSecurityManager securityManager;

    private RedbackRealm realm;

    @Inject
    @Named( value = "userManager#memory" )
    private UserManager userManager;

    @Inject
    @Named( value = "rBACManager#memory" )
    private RBACManager rbacManager;

    @Inject
    private UserSecurityPolicy userSecurityPolicy;

    private User user;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        securityManager = new DefaultSecurityManager();



        realm = new RedbackRealm( userManager, rbacManager, userSecurityPolicy );
        securityManager.setRealm( realm );

        user = userManager.createUser( "test1", "John Tester", "jtester@redback.codehaus.org" );
        user.setPassword( "password1" );
        userManager.addUser( user );
        userManager.updateUser( user );
    }

    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        securityManager.destroy();
        securityManager = null;
        realm = null;
    }

    protected String getPlexusConfigLocation()
    {
        return "plexus.xml";
    }

    public void testThrowsExceptionIfUserAccountLocked()
        throws Exception
    {
        user.setLocked( true );
        userManager.updateUser( user );
        try
        {
            securityManager.login( new UsernamePasswordToken( "test1", "password1" ) );
            fail( "Should not be able to login" );
        }
        catch ( PrincipalLockedException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testThrowsExceptionIfUserAccountNeedsPasswordChange()
        throws Exception
    {
        user.setPasswordChangeRequired( true );
        userManager.updateUser( user );
        try
        {
            securityManager.login( new UsernamePasswordToken( "test1", "password1" ) );
            fail( "Should not be able to login" );
        }
        catch ( PrincipalPasswordChangeRequiredException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testUnsuccessfullAuthAttemptsLockAccount()
        throws Exception
    {
        assertFalse( user.isLocked() );
        userSecurityPolicy.setLoginAttemptCount( 2 );
        try
        {
            securityManager.login( new UsernamePasswordToken( "test1", "incorrectpassowrd" ) );
            fail( "password should be incorrect" );
        }
        catch ( IncorrectCredentialsException e )
        {
            assertFalse( user.isLocked() );
        }

        try
        {
            securityManager.login( new UsernamePasswordToken( "test1", "incorrectpassowrd" ) );
            fail( "password should be incorrect" );
        }
        catch ( IncorrectCredentialsException e )
        {
            assertTrue( user.isLocked() );
        }
    }

    @Test
    public void testBasic()
        throws Exception
    {
        assertEquals( 1, userManager.getUsers().size() );

        Role role1 = rbacManager.createRole( "role1" );
        Permission permission = rbacManager.createPermission( "Allowed to write to repository" );
        Operation operation = rbacManager.createOperation( "myop" );
        Resource resource = rbacManager.createResource( "filesystem" );

        permission.setOperation( operation );
        permission.setPermanent( false );
        permission.setResource( resource );

        role1.addPermission( permission );
        rbacManager.savePermission( permission );
        rbacManager.saveRole( role1 );

        Role role2 = rbacManager.createRole( "role2" );

        UserAssignment assignment = rbacManager.createUserAssignment( user.getUsername() );
        assignment.addRoleName( "role1" );
        rbacManager.saveUserAssignment( assignment );

        Subject subject = securityManager.login( new UsernamePasswordToken( "test1", "password1" ) );
        assertTrue( subject.isAuthenticated() );
        assertTrue( subject.hasRole( "role1" ) );
        assertFalse( subject.hasRole( "role2" ) );

        PrincipalCollection principals = new SimplePrincipalCollection( "test1", realm.getName() );

        assertTrue( securityManager.isPermitted( principals, "Allowed to write to repository" ) );
    }
}
