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
import net.sf.ehcache.CacheManager;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * ArchivaServletAuthenticatorTest
 */
public class ArchivaServletAuthenticatorTest
    extends AbstractSecurityTest
{
    @Inject
    @Named( value = "servletAuthenticator#test" )
    private ServletAuthenticator servletAuth;

    private MockControl httpServletRequestControl;

    private HttpServletRequest request;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        httpServletRequestControl = MockControl.createControl( HttpServletRequest.class );
        request = (HttpServletRequest) httpServletRequestControl.getMock();

        setupRepository( "corporate" );
    }

    protected String getPlexusConfigLocation()
    {
        return "org/apache/maven/archiva/security/ArchivaServletAuthenticatorTest.xml";
    }

    protected void assignRepositoryManagerRole( String principal, String repoId )
        throws Exception
    {
        roleManager.assignTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId, principal );
    }

    @Test
    public void testIsAuthenticatedUserExists()
        throws Exception
    {
        AuthenticationResult result = new AuthenticationResult( true, "user", null );
        boolean isAuthenticated = servletAuth.isAuthenticated( request, result );

        assertTrue( isAuthenticated );
    }

    @Test
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

    @Test
    public void testIsAuthorizedUserHasWriteAccess()
        throws Exception
    {
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );

        assignRepositoryManagerRole( USER_ALPACA, "corporate" );

        UserManager userManager = securitySystem.getUserManager();
        User user = userManager.findUser( USER_ALPACA );

        AuthenticationResult result = new AuthenticationResult( true, USER_ALPACA, null );

        SecuritySession session = new DefaultSecuritySession( result, user );
        boolean isAuthorized =
            servletAuth.isAuthorized( request, session, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );

        assertTrue( isAuthorized );

        restoreGuestInitialValues( USER_ALPACA );
    }

    @Test
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
            servletAuth.isAuthorized( request, session, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );
            fail( "UnauthorizedException should have been thrown." );
        }
        catch ( UnauthorizedException e )
        {
            assertEquals( "Access denied for repository corporate", e.getMessage() );
        }

        httpServletRequestControl.verify();

        restoreGuestInitialValues( USER_ALPACA );
    }

    @Test
    public void testIsAuthorizedUserHasReadAccess()
        throws Exception
    {
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );

        assignRepositoryObserverRole( USER_ALPACA, "corporate" );

        UserManager userManager = securitySystem.getUserManager();
        User user = userManager.findUser( USER_ALPACA );

        AuthenticationResult result = new AuthenticationResult( true, USER_ALPACA, null );

        SecuritySession session = new DefaultSecuritySession( result, user );
        boolean isAuthorized =
            servletAuth.isAuthorized( request, session, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );

        assertTrue( isAuthorized );

        restoreGuestInitialValues( USER_ALPACA );
    }

    @Test
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
            servletAuth.isAuthorized( request, session, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );
            fail( "UnauthorizedException should have been thrown." );
        }
        catch ( UnauthorizedException e )
        {
            assertEquals( "Access denied for repository corporate", e.getMessage() );
        }

        restoreGuestInitialValues( USER_ALPACA );
    }

    @Test
    public void testIsAuthorizedGuestUserHasWriteAccess()
        throws Exception
    {
        assignRepositoryManagerRole( USER_GUEST, "corporate" );
        boolean isAuthorized =
            servletAuth.isAuthorized( USER_GUEST, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );

        assertTrue( isAuthorized );

        // cleanup previously add karma
        restoreGuestInitialValues(USER_GUEST);

    }

    @Test
    public void testIsAuthorizedGuestUserHasNoWriteAccess()
        throws Exception
    {
        assignRepositoryObserverRole( USER_GUEST, "corporate" );

        boolean isAuthorized =
            servletAuth.isAuthorized( USER_GUEST, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );
        assertFalse( isAuthorized );

        // cleanup previously add karma
        restoreGuestInitialValues(USER_GUEST);

    }

    @Test
    public void testIsAuthorizedGuestUserHasReadAccess()
        throws Exception
    {
        assignRepositoryObserverRole( USER_GUEST, "corporate" );

        boolean isAuthorized =
            servletAuth.isAuthorized( USER_GUEST, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );

        assertTrue( isAuthorized );

        // cleanup previously add karma
        restoreGuestInitialValues(USER_GUEST);
    }

    @Test
    public void testIsAuthorizedGuestUserHasNoReadAccess()
        throws Exception
    {
        boolean isAuthorized =
            servletAuth.isAuthorized( USER_GUEST, "corporate", ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );

        assertFalse( isAuthorized );
    }

}
