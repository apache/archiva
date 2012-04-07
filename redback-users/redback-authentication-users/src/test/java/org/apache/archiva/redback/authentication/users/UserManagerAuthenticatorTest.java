package org.apache.archiva.redback.authentication.users;

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
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;
import java.util.Date;

/**
 * Tests for {@link org.apache.archiva.redback.authentication.users.UserManagerAuthenticator} implementation.
 *
 * @author <a href='mailto:rahul.thakur.xdev@gmail.com'>Rahul Thakur</a>
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class UserManagerAuthenticatorTest
    extends TestCase
{
    @Inject
    private UserSecurityPolicy userSecurityPolicy;

    @Inject
    @Named(value = "authenticator#user-manager")
    Authenticator component;

    @Inject
    @Named(value = "userManager#memory")
    UserManager um;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        userSecurityPolicy.setEnabled( false );
    }

    @Test
    public void testLookup()
        throws Exception
    {
        assertNotNull( component );
        assertEquals( UserManagerAuthenticator.class.getName(), component.getClass().getName() );
    }

    @Test
    public void testAuthenticate()
        throws Exception
    {
        // Set up a few users for the Authenticator

        User user = um.createUser( "test", "Test User", "testuser@somedomain.com" );
        user.setPassword( "testpass" );
        um.addUser( user );

        user = um.createUser( "guest", "Guest User", "testuser@somedomain.com" );
        user.setPassword( "guestpass" );
        um.addUser( user );

        user = um.createUser( "anonymous", "Anonymous User", "testuser@somedomain.com" );
        user.setPassword( "nopass" );
        um.addUser( user );

        // test with valid credentials
        Authenticator auth = component;
        assertNotNull( auth );

        AuthenticationResult result = auth.authenticate( createAuthDataSource( "anonymous", "nopass" ) );
        assertTrue( result.isAuthenticated() );

        // test with invalid password
        result = auth.authenticate( createAuthDataSource( "anonymous", "wrongpass" ) );
        assertFalse( result.isAuthenticated() );
        assertNull( result.getException() );

        // test with unknown user
        result = auth.authenticate( createAuthDataSource( "unknownuser", "wrongpass" ) );
        assertFalse( result.isAuthenticated() );
        assertNotNull( result.getException() );
        assertEquals( result.getException().getClass().getName(), UserNotFoundException.class.getName() );
    }

    @Test
    public void testAuthenticateLockedPassword()
        throws AuthenticationException, MustChangePasswordException, UserNotFoundException
    {
        userSecurityPolicy.setEnabled( true );

        // Set up a user for the Authenticator
        User user = um.createUser( "testuser", "Test User Locked Password", "testuser@somedomain.com" );
        user.setPassword( "correctpass1" );
        user.setValidated( true );
        user.setPasswordChangeRequired( false );
        um.addUser( user );

        Authenticator auth = component;
        assertNotNull( auth );

        boolean hasException = false;
        AuthenticationResult result = null;

        try
        {
            // test password lock
            for ( int i = 0; i < 11; i++ )
            {
                result = auth.authenticate( createAuthDataSource( "testuser", "wrongpass" ) );
            }
        }
        catch ( AccountLockedException e )
        {
            hasException = true;
        }
        finally
        {
            assertNotNull( result );
            assertFalse( result.isAuthenticated() );
            assertTrue( hasException );
        }
    }

    @Test
    public void testAuthenticateExpiredPassword()
        throws AuthenticationException, AccountLockedException, UserNotFoundException
    {
        userSecurityPolicy.setEnabled( true );
        userSecurityPolicy.setPasswordExpirationDays( 15 );

        // Set up a user for the Authenticator
        User user = um.createUser( "testuser", "Test User Expired Password", "testuser@somedomain.com" );
        user.setPassword( "expiredpass1" );
        user.setValidated( true );
        user.setPasswordChangeRequired( false );
        um.addUser( user );

        Authenticator auth = component;
        assertNotNull( auth );

        boolean hasException = false;

        try
        {
            // test successful authentication
            AuthenticationResult result = auth.authenticate( createAuthDataSource( "testuser", "expiredpass1" ) );
            assertTrue( result.isAuthenticated() );

            // test expired password
            user = um.findUser( "testuser" );

            Calendar currentDate = Calendar.getInstance();
            currentDate.set( Calendar.YEAR, currentDate.get( Calendar.YEAR ) - 1 );
            Date lastPasswordChange = currentDate.getTime();
            user.setLastPasswordChange( lastPasswordChange );

            um.updateUser( user );

            auth.authenticate( createAuthDataSource( "testuser", "expiredpass1" ) );
        }
        catch ( MustChangePasswordException e )
        {
            hasException = true;
        }
        finally
        {
            assertTrue( hasException );
        }
    }

    private PasswordBasedAuthenticationDataSource createAuthDataSource( String username, String password )
    {
        PasswordBasedAuthenticationDataSource source = new PasswordBasedAuthenticationDataSource();

        source.setPrincipal( username );
        source.setPassword( password );

        return source;

    }
}
