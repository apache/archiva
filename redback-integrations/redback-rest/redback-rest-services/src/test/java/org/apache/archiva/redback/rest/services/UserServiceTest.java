package org.apache.archiva.redback.rest.services;

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

import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.apache.archiva.redback.rest.services.mock.EmailMessage;
import org.apache.archiva.redback.rest.services.mock.ServicesAssert;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class UserServiceTest
    extends AbstractRestServicesTest
{


    @Test
    public void ping()
        throws Exception
    {
        Boolean res = getUserService().ping();
        assertTrue( res.booleanValue() );
    }

    @Test
    public void getUsers()
        throws Exception
    {
        UserService userService = getUserService();

        WebClient.client( userService ).header( "Authorization", authorizationHeader );

        List<User> users = userService.getUsers();
        assertTrue( users != null );
        assertFalse( users.isEmpty() );
    }

    @Test (expected = ServerWebApplicationException.class)
    public void getUsersWithoutAuthz()
        throws Exception
    {
        UserService userService = getUserService();
        try
        {
            userService.getUsers();
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;
        }

    }

    @Test
    public void getNoPermissionNotAuthz()
        throws Exception
    {

        try
        {
            getFakeCreateAdminService().testAuthzWithoutKarmasNeededButAuthz();
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
        }
    }

    @Test
    public void getNoPermissionAuthz()
        throws Exception
    {

        try
        {
            FakeCreateAdminService service = getFakeCreateAdminService();

            WebClient.client( service ).header( "Authorization", authorizationHeader );

            assertTrue( service.testAuthzWithoutKarmasNeededButAuthz().booleanValue() );

        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
        }
    }

    @Test
    public void register()
        throws Exception
    {
        try
        {
            UserService service = getUserService();
            User u = new User();
            u.setFullName( "the toto" );
            u.setUsername( "toto" );
            u.setEmail( "toto@toto.fr" );
            u.setPassword( "toto123" );
            u.setConfirmPassword( "toto123" );
            String key = service.registerUser( new UserRegistrationRequest( u, "http://wine.fr/bordeaux" ) ).getKey();

            assertFalse( key.equals( "-1" ) );

            ServicesAssert assertService =
                JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/testsService/",
                                           ServicesAssert.class,
                                           Collections.singletonList( new JacksonJaxbJsonProvider() ) );

            List<EmailMessage> emailMessages = assertService.getEmailMessageSended();
            assertEquals( 1, emailMessages.size() );
            assertEquals( "toto@toto.fr", emailMessages.get( 0 ).getTos().get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject() );
            String messageContent = emailMessages.get( 0 ).getText();

            log.info( "messageContent: {}", messageContent );

            assertThat( messageContent ).contains( "Use the following URL to validate your account." ).contains(
                "http://wine.fr/bordeaux" ).containsIgnoringCase( "toto" );

            assertTrue( service.validateUserFromKey( key ) );

            service = getUserService( authorizationHeader );

            u = service.getUser( "toto" );

            assertNotNull( u );
            assertTrue( u.isValidated() );
            assertTrue( u.isPasswordChangeRequired() );

            assertTrue( service.validateUserFromKey( key ) );

        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
        finally
        {
            getUserService( authorizationHeader ).deleteUser( "toto" );
        }

    }

    @Test
    public void registerNoUrl()
        throws Exception
    {
        try
        {
            UserService service = getUserService();
            User u = new User();
            u.setFullName( "the toto" );
            u.setUsername( "toto" );
            u.setEmail( "toto@toto.fr" );
            u.setPassword( "toto123" );
            u.setConfirmPassword( "toto123" );
            String key = service.registerUser( new UserRegistrationRequest( u, null ) ).getKey();

            assertFalse( key.equals( "-1" ) );

            ServicesAssert assertService =
                JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/testsService/",
                                           ServicesAssert.class,
                                           Collections.singletonList( new JacksonJaxbJsonProvider() ) );

            List<EmailMessage> emailMessages = assertService.getEmailMessageSended();
            assertEquals( 1, emailMessages.size() );
            assertEquals( "toto@toto.fr", emailMessages.get( 0 ).getTos().get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject() );
            String messageContent = emailMessages.get( 0 ).getText();

            log.info( "messageContent: {}", messageContent );

            assertThat( messageContent ).contains( "Use the following URL to validate your account." ).contains(
                "http://localhost:" + port ).containsIgnoringCase( "toto" );

            assertTrue( service.validateUserFromKey( key ) );

            service = getUserService( authorizationHeader );

            u = service.getUser( "toto" );

            assertNotNull( u );
            assertTrue( u.isValidated() );
            assertTrue( u.isPasswordChangeRequired() );

            assertTrue( service.validateUserFromKey( key ) );

        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
        finally
        {
            getUserService( authorizationHeader ).deleteUser( "toto" );
        }

    }

    @Test
    public void resetPassword()
        throws Exception
    {
        try
        {
            UserService service = getUserService();
            User u = new User();
            u.setFullName( "the toto" );
            u.setUsername( "toto" );
            u.setEmail( "toto@toto.fr" );
            u.setPassword( "toto123" );
            u.setConfirmPassword( "toto123" );
            String key = service.registerUser( new UserRegistrationRequest( u, "http://wine.fr/bordeaux" ) ).getKey();

            assertFalse( key.equals( "-1" ) );

            ServicesAssert assertService =
                JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/testsService/",
                                           ServicesAssert.class,
                                           Collections.singletonList( new JacksonJaxbJsonProvider() ) );

            WebClient.client( assertService ).accept( MediaType.APPLICATION_JSON_TYPE );
            WebClient.client( assertService ).type( MediaType.APPLICATION_JSON_TYPE );

            List<EmailMessage> emailMessages = assertService.getEmailMessageSended();
            assertEquals( 1, emailMessages.size() );
            assertEquals( "toto@toto.fr", emailMessages.get( 0 ).getTos().get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject() );
            assertTrue(
                emailMessages.get( 0 ).getText().contains( "Use the following URL to validate your account." ) );

            assertTrue( service.validateUserFromKey( key ) );

            service = getUserService( authorizationHeader );

            u = service.getUser( "toto" );

            assertNotNull( u );
            assertTrue( u.isValidated() );
            assertTrue( u.isPasswordChangeRequired() );

            assertTrue( service.validateUserFromKey( key ) );

            assertTrue( service.resetPassword( new ResetPasswordRequest( "toto", "http://foo.fr/bar" ) ) );

            emailMessages = assertService.getEmailMessageSended();
            assertEquals( 2, emailMessages.size() );
            assertEquals( "toto@toto.fr", emailMessages.get( 1 ).getTos().get( 0 ) );

            String messageContent = emailMessages.get( 1 ).getText();

            assertThat( messageContent ).contains( "Password Reset" ).contains( "Username: toto" ).contains(
                "http://foo.fr/bar" );


        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
        finally
        {
            getUserService( authorizationHeader ).deleteUser( "toto" );
        }

    }

    @Test
    public void getAdminPermissions()
        throws Exception
    {
        Collection<Permission> permissions = getUserService( authorizationHeader ).getUserPermissions( "admin" );
        log.info( "admin permisssions:" + permissions );
    }

    @Test
    public void getGuestPermissions()
        throws Exception
    {
        createGuestIfNeeded();
        Collection<Permission> permissions = getUserService().getCurrentUserPermissions();
        log.info( "guest permisssions:" + permissions );
    }

    @Test
    public void getAdminOperations()
        throws Exception
    {
        Collection<Operation> operations = getUserService( authorizationHeader ).getUserOperations( "admin" );
        log.info( "admin operations:" + operations );
    }

    @Test
    public void getGuestOperations()
        throws Exception
    {
        createGuestIfNeeded();
        Collection<Operation> operations = getUserService().getCurrentUserOperations();
        log.info( "guest operations:" + operations );
    }

    @Test
    public void updateMe()
        throws Exception
    {
        User u = new User();
        u.setFullName( "the toto" );
        u.setUsername( "toto" );
        u.setEmail( "toto@toto.fr" );
        u.setPassword( "toto123" );
        u.setConfirmPassword( "toto123" );
        u.setValidated( true );
        getUserService( authorizationHeader ).createUser( u );

        u.setFullName( "the toto123" );
        u.setEmail( "toto@titi.fr" );
        u.setPassword( "toto1234" );
        u.setPreviousPassword( "toto123" );
        getUserService( encode( "toto", "toto123" ) ).updateMe( u );

        u = getUserService( authorizationHeader ).getUser( "toto" );
        assertEquals( "the toto123", u.getFullName() );
        assertEquals( "toto@titi.fr", u.getEmail() );

        u.setFullName( "the toto1234" );
        u.setEmail( "toto@tititi.fr" );
        u.setPassword( "toto12345" );
        u.setPreviousPassword( "toto1234" );
        getUserService( encode( "toto", "toto1234" ) ).updateMe( u );

        u = getUserService( authorizationHeader ).getUser( "toto" );
        assertEquals( "the toto1234", u.getFullName() );
        assertEquals( "toto@tititi.fr", u.getEmail() );

        getUserService( authorizationHeader ).deleteUser( "toto" );
    }

    @Test
    public void lockUnlockUser()
        throws Exception
    {
        try
        {

            // START SNIPPET: create-user
            User user = new User( "toto", "toto the king", "toto@toto.fr", false, false );
            user.setPassword( "foo123" );
            user.setPermanent( false );
            user.setPasswordChangeRequired( false );
            user.setLocked( false );
            user.setValidated( true );
            UserService userService = getUserService( authorizationHeader );
            userService.createUser( user );
            // END SNIPPET: create-user
            user = userService.getUser( "toto" );
            assertNotNull( user );
            assertEquals( "toto the king", user.getFullName() );
            assertEquals( "toto@toto.fr", user.getEmail() );
            getLoginService( encode( "toto", "foo123" ) ).pingWithAutz();

            userService.lockUser( "toto" );

            assertTrue( userService.getUser( "toto" ).isLocked() );

            userService.unlockUser( "toto" );

            assertFalse( userService.getUser( "toto" ).isLocked() );
        }
        finally
        {
            getUserService( authorizationHeader ).deleteUser( "toto" );
            getUserService( authorizationHeader ).removeFromCache( "toto" );
            assertNull( getUserService( authorizationHeader ).getUser( "toto" ) );
        }
    }

    public void guestUserCreate()
        throws Exception
    {
        UserService userService = getUserService( authorizationHeader );
        assertNull( userService.getGuestUser() );
        assertNull( userService.createGuestUser() );

    }

    protected void createGuestIfNeeded()
        throws Exception
    {
        UserService userService = getUserService( authorizationHeader );
        if ( userService.getGuestUser() == null )
        {
            userService.createGuestUser();
        }
    }

}
