package org.apache.archiva.redback.users.provider.test;

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
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.UserQuery;
import org.apache.archiva.redback.users.PermanentUserException;
import org.apache.archiva.redback.users.UserManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.List;
import org.springframework.test.annotation.DirtiesContext;

/**
 * AbstractUserManagerTestCase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath*:/spring-context.xml"} )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public class AbstractUserManagerTestCase
    extends TestCase
{
    /**
     * This value is set by the sub classes of this test case.
     * They should override .setUp() and inject this value via
     * the {@link #setUserManager(UserManager)} method call.
     */
    private UserManager userManager;

    @Inject
    private UserSecurityPolicy securityPolicy;
    
    private UserManagerEventTracker eventTracker;

    
    private static final int EVENTTRACKERCOUNT = 2;
    // assertCleanUserManager
    // getEventTracker().userManagerInit( true ); is 1
    // userManager.getUsers() is 2
    // only on clear context
    
    public UserManager getUserManager()
    {
        return userManager;
    }

    public void setUserManager( UserManager um )
    {
        this.userManager = um;
        if ( this.userManager != null )
        {
            this.eventTracker = new UserManagerEventTracker();
            this.userManager.addUserManagerListener( this.eventTracker );
        }
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    protected void assertCleanUserManager()
    {

        getUserManager().eraseDatabase();
        getEventTracker().userManagerInit( true );
        assertNotNull( getUserManager() );

        assertEquals( "New UserManager should contain no users. " + userManager.getUsers(), 0, userManager.getUsers().size() );
    }

    @Test
    public void testFindUserByNullPrincipal()
    {
        try
        {
            Object obj = null;
            getUserManager().findUser( obj );
            fail( "findUser() with null Object Should have thrown a UserNotFoundException." );
        }
        catch ( UserNotFoundException e )
        {
            // Expected Path.
        }
    }

    @Test
    public void testFindUserByEmptyUsername()
    {
        try
        {
            String username = null;
            getUserManager().findUser( username );
            fail( "findUser() with null username Should have thrown a UserNotFoundException." );
        }
        catch ( UserNotFoundException e )
        {
            // Expected Path.
        }
        
        try
        {
            String username = "";
            getUserManager().findUser( username );
            fail( "findUser() with empty username Should have thrown a UserNotFoundException." );
        }
        catch ( UserNotFoundException e )
        {
            // Expected Path.
        }
        
        try
        {
            String username = "   ";
            getUserManager().findUser( username );
            fail( "findUser() with all whitespace username Should have thrown a UserNotFoundException." );
        }
        catch ( UserNotFoundException e )
        {
            // Expected Path.
        }
    }

    @Test
    public void testAddFindUserByPrincipal()
        throws UserNotFoundException
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        User smcqueen = getUserManager().createUser( "smcqueen", "Steve McQueen", "the cooler king" );

        /* Keep a reference to the object that was added.
         * Since it has the actual principal that was managed by jpox/jdo.
         */
        User added = userManager.addUser( smcqueen );

        assertEquals( 1, userManager.getUsers().size() );

        /* Fetch user from userManager using principal returned earlier */
        User actual = userManager.findUser( added.getPrincipal() );
        assertEquals( added, actual );
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testAddFindUserLockedStatus() throws UserNotFoundException {
		assertCleanUserManager();
		securityPolicy.setEnabled(false);

		User smcqueen = getUserManager().createUser("smcqueen", "Steve McQueen", "the cooler king");

		smcqueen.setLocked( true );
		
		/*
		 * Keep a reference to the object that was added. Since it has the
		 * actual principal that was managed by jpox/jdo.
		 */
		User added = userManager.addUser( smcqueen );

		assertTrue( added.isLocked() );
		
		assertEquals(1, userManager.getUsers().size());

		/* Fetch user from userManager using principal returned earlier */
		User actual = userManager.findUser(added.getPrincipal());
		assertEquals(added, actual);

		assertTrue( actual.isLocked() );
		
		/* Check into the event tracker. */
		assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit);
		assertNotNull(getEventTracker().lastDbFreshness);
		assertTrue(getEventTracker().lastDbFreshness.booleanValue());

		assertEquals(1, getEventTracker().addedUsernames.size());
		assertEquals(0, getEventTracker().removedUsernames.size());
		assertEquals(0, getEventTracker().updatedUsernames.size());
	}

    @Test
    public void testAddFindUserByUsername()
        throws UserNotFoundException
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        User smcqueen = getUserManager().createUser( "smcqueen", "Steve McQueen", "the cooler king" );

        User added = userManager.addUser( smcqueen );

        assertEquals( 1, userManager.getUsers().size() );

        User actual = userManager.findUser( "smcqueen" );
        assertEquals( added, actual );
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testCreateUser()
        throws Exception
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();
        User user = um.createUser( "admin", "Administrator", "admin@somedomain.com" );
        user.setPassword( "adminpass" );
        um.addUser( user );

        assertEquals( 1, um.getUsers().size() );
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testAddUser()
        throws Exception
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();
        assertNotNull( um.getUsers() );
        assertEquals( 0, um.getUsers().size() );

        User user = um.createUser( "tommy123", "Tommy Traddles", "tommy.traddles@somedomain.com" );
        user.setPassword( "hillybilly" );
        um.addUser( user );

        assertNotNull( um.getUsers() );
        assertEquals( 1, um.getUsers().size() );
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testDeleteUser()
        throws Exception
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();
        User user = um.createUser( "admin", "Administrator", "admin@somedomain.com" );
        user.setPassword( "adminpass" );
        um.addUser( user );

        assertEquals( 1, um.getUsers().size() );

        um.deleteUser( user.getPrincipal() );
        assertEquals( 0, um.getUsers().size() );

        // attempt finding a non-existent user
        try
        {
            um.findUser( "admin" );
            fail( "Expected UserNotFoundException!" );
        }
        catch ( UserNotFoundException e )
        {
            // do nothing, expected!
        }
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 1, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testFindUser()
        throws Exception
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();

        // create and add a few users
        User u1 = um.createUser( "admin", "Administrator", "admin@somedomain.com" );
        u1.setPassword( "adminpass" );
        um.addUser( u1 );

        u1 = um.createUser( "administrator", "Administrator User", "administrator@somedomain.com" );
        u1.setPassword( "password" );
        um.addUser( u1 );

        u1 = um.createUser( "root", "Root User", "root@somedomain.com" );
        u1.setPassword( "rootpass" );
        um.addUser( u1 );

        assertEquals( 3, um.getUsers().size() );

        // find an existing user
        User user = um.findUser( "root" );
        assertNotNull( user );
        assertEquals( "root@somedomain.com", user.getEmail() );
        assertEquals( "root", user.getPrincipal() );
        assertEquals( "Root User", user.getFullName() );
        // test if the plain string password is encoded and NULL'ified
        assertNull( user.getPassword() );
        // test if encoded password was as expected 
        assertTrue( securityPolicy.getPasswordEncoder().isPasswordValid( user.getEncodedPassword(), "rootpass" ) );

        // attempt finding a non-existent user
        try
        {
            um.findUser( "non-existent" );
            fail( "Expected UserNotFoundException!" );
        }
        catch ( UserNotFoundException e )
        {
            // do nothing, expected!
        }
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 3, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testFindUsersByQuery()
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();

        // create and add a few users
        User u1 = um.createUser( "admin", "Administrator", "admin@somedomain.com" );
        u1.setPassword( "adminpass" );
        um.addUser( u1 );

        u1 = um.createUser( "administrator", "Administrator User", "administrator@somedomain.com" );
        u1.setPassword( "password" );
        um.addUser( u1 );

        u1 = um.createUser( "root", "Root User", "root@somedomain.com" );
        u1.setPassword( "rootpass" );
        um.addUser( u1 );

        assertEquals( 3, um.getUsers().size() );

        // Query by username
        UserQuery query = um.createUserQuery();
        query.setUsername( "Admin" );
        assertEquals( 2, um.findUsersByQuery( query ).size() );

        // Query by full name
        query = um.createUserQuery();
        query.setFullName( "Admin" );
        assertEquals( 2, um.findUsersByQuery( query ).size() );

        query = um.createUserQuery();
        query.setFullName( "Administrator" );
        assertEquals( 2, um.findUsersByQuery( query ).size() );

        query = um.createUserQuery();
        query.setFullName( "r User" );
        assertEquals( 1, um.findUsersByQuery( query ).size() );

        // Query by user name
        query = um.createUserQuery();
        query.setEmail( "somedomain" );
        assertEquals( 3, um.findUsersByQuery( query ).size() );

        // Query by email
        query = um.createUserQuery();
        query.setEmail( "root@" );
        assertEquals( 1, um.findUsersByQuery( query ).size() );

        // Query by illegal property
        query = um.createUserQuery();
        try
        {
            query.setOrderBy( "unknownString" );
            fail( "Expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {

        }

        // Query with default ordering ascending
        query = um.createUserQuery();
        query.setOrderBy( UserQuery.ORDER_BY_EMAIL );
        List<User> users = um.findUsersByQuery( query );
        assertEquals( 3, users.size() );
        assertEquals( "admin@somedomain.com", ( (User) users.get( 0 ) ).getEmail() );

        // Query with ordering ascending
        query = um.createUserQuery();
        query.setOrderBy( UserQuery.ORDER_BY_EMAIL );
        query.setAscending( false );
        users = um.findUsersByQuery( query );
        assertEquals( 3, users.size() );
        assertEquals( "root@somedomain.com", ( (User) users.get( 0 ) ).getEmail() );

        // Query with ordering descending, max 2 results
        query = um.createUserQuery();
        query.setOrderBy( UserQuery.ORDER_BY_EMAIL );
        query.setAscending( false );
        query.setMaxResults( 2 );
        users = um.findUsersByQuery( query );
        assertEquals( 2, users.size() );
        assertEquals( "root@somedomain.com", ( (User) users.get( 0 ) ).getEmail() );

        // Query with ordering ascending, max 2 results, first result = 2 so only one result
        query = um.createUserQuery();
        query.setOrderBy( UserQuery.ORDER_BY_EMAIL );
        query.setAscending( false );
        query.setMaxResults( 2 );
        query.setFirstResult( 2 );
        users = um.findUsersByQuery( query );
        assertEquals( 1, users.size() );
        assertEquals( "admin@somedomain.com", ( (User) users.get( 0 ) ).getEmail() );

        // Query on more than one field
        query = um.createUserQuery();
        query.setOrderBy( UserQuery.ORDER_BY_EMAIL );
        query.setFullName( "admin" );
        query.setEmail( "admin@" );
        users = um.findUsersByQuery( query );
        assertEquals( 1, users.size() );
        assertEquals( "admin@somedomain.com", ( (User) users.get( 0 ) ).getEmail() );
    }

    @Test
    public void testUserExists()
        throws Exception
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();

        // create and add a few users
        User u1 = um.createUser( "admin", "Administrator", "admin@somedomain.com" );
        u1.setPassword( "adminpass" );
        um.addUser( u1 );

        assertTrue( um.userExists( "admin" ) );
        assertFalse( um.userExists( "voodoohatrack" ) );

        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testUpdateUser()
        throws Exception
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();

        // create and add a user
        User u1 = um.createUser( "root", "Root User", "root@somedomain.com" );
        u1.setPassword( "rootpass" );
        u1 = um.addUser( u1 );

        // find user
        User user = um.findUser( "root" );
        assertNotNull( user );
        assertEquals( u1, user );

        user.setEmail( "superuser@somedomain.com" );
        user.setPassword( "superpass" );
        user.setFullName( "Super User" );

        um.updateUser( user );

        // find updated user
        user = um.findUser( "root" );
        assertNotNull( user );
        assertEquals( "superuser@somedomain.com", user.getEmail() );
        assertEquals( "Super User", user.getFullName() );
        assertTrue( securityPolicy.getPasswordEncoder().isPasswordValid( user.getEncodedPassword(), "superpass" ) );
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 1, getEventTracker().updatedUsernames.size() );
    }

    @Test
    public void testDeletePermanentUser()
        throws UserNotFoundException
    {
        assertCleanUserManager();
        securityPolicy.setEnabled( false );

        UserManager um = getUserManager();
        User user = um.createUser( "admin", "Administrator", "admin@somedomain.com" );
        user.setPassword( "adminpass" );
        user.setPermanent( true );
        user = um.addUser( user );

        assertEquals( 1, um.getUsers().size() );

        try
        {
            um.deleteUser( user.getPrincipal() );
            fail("Deleting permanent user shold throw PermanentUserException.");
        } catch( PermanentUserException e )
        {
            // do nothing, expected route.
        }
        
        assertEquals( 1, um.getUsers().size() );

        // attempt to finding user
        User admin = um.findUser( "admin" );
        assertNotNull( admin );
        assertEquals( user.getEmail(), admin.getEmail() );
        assertEquals( user.getFullName(), admin.getFullName() );
        
        /* Check into the event tracker. */
        assertEquals( EVENTTRACKERCOUNT, getEventTracker().countInit );
        assertNotNull( getEventTracker().lastDbFreshness );
        assertTrue( getEventTracker().lastDbFreshness.booleanValue() );
        
        assertEquals( 1, getEventTracker().addedUsernames.size() );
        assertEquals( 0, getEventTracker().removedUsernames.size() );
        assertEquals( 0, getEventTracker().updatedUsernames.size() );    }

    public UserManagerEventTracker getEventTracker()
    {
        return eventTracker;
    }

    public void setEventTracker( UserManagerEventTracker eventTracker )
    {
        this.eventTracker = eventTracker;
    }
}
