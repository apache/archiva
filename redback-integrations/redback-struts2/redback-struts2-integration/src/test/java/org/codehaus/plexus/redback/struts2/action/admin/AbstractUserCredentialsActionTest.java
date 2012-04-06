package org.codehaus.plexus.redback.struts2.action.admin;

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

import net.sf.ehcache.CacheManager;
import org.apache.struts2.StrutsSpringTestCase;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.RbacObjectInvalidException;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.struts2.action.AbstractUserCredentialsAction;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.redback.users.memory.SimpleUser;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.JUnit4;

import java.util.Collections;

@RunWith( JUnit4.class )
public abstract class AbstractUserCredentialsActionTest
    extends StrutsSpringTestCase
{
    protected static final String PASSWORD = "password1";

    //@Inject
    //@Named( value = "rBACManager#memory" )
    protected RBACManager rbacManager;

    //@Inject
    private RoleManager roleManager;

    //@Inject
    protected SecuritySystem system;

    protected SecuritySession session;

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    @Before
    public void setUp()
        throws Exception
    {
        CacheManager.getInstance().clearAll();
        super.setUp();

        rbacManager = applicationContext.getBean( "rBACManager#memory" , RBACManager.class );
        roleManager = applicationContext.getBean( RoleManager.class );
        system = applicationContext.getBean( SecuritySystem.class );


        roleManager.loadRoleModel( getClass().getResource( "/redback.xml" ) );
        roleManager.createTemplatedRole( "project-administrator", "default" );
        roleManager.createTemplatedRole( "project-administrator", "other" );
        roleManager.createTemplatedRole( "project-grant-only", "default" );

        UserManager userManager = system.getUserManager();

        User user = new SimpleUser();
        user.setUsername( "user" );
        user.setPassword( PASSWORD );
        userManager.addUserUnchecked( user );

        user = new SimpleUser();
        user.setUsername( "user2" );
        user.setPassword( PASSWORD );
        userManager.addUserUnchecked( user );

        user = new SimpleUser();
        user.setUsername( "user3" );
        user.setPassword( PASSWORD );
        userManager.addUserUnchecked( user );

        user = new SimpleUser();
        user.setUsername( "admin" );
        user.setPassword( PASSWORD );
        userManager.addUserUnchecked( user );

        user = new SimpleUser();
        user.setUsername( "user-admin" );
        user.setPassword( PASSWORD );
        userManager.addUserUnchecked( user );

        UserAssignment assignment = rbacManager.createUserAssignment( "admin" );
        assignment.addRoleName( "System Administrator" );
        rbacManager.saveUserAssignment( assignment );

        assignment = rbacManager.createUserAssignment( "user-admin" );
        assignment.addRoleName( "User Administrator" );
        rbacManager.saveUserAssignment( assignment );

        assignment = rbacManager.createUserAssignment( "user2" );
        rbacManager.saveUserAssignment( assignment );
    }

    @After
    public void after()
    {
        CacheManager.getInstance().clearAll();
    }

    protected void addAssignment( String principal, String roleName )
        throws RbacManagerException, RbacObjectInvalidException
    {
        UserAssignment assignment;

        if ( rbacManager.userAssignmentExists( principal ) )
        {
            assignment = rbacManager.getUserAssignment( principal );
        }
        else
        {
            assignment = rbacManager.createUserAssignment( principal );
        }
        assignment.addRoleName( roleName );
        rbacManager.saveUserAssignment( assignment );
    }

    protected void login( AbstractUserCredentialsAction action, String principal, String password )
        throws AuthenticationException, UserNotFoundException, AccountLockedException, MustChangePasswordException
    {
        PasswordBasedAuthenticationDataSource authdatasource = new PasswordBasedAuthenticationDataSource();
        authdatasource.setPrincipal( principal );
        authdatasource.setPassword( password );
        session = system.authenticate( authdatasource );
        assertTrue( session.isAuthenticated() );

        action.setSession( Collections.singletonMap( SecuritySystemConstants.SECURITY_SESSION_KEY, (Object) session ) );
    }

}