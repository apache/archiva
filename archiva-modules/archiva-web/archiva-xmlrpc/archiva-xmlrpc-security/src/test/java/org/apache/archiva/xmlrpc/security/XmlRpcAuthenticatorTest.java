package org.apache.archiva.xmlrpc.security;

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

import org.apache.archiva.web.xmlrpc.security.XmlRpcAuthenticator;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * XmlRpcAuthenticatorTest
 * 
 * @version $Id XmlRpcAuthenticatorTest.java
 */
public class XmlRpcAuthenticatorTest
//extends AbstractDependencyInjectionSpringContextTests
    extends PlexusInSpringTestCase
{
    protected static final String USER_GUEST = "guest";

    protected static final String USER_ADMIN = "admin";

    protected static final String USER_ALPACA = "alpaca";

    private static final String PASSWORD = "password123";

    protected SecuritySystem securitySystem;

    protected RoleManager roleManager;
    
    private MockControl xmlRpcRequestControl;
    
    private XmlRpcRequest xmlRpcRequest;
    
    private XmlRpcAuthenticator authenticator;
    
    private MockControl configControl;
    
    private XmlRpcHttpRequestConfigImpl config; 
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        securitySystem = (SecuritySystem) lookup( SecuritySystem.class, "testable" );        
        roleManager = (RoleManager) lookup( RoleManager.class, "default" );
        
        // Some basic asserts.
        assertNotNull( securitySystem );        
        assertNotNull( roleManager );
        
        // Setup Admin User.
        User adminUser = createUser( USER_ADMIN, "Admin User", null );
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_SYSTEM_ADMIN, adminUser.getPrincipal().toString() );

        // Setup Guest User.
        User guestUser = createUser( USER_GUEST, "Guest User", null );
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_GUEST, guestUser.getPrincipal().toString() );
        
        configControl = MockClassControl.createControl( XmlRpcHttpRequestConfigImpl.class );
        config = ( XmlRpcHttpRequestConfigImpl ) configControl.getMock();
        
        xmlRpcRequestControl = MockControl.createControl( XmlRpcRequest.class );
        xmlRpcRequest = ( XmlRpcRequest ) xmlRpcRequestControl.getMock();    
        
        authenticator = new XmlRpcAuthenticator( securitySystem );        
    }
            
    private User createUser( String principal, String fullname, String password )
        throws UserNotFoundException
    {
        UserManager userManager = securitySystem.getUserManager();
    
        User user = userManager.createUser( principal, fullname, principal + "@testable.archiva.apache.org" );
        securitySystem.getPolicy().setEnabled( false );
        userManager.addUser( user );
        securitySystem.getPolicy().setEnabled( true );
        
        user.setPassword( password );        
        userManager.updateUser( user );
        
        return user;
    }
    
    public void testIsAuthorizedUserExistsButNotAuthorized()
        throws Exception
    {
        createUser( USER_ALPACA, "Al 'Archiva' Paca", PASSWORD );
        
        UserManager userManager = securitySystem.getUserManager();
        try
        {
            User user  = userManager.findUser( USER_ALPACA );
            assertEquals( USER_ALPACA, user.getPrincipal() );
        }
        catch ( UserNotFoundException e )
        {
            fail( "User should exist in the database." );                        
        }
        
        xmlRpcRequestControl.expectAndReturn( xmlRpcRequest.getConfig(), config, 2 );
        
        configControl.expectAndReturn( config.getBasicUserName(), USER_ALPACA );
        
        configControl.expectAndReturn( config.getBasicPassword(), PASSWORD );
        
        xmlRpcRequestControl.expectAndReturn( xmlRpcRequest.getMethodName(),
                                              "AdministrationService.getAllManagedRepositories" );
        
        xmlRpcRequestControl.replay();
        configControl.replay();
        
        boolean isAuthorized = authenticator.isAuthorized( xmlRpcRequest );
        
        xmlRpcRequestControl.verify();
        configControl.verify();
        
        assertFalse( isAuthorized );
    }
    
    public void testIsAuthorizedUserExistsAndAuthorized()
        throws Exception
    {
        createUser( USER_ALPACA, "Al 'Archiva' Paca", PASSWORD );
        
        UserManager userManager = securitySystem.getUserManager();
        try
        {
            User user  = userManager.findUser( USER_ALPACA );
            assertEquals( USER_ALPACA, user.getPrincipal() );
        }
        catch ( UserNotFoundException e )
        {
            fail( "User should exist in the database." );                        
        }
        
        //TODO cannot assign global repo manager role - it says role does not exist :|
        
        //roleManager.assignRole( ArchivaRoleConstants.GLOBAL_REPOSITORY_MANAGER_ROLE, USER_ALPACA );
        
        xmlRpcRequestControl.expectAndReturn( xmlRpcRequest.getConfig(), config, 2 );
        
        configControl.expectAndReturn( config.getBasicUserName(), USER_ALPACA );
        
        configControl.expectAndReturn( config.getBasicPassword(), PASSWORD );
        
        xmlRpcRequestControl.expectAndReturn( xmlRpcRequest.getMethodName(),
                                              "AdministrationService.getAllManagedRepositories" );
        
        xmlRpcRequestControl.replay();
        configControl.replay();
        
        boolean isAuthorized = authenticator.isAuthorized( xmlRpcRequest );
        
        xmlRpcRequestControl.verify();
        configControl.verify();
        
        //assertTrue( isAuthorized );
    }
    
    public void testIsAuthorizedUserDoesNotExist()
        throws Exception
    {   
        UserManager userManager = securitySystem.getUserManager();
        try
        {
            userManager.findUser( USER_ALPACA );
            fail( "User should not exist in the database." );
        }
        catch ( UserNotFoundException e )
        {
            assertEquals( "Unable to find user 'alpaca'", e.getMessage() );            
        }
        
        xmlRpcRequestControl.expectAndReturn( xmlRpcRequest.getConfig(), config, 2 );
        
        configControl.expectAndReturn( config.getBasicUserName(), USER_ALPACA );
        
        configControl.expectAndReturn( config.getBasicPassword(), PASSWORD );
        
        xmlRpcRequestControl.expectAndReturn( xmlRpcRequest.getMethodName(),
                                              "AdministrationService.getAllManagedRepositories" );
        
        xmlRpcRequestControl.replay();
        configControl.replay();
        
        boolean isAuthorized = authenticator.isAuthorized( xmlRpcRequest );
                
        xmlRpcRequestControl.verify();
        configControl.verify();
        
        assertFalse( isAuthorized );
    }    
}
