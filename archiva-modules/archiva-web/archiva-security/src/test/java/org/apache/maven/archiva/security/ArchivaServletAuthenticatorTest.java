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

import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager; 

import org.easymock.MockControl;

/**
 * ArchivaServletAuthenticatorTest
 * 
 * @version
 */
public class ArchivaServletAuthenticatorTest
    extends AbstractSecurityTest
{    
    private ServletAuthenticator servletAuth;
    
    private MockControl httpServletRequestControl;
    
    private HttpServletRequest request;
    
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        servletAuth = ( ServletAuthenticator ) lookup( ServletAuthenticator.class, "default" );
        
        httpServletRequestControl = MockControl.createControl( HttpServletRequest.class );
        request = ( HttpServletRequest ) httpServletRequestControl.getMock();
        
        setupRepository( "corporate" );
    }
    
    @Override
    protected String getPlexusConfigLocation()
    {
        return "org/apache/maven/archiva/security/ArchivaServletAuthenticatorTest.xml";
    }
    
    protected void assignRepositoryManagerRole( String principal, String repoId )
        throws Exception
    {
        roleManager.assignTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId, principal );
    }
    
    public void testIsAuthenticatedUserExists()
        throws Exception
    {
        AuthenticationResult result = new AuthenticationResult( true, "user", null );
        boolean isAuthenticated = servletAuth.isAuthenticated( request, result );
        
        assertTrue( isAuthenticated );
    }
    
    public void testIsAuthenticatedUserDoesNotExist()
        throws Exception
    {
        AuthenticationResult result = new AuthenticationResult( false, "non-existing-user", null );
        try
        {
            servletAuth.isAuthenticated( request, result );
            fail( "Authentication exception should have been thrown." );
        }
        catch ( AuthenticationException e )
        {
            assertEquals( "User Credentials Invalid", e.getMessage() );
        }        
    }
    
    public void testIsAuthorizedUserHasWriteAccess()
        throws Exception
    {   
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );
        
        assignRepositoryManagerRole( USER_ALPACA, "corporate" );

        UserManager userManager = securitySystem.getUserManager();
        User user = userManager.findUser( USER_ALPACA );
        
        AuthenticationResult result = new AuthenticationResult( true, USER_ALPACA, null );
        
        SecuritySession session = new DefaultSecuritySession( result, user );
        boolean isAuthorized = servletAuth.isAuthorized( request, session, "corporate", true );
                
        assertTrue( isAuthorized );
    }
    
    public void testIsAuthorizedUserHasNoWriteAccess()
        throws Exception
    {
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );
        
        assignRepositoryObserverRole( USER_ALPACA, "corporate" );
    
        httpServletRequestControl.expectAndReturn( request.getRemoteAddr(), "192.168.111.111" );
        
        UserManager userManager = securitySystem.getUserManager();
        User user = userManager.findUser( USER_ALPACA );
        
        AuthenticationResult result = new AuthenticationResult( true, USER_ALPACA, null );
        
        SecuritySession session = new DefaultSecuritySession( result, user );
        
        httpServletRequestControl.replay();
        
        try
        {
            servletAuth.isAuthorized( request, session, "corporate", true );
            fail( "UnauthorizedException should have been thrown." ); 
        }
        catch ( UnauthorizedException e )
        {
            assertEquals( "Access denied for repository corporate", e.getMessage() );
        }
    
        httpServletRequestControl.verify();
    }
    
    
    public void testIsAuthorizedUserHasReadAccess()
        throws Exception
    { 
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );
        
        assignRepositoryObserverRole( USER_ALPACA, "corporate" );
        
        UserManager userManager = securitySystem.getUserManager();
        User user = userManager.findUser( USER_ALPACA );
        
        AuthenticationResult result = new AuthenticationResult( true, USER_ALPACA, null );
        
        SecuritySession session = new DefaultSecuritySession( result, user );
        boolean isAuthorized = servletAuth.isAuthorized( request, session, "corporate", false );
                
        assertTrue( isAuthorized );        
    }
    
    public void testIsAuthorizedUserHasNoReadAccess()
        throws Exception
    {
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );
        
        UserManager userManager = securitySystem.getUserManager();
        User user = userManager.findUser( USER_ALPACA );
        
        AuthenticationResult result = new AuthenticationResult( true, USER_ALPACA, null );
        
        SecuritySession session = new DefaultSecuritySession( result, user );
        try
        {
            servletAuth.isAuthorized( request, session, "corporate", false );
            fail( "UnauthorizedException should have been thrown." );
        }
        catch ( UnauthorizedException e )
        {
            assertEquals( "Access denied for repository corporate", e.getMessage() );
        }       
    }
    
    public void testIsAuthorizedGuestUserHasWriteAccess()
        throws Exception
    {   
        assignRepositoryManagerRole( USER_GUEST, "corporate" );        
        boolean isAuthorized = servletAuth.isAuthorized( USER_GUEST, "corporate", true );
        
        assertTrue( isAuthorized );
    }
    
    public void testIsAuthorizedGuestUserHasNoWriteAccess()
        throws Exception
    {   
        assignRepositoryObserverRole( USER_GUEST, "corporate" );
        
        boolean isAuthorized = servletAuth.isAuthorized( USER_GUEST, "corporate", true );
        assertFalse( isAuthorized );
    }
    
    public void testIsAuthorizedGuestUserHasReadAccess()
        throws Exception
    {
        assignRepositoryObserverRole( USER_GUEST, "corporate" );
        
        boolean isAuthorized = servletAuth.isAuthorized( USER_GUEST, "corporate", false );
        
        assertTrue( isAuthorized );        
    }
    
    public void testIsAuthorizedGuestUserHasNoReadAccess()
        throws Exception
    {                   
        boolean isAuthorized = servletAuth.isAuthorized( USER_GUEST, "corporate", false );
            
        assertFalse( isAuthorized );
    }
}
