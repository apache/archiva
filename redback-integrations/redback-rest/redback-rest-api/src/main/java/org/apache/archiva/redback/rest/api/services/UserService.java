package org.apache.archiva.redback.rest.api.services;

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

import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserRegistrationRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

@Path( "/userService/" )
public interface UserService
{
    @Path( "getUser/{userName}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    User getUser( @PathParam( "userName" ) String username )
        throws RedbackServiceException;


    @Path( "getUsers" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    List<User> getUsers()
        throws RedbackServiceException;

    @Path( "createUser" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION )
    Boolean createUser( User user )
        throws RedbackServiceException;


    /**
     * will create admin user only if not exists !! if exists will return false
     */
    @Path( "createAdminUser" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true )
    Boolean createAdminUser( User user )
        throws RedbackServiceException;

    @Path( "isAdminUserExists" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    Boolean isAdminUserExists()
        throws RedbackServiceException;


    @Path( "deleteUser/{userName}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION )
    Boolean deleteUser( @PathParam( "userName" ) String username )
        throws RedbackServiceException;

    @Path( "updateUser" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    Boolean updateUser( User user )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "lockUser/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    Boolean lockUser( @PathParam( "username" ) String username )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "unlockUser/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    Boolean unlockUser( @PathParam( "username" ) String username )
        throws RedbackServiceException;


    /**
     * @since 2.0
     */
    @Path( "passwordChangeRequired/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    Boolean passwordChangeRequired( @PathParam( "username" ) String username )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "passwordChangeNotRequired/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    Boolean passwordChangeNotRequired( @PathParam( "username" ) String username )
        throws RedbackServiceException;


    /**
     * update only the current user and this fields: fullname, email, password.
     * the service verify the curent logged user with the one passed in the method
     * @since 1.4
     */
    @Path( "updateMe" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = false, noPermission = true )
    Boolean updateMe( User user )
        throws RedbackServiceException;

    @Path( "ping" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    Boolean ping()
        throws RedbackServiceException;

    @Path( "removeFromCache/{userName}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    int removeFromCache( @PathParam( "userName" ) String username )
        throws RedbackServiceException;

    @Path( "getGuestUser" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    User getGuestUser()
        throws RedbackServiceException;

    @Path( "createGuestUser" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    User createGuestUser()
        throws RedbackServiceException;

    /**
     * if redback is not configured for email validation is required, -1 is returned as key
     * @since 1.4
     */
    @Path( "registerUser" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    RegistrationKey registerUser( UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException;


    /**
     * validate the key and the user with forcing a password change for next login.
     * http session is created.
     * @param key authentication key
     * @since 1.4
     */
    @Path( "validateKey/{key}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    Boolean validateUserFromKey( @PathParam( "key" ) String key )
        throws RedbackServiceException;

    /**
     *
     * @param resetPasswordRequest contains username for send a password reset email
     * @since 1.4
     */
    @Path( "resetPassword" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    Boolean resetPassword( ResetPasswordRequest resetPasswordRequest )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "getUserPermissions/{userName}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    Collection<Permission> getUserPermissions( @PathParam( "userName" ) String userName )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "getUserOperations/{userName}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    Collection<Operation> getUserOperations( @PathParam( "userName" ) String userName )
        throws RedbackServiceException;

    /**
     * @return  the current logged user permissions, if no logged user guest permissions are returned
     * @since 1.4
     */
    @Path( "getCurrentUserPermissions" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    Collection<Permission> getCurrentUserPermissions()
        throws RedbackServiceException;

    /**
     * @return the current logged user operations, if no logged user guest operations are returned
     * @since 1.4
     */
    @Path( "getCurrentUserOperations" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    Collection<Operation> getCurrentUserOperations()
        throws RedbackServiceException;

}
