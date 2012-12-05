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

import net.sf.ehcache.CacheManager;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.TokenBasedAuthenticationDataSource;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.integration.mail.Mailer;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.keys.KeyNotFoundException;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.Resource;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.apache.archiva.redback.rest.services.utils.PasswordValidator;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service( "userService#rest" )
public class DefaultUserService
    implements UserService
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private static final String VALID_USERNAME_CHARS = "[a-zA-Z_0-9\\-.@]*";

    private UserManager userManager;

    private SecuritySystem securitySystem;

    @Inject
    @Named( value = "userConfiguration" )
    private UserConfiguration config;

    @Inject
    private RoleManager roleManager;

    /**
     * cache used for user assignments
     */
    @Inject
    @Named( value = "cache#userAssignments" )
    private Cache userAssignmentsCache;

    /**
     * cache used for user permissions
     */
    @Inject
    @Named( value = "cache#userPermissions" )
    private Cache userPermissionsCache;

    /**
     * Cache used for users
     */
    @Inject
    @Named( value = "cache#users" )
    private Cache usersCache;

    @Inject
    private Mailer mailer;

    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager rbacManager;

    private HttpAuthenticator httpAuthenticator;

    @Inject
    private PasswordValidator passwordValidator;

    @Context
    private HttpServletRequest httpServletRequest;

    @Inject
    public DefaultUserService( @Named( value = "userManager#cached" ) UserManager userManager,
                               SecuritySystem securitySystem,
                               @Named( "httpAuthenticator#basic" ) HttpAuthenticator httpAuthenticator )
    {
        this.userManager = userManager;
        this.securitySystem = securitySystem;
        this.httpAuthenticator = httpAuthenticator;
    }


    public Boolean createUser( User user )
        throws RedbackServiceException
    {

        try
        {
            org.apache.archiva.redback.users.User u = userManager.findUser( user.getUsername() );
            if ( u != null )
            {
                throw new RedbackServiceException(
                    new ErrorMessage( "user " + user.getUsername() + " already exists" ) );
            }
        }
        catch ( UserNotFoundException e )
        {
            //ignore we just want to prevent non human readable error message from backend :-)
            log.debug( "user {} not exists", user.getUsername() );
        }

        // data validation
        if ( StringUtils.isEmpty( user.getUsername() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "username cannot be empty" ) );
        }

        if ( StringUtils.isEmpty( user.getFullName() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "fullName cannot be empty" ) );
        }

        if ( StringUtils.isEmpty( user.getEmail() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "email cannot be empty" ) );
        }

        org.apache.archiva.redback.users.User u =
            userManager.createUser( user.getUsername(), user.getFullName(), user.getEmail() );
        u.setPassword( user.getPassword() );
        u.setLocked( user.isLocked() );
        u.setPasswordChangeRequired( user.isPasswordChangeRequired() );
        u.setPermanent( user.isPermanent() );
        u.setValidated( user.isValidated() );
        u = userManager.addUser( u );
        if ( !user.isPasswordChangeRequired() )
        {
            u.setPasswordChangeRequired( false );
            try
            {
                u = userManager.updateUser( u );
                log.debug( "user {} created", u.getUsername() );
            }
            catch ( UserNotFoundException e )
            {
                throw new RedbackServiceException( e.getMessage() );
            }
        }
        try
        {
            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getUsername() );
        }
        catch ( RoleManagerException rpe )
        {
            log.error( "RoleProfile Error: " + rpe.getMessage(), rpe );
            throw new RedbackServiceException( new ErrorMessage( "assign.role.failure", null ) );
        }
        return Boolean.TRUE;
    }

    public Boolean deleteUser( String username )
        throws RedbackServiceException
    {

        try
        {

            if ( rbacManager.userAssignmentExists( username ) )
            {
                UserAssignment assignment = rbacManager.getUserAssignment( username );
                rbacManager.removeUserAssignment( assignment );
            }

        }
        catch ( RbacManagerException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        try
        {
            userManager.deleteUser( username );
            return Boolean.TRUE;
        }
        catch ( UserNotFoundException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        finally
        {
            removeFromCache( username );
        }
    }


    public User getUser( String username )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( username );
            return getSimpleUser( user );
        }
        catch ( UserNotFoundException e )
        {
            return null;
        }
    }

    public List<User> getUsers()
        throws RedbackServiceException
    {
        List<org.apache.archiva.redback.users.User> users = userManager.getUsers();
        List<User> simpleUsers = new ArrayList<User>( users.size() );

        for ( org.apache.archiva.redback.users.User user : users )
        {
            simpleUsers.add( getSimpleUser( user ) );
        }

        return simpleUsers;
    }

    public Boolean updateMe( User user )
        throws RedbackServiceException
    {
        // check username == one in the session
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        if ( redbackRequestInformation == null || redbackRequestInformation.getUser() == null )
        {
            throw new RedbackServiceException( new ErrorMessage( "you must be logged to update your profile" ),
                                               Response.Status.FORBIDDEN.getStatusCode() );
        }
        if ( user == null )
        {
            throw new RedbackServiceException( new ErrorMessage( "user parameter is mandatory" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        if ( !StringUtils.equals( redbackRequestInformation.getUser().getUsername(), user.getUsername() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "you can update only your profile" ),
                                               Response.Status.FORBIDDEN.getStatusCode() );
        }

        if ( StringUtils.isEmpty( user.getPreviousPassword() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "previous password is empty" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }

        User realUser = getUser( user.getUsername() );
        try
        {
            String previousEncodedPassword =
                securitySystem.getUserManager().findUser( user.getUsername() ).getEncodedPassword();

            // check oldPassword with the current one

            PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

            if ( !encoder.isPasswordValid( previousEncodedPassword, user.getPreviousPassword() ) )
            {

                throw new RedbackServiceException( new ErrorMessage( "password.provided.does.not.match.existing" ),
                                                   Response.Status.BAD_REQUEST.getStatusCode() );
            }
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "user not found" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        // only 3 fields to update
        realUser.setFullName( user.getFullName() );
        realUser.setEmail( user.getEmail() );
        // ui can limit to not update password
        if ( StringUtils.isNotBlank( user.getPassword() ) )
        {
            passwordValidator.validatePassword( user.getPassword(), user.getUsername() );

            realUser.setPassword( user.getPassword() );
        }

        updateUser( realUser );

        return Boolean.TRUE;
    }

    public Boolean updateUser( User user )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User rawUser = userManager.findUser( user.getUsername() );
            rawUser.setFullName( user.getFullName() );
            rawUser.setEmail( user.getEmail() );
            rawUser.setValidated( user.isValidated() );
            rawUser.setLocked( user.isLocked() );
            rawUser.setPassword( user.getPassword() );
            rawUser.setPasswordChangeRequired( user.isPasswordChangeRequired() );
            rawUser.setPermanent( user.isPermanent() );

            userManager.updateUser( rawUser );
            return Boolean.TRUE;
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    public int removeFromCache( String userName )
        throws RedbackServiceException
    {
        if ( userAssignmentsCache != null )
        {
            userAssignmentsCache.remove( userName );
        }
        if ( userPermissionsCache != null )
        {
            userPermissionsCache.remove( userName );
        }
        if ( usersCache != null )
        {
            usersCache.remove( userName );
        }

        CacheManager cacheManager = CacheManager.getInstance();
        String[] caches = cacheManager.getCacheNames();
        for ( String cacheName : caches )
        {
            if ( StringUtils.startsWith( cacheName, "org.apache.archiva.redback.rbac.jdo" ) )
            {
                cacheManager.getCache( cacheName ).removeAll();
            }
        }

        return 0;
    }

    public User getGuestUser()
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User user = userManager.getGuestUser();
            return getSimpleUser( user );
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    public User createGuestUser()
        throws RedbackServiceException
    {
        User u = getGuestUser();
        if ( u != null )
        {
            return u;
        }
        // temporary disable policy during guest creation as no password !
        try
        {
            securitySystem.getPolicy().setEnabled( false );
            org.apache.archiva.redback.users.User user = userManager.createGuestUser();
            user.setPasswordChangeRequired( false );
            user = userManager.updateUser( user, false );
            roleManager.assignRole( "guest", user.getUsername() );
            return getSimpleUser( user );
        }
        catch ( RoleManagerException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( UserNotFoundException e )
        {
            // olamy I wonder how this can happen :-)
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        finally
        {

            if ( !securitySystem.getPolicy().isEnabled() )
            {
                securitySystem.getPolicy().setEnabled( true );
            }
        }
    }

    public Boolean ping()
        throws RedbackServiceException
    {
        return Boolean.TRUE;
    }

    private User getSimpleUser( org.apache.archiva.redback.users.User user )
    {
        if ( user == null )
        {
            return null;
        }
        return new User( user );
    }

    public Boolean createAdminUser( User adminUser )
        throws RedbackServiceException
    {
        if ( isAdminUserExists() )
        {
            return Boolean.FALSE;
        }

        org.apache.archiva.redback.users.User user =
            userManager.createUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, adminUser.getFullName(),
                                    adminUser.getEmail() );
        user.setPassword( adminUser.getPassword() );

        user.setLocked( false );
        user.setPasswordChangeRequired( false );
        user.setPermanent( true );
        user.setValidated( true );

        userManager.addUser( user );

        try
        {
            roleManager.assignRole( "system-administrator", user.getUsername() );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean isAdminUserExists()
        throws RedbackServiceException
    {
        try
        {
            userManager.findUser( config.getString( UserConfigurationKeys.DEFAULT_ADMIN ) );
            return Boolean.TRUE;
        }
        catch ( UserNotFoundException e )
        {
            // ignore
        }
        return Boolean.FALSE;
    }

    public Boolean resetPassword( ResetPasswordRequest resetPasswordRequest )
        throws RedbackServiceException
    {
        String username = resetPasswordRequest.getUsername();
        if ( StringUtils.isEmpty( username ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "username.cannot.be.empty" ) );
        }

        UserManager userManager = securitySystem.getUserManager();
        KeyManager keyManager = securitySystem.getKeyManager();
        UserSecurityPolicy policy = securitySystem.getPolicy();

        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( username );

            AuthenticationKey authkey = keyManager.createKey( username, "Password Reset Request",
                                                              policy.getUserValidationSettings().getEmailValidationTimeout() );

            String applicationUrl = resetPasswordRequest.getApplicationUrl();
            if ( StringUtils.isBlank( applicationUrl ) )
            {
                applicationUrl = getBaseUrl();
            }

            mailer.sendPasswordResetEmail( Arrays.asList( user.getEmail() ), authkey, applicationUrl );
            log.info( "password reset request for username {}", username );
        }
        catch ( UserNotFoundException e )
        {
            log.info( "Password Reset on non-existant user [{}].", username );
            throw new RedbackServiceException( new ErrorMessage( "password.reset.failure" ) );
        }
        catch ( KeyManagerException e )
        {
            log.info( "Unable to issue password reset.", e );
            throw new RedbackServiceException( new ErrorMessage( "password.reset.email.generation.failure" ) );
        }

        return Boolean.TRUE;
    }

    public RegistrationKey registerUser( UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException
    {
        User user = userRegistrationRequest.getUser();
        if ( user == null )
        {
            throw new RedbackServiceException( new ErrorMessage( "invalid.user.credentials", null ) );

        }

        UserSecurityPolicy securityPolicy = securitySystem.getPolicy();

        boolean emailValidationRequired = securityPolicy.getUserValidationSettings().isEmailValidationRequired();

        if ( emailValidationRequired )
        {
            validateCredentialsLoose( user );
        }
        else
        {
            validateCredentialsStrict( user );
        }

        // NOTE: Do not perform Password Rules Validation Here.

        if ( userManager.userExists( user.getUsername() ) )
        {
            throw new RedbackServiceException(
                new ErrorMessage( "user.already.exists", new String[]{ user.getUsername() } ) );
        }

        org.apache.archiva.redback.users.User u =
            userManager.createUser( user.getUsername(), user.getFullName(), user.getEmail() );
        u.setPassword( user.getPassword() );
        u.setValidated( false );
        u.setLocked( false );

        try
        {
            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getUsername() );
        }
        catch ( RoleManagerException rpe )
        {
            log.error( "RoleProfile Error: " + rpe.getMessage(), rpe );
            throw new RedbackServiceException( new ErrorMessage( "assign.role.failure", null ) );
        }

        if ( emailValidationRequired )
        {
            u.setLocked( true );

            try
            {
                AuthenticationKey authkey =
                    securitySystem.getKeyManager().createKey( u.getUsername(), "New User Email Validation",
                                                              securityPolicy.getUserValidationSettings().getEmailValidationTimeout() );

                String baseUrl = userRegistrationRequest.getApplicationUrl();
                if ( StringUtils.isBlank( baseUrl ) )
                {
                    baseUrl = getBaseUrl();
                }

                log.debug( "register user {} with email {} and app url {}", u.getUsername(), u.getEmail(), baseUrl );

                mailer.sendAccountValidationEmail( Arrays.asList( u.getEmail() ), authkey, baseUrl );

                securityPolicy.setEnabled( false );
                userManager.addUser( u );
                return new RegistrationKey( authkey.getKey() );

            }
            catch ( KeyManagerException e )
            {
                log.error( "Unable to register a new user.", e );
                throw new RedbackServiceException( new ErrorMessage( "cannot.register.user", null ) );
            }
            finally
            {
                securityPolicy.setEnabled( true );
            }
        }
        else
        {
            userManager.addUser( u );
            return new RegistrationKey( "-1" );
        }

        // FIXME log this event
        /*
        AuditEvent event = new AuditEvent( getText( "log.account.create" ) );
        event.setAffectedUser( username );
        event.log();
        */

    }

    public Boolean validateUserFromKey( String key )
        throws RedbackServiceException
    {
        String principal = null;
        try
        {
            AuthenticationKey authkey = securitySystem.getKeyManager().findKey( key );

            org.apache.archiva.redback.users.User user =
                securitySystem.getUserManager().findUser( authkey.getForPrincipal() );

            user.setValidated( true );
            user.setLocked( false );
            user.setPasswordChangeRequired( true );
            user.setEncodedPassword( "" );

            principal = user.getUsername();

            TokenBasedAuthenticationDataSource authsource = new TokenBasedAuthenticationDataSource();
            authsource.setPrincipal( principal );
            authsource.setToken( authkey.getKey() );
            authsource.setEnforcePasswordChange( false );

            securitySystem.getUserManager().updateUser( user );

            httpAuthenticator.authenticate( authsource, httpServletRequest.getSession( true ) );

            log.info( "account validated for user {}", user.getUsername() );

            return Boolean.TRUE;
        }
        catch ( MustChangePasswordException e )
        {
            throw new RedbackServiceException( e.getMessage(), Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( KeyNotFoundException e )
        {
            log.info( "Invalid key requested: {}", key );
            throw new RedbackServiceException( new ErrorMessage( "cannot.find.key" ) );
        }
        catch ( KeyManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "cannot.find.key.at.the.momment" ) );

        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "cannot.find.user", new String[]{ principal } ) );

        }
        catch ( AccountLockedException e )
        {
            throw new RedbackServiceException( e.getMessage(), Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( AuthenticationException e )
        {
            throw new RedbackServiceException( e.getMessage(), Response.Status.FORBIDDEN.getStatusCode() );
        }
    }

    public Collection<Permission> getCurrentUserPermissions()
        throws RedbackServiceException
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        String userName = UserManager.GUEST_USERNAME;
        if ( redbackRequestInformation != null && redbackRequestInformation.getUser() != null )
        {
            userName = redbackRequestInformation.getUser().getUsername();
        }

        return getUserPermissions( userName );
    }

    public Collection<Operation> getCurrentUserOperations()
        throws RedbackServiceException
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        String userName = UserManager.GUEST_USERNAME;
        if ( redbackRequestInformation != null && redbackRequestInformation.getUser() != null )
        {
            userName = redbackRequestInformation.getUser().getUsername();
        }

        return getUserOperations( userName );
    }

    public Collection<Operation> getUserOperations( String userName )
        throws RedbackServiceException
    {
        Collection<Permission> permissions = getUserPermissions( userName );
        List<Operation> operations = new ArrayList<Operation>( permissions.size() );
        for ( Permission permission : permissions )
        {
            if ( permission.getOperation() != null )
            {
                Operation operation = new Operation();
                operation.setName( permission.getOperation().getName() );
                operations.add( operation );
            }
        }
        return operations;
    }

    public Collection<Permission> getUserPermissions( String userName )
        throws RedbackServiceException
    {
        try
        {
            Set<org.apache.archiva.redback.rbac.Permission> permissions =
                rbacManager.getAssignedPermissions( userName );
            // FIXME return guest permissions !!
            List<Permission> userPermissions = new ArrayList<Permission>( permissions.size() );
            for ( org.apache.archiva.redback.rbac.Permission p : permissions )
            {
                Permission permission = new Permission();
                permission.setName( p.getName() );

                if ( p.getOperation() != null )
                {
                    Operation operation = new Operation();
                    operation.setName( p.getOperation().getName() );
                    permission.setOperation( operation );
                }

                if ( p.getResource() != null )
                {
                    Resource resource = new Resource();
                    resource.setIdentifier( p.getResource().getIdentifier() );
                    resource.setPattern( p.getResource().isPattern() );
                    permission.setResource( resource );
                }

                userPermissions.add( permission );
            }
            return userPermissions;
        }
        catch ( RbacObjectNotFoundException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( RbacManagerException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    public void validateCredentialsLoose( User user )
        throws RedbackServiceException
    {
        RedbackServiceException redbackServiceException =
            new RedbackServiceException( "issues during validating user" );
        if ( StringUtils.isEmpty( user.getUsername() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "username.required", null ) );
        }
        else
        {
            if ( !user.getUsername().matches( VALID_USERNAME_CHARS ) )
            {
                redbackServiceException.addErrorMessage( new ErrorMessage( "username.invalid.characters", null ) );
            }
        }

        if ( StringUtils.isEmpty( user.getFullName() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "fullName.required", null ) );
        }

        if ( StringUtils.isEmpty( user.getEmail() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "email.required", null ) );
        }

        if ( !StringUtils.equals( user.getPassword(), user.getConfirmPassword() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "passwords.does.not.match", null ) );
        }

        try
        {
            if ( !org.codehaus.plexus.util.StringUtils.isEmpty( user.getEmail() ) )
            {
                new InternetAddress( user.getEmail(), true );
            }
        }
        catch ( AddressException e )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "email.invalid", null ) );
        }
        if ( !redbackServiceException.getErrorMessages().isEmpty() )
        {
            throw redbackServiceException;
        }
    }

    public void validateCredentialsStrict( User user )
        throws RedbackServiceException
    {
        validateCredentialsLoose( user );

        org.apache.archiva.redback.users.User tmpuser =
            userManager.createUser( user.getUsername(), user.getFullName(), user.getEmail() );

        user.setPassword( user.getPassword() );

        securitySystem.getPolicy().validatePassword( tmpuser );

        if ( ( org.codehaus.plexus.util.StringUtils.isEmpty( user.getPassword() ) ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "password.required", null ) );
        }
    }

    private String getBaseUrl()
    {
        if ( httpServletRequest != null )
        {
            if ( httpServletRequest != null )
            {
                return httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + (
                    httpServletRequest.getServerPort() == 80
                        ? ""
                        : ":" + httpServletRequest.getServerPort() ) + httpServletRequest.getContextPath();
            }
        }
        return null;
    }

    public Boolean unlockUser( String username )
        throws RedbackServiceException
    {
        User user = getUser( username );
        if ( user != null )
        {
            user.setLocked( false );
            updateUser( user );
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public Boolean lockUser( String username )
        throws RedbackServiceException
    {
        User user = getUser( username );
        if ( user != null )
        {
            user.setLocked( true );
            updateUser( user );
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public Boolean passwordChangeRequired( String username )
        throws RedbackServiceException
    {
        User user = getUser( username );
        if ( user == null )
        {
            user.setPasswordChangeRequired( true );
            updateUser( user );
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public Boolean passwordChangeNotRequired( String username )
        throws RedbackServiceException
    {
        User user = getUser( username );
        if ( user == null )
        {
            user.setPasswordChangeRequired( false );
            updateUser( user );
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
