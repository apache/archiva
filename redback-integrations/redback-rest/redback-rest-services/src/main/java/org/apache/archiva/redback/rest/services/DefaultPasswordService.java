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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.PasswordRuleViolationException;
import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.services.PasswordService;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.services.utils.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service("passwordService#rest")
public class DefaultPasswordService
    implements PasswordService
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private SecuritySystem securitySystem;

    private HttpAuthenticator httpAuthenticator;

    private PasswordValidator passwordValidator;

    @Context
    private HttpServletRequest httpServletRequest;

    @Inject
    public DefaultPasswordService( SecuritySystem securitySystem,
                                   @Named("httpAuthenticator#basic") HttpAuthenticator httpAuthenticator,
                                   PasswordValidator passwordValidator )
    {
        this.securitySystem = securitySystem;
        this.httpAuthenticator = httpAuthenticator;
        this.passwordValidator = passwordValidator;
    }

    public org.apache.archiva.redback.rest.api.model.User changePasswordWithKey( String password,
                                                                                 String passwordConfirmation,
                                                                                 String key )
        throws RedbackServiceException
    {

        //RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();

        String principal = null;

        if ( StringUtils.isEmpty( password ) )
        {
            throw new RedbackServiceException( "password cannot be empty", Response.Status.FORBIDDEN.getStatusCode() );
        }
        if ( StringUtils.isEmpty( passwordConfirmation ) )
        {
            throw new RedbackServiceException( "password confirmation cannot be empty",
                                               Response.Status.FORBIDDEN.getStatusCode() );
        }
        if ( !StringUtils.equals( password, passwordConfirmation ) )
        {
            throw new RedbackServiceException( "password confirmation must be same as password",
                                               Response.Status.FORBIDDEN.getStatusCode() );
        }

        try
        {
            AuthenticationKey authKey = securitySystem.getKeyManager().findKey( key );

            principal = authKey.getForPrincipal();

            String encodedPassword = passwordValidator.validatePassword( password, principal );

            User user = securitySystem.getUserManager().findUser( principal );
            user.setPassword( password );
            user.setEncodedPassword( encodedPassword );
            user = securitySystem.getUserManager().updateUser( user );

            return new org.apache.archiva.redback.rest.api.model.User( user );

        }
        catch ( KeyManagerException e )
        {
            log.info( "issue to find key {}: {}", key, e.getMessage() );
            throw new RedbackServiceException( "issue with key", Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( UserNotFoundException e )
        {
            log.info( "user {} not found", e.getMessage() );
            List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>( 2 );
            ErrorMessage errorMessage = new ErrorMessage( "cannot.update.user.not.found", new String[]{ principal } );
            errorMessages.add( errorMessage );
            errorMessage = new ErrorMessage( "admin.deleted.account" );
            errorMessages.add( errorMessage );
            throw new RedbackServiceException( errorMessages );
        }
        catch ( UserManagerException e )
        {
            log.info( "UserManagerException: {}", e.getMessage() );
            List<ErrorMessage> errorMessages =
                Arrays.asList( new ErrorMessage().message( "UserManagerException: " + e.getMessage() ) );
            throw new RedbackServiceException( errorMessages );
        }
        catch ( PasswordRuleViolationException e )
        {
            PasswordRuleViolations violations = e.getViolations();
            List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>( violations.getViolations().size() );
            if ( violations != null )
            {
                for ( String violation : violations.getLocalizedViolations() )
                {
                    errorMessages.add( new ErrorMessage( violation ) );
                }
            }
            throw new RedbackServiceException( errorMessages );
        }

    }

    public org.apache.archiva.redback.rest.api.model.User changePassword( String userName, String previousPassword,
                                                                          String password, String passwordConfirmation )
        throws RedbackServiceException
    {
        if ( StringUtils.isEmpty( userName ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "username.cannot.be.empty" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        if ( StringUtils.isEmpty( previousPassword ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "password.previous.empty" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        if ( StringUtils.isEmpty( password ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "password.empty" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        if ( StringUtils.isEmpty( passwordConfirmation ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "password.confirmation.empty" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }

        if ( !StringUtils.equals( password, passwordConfirmation ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "password.confirmation.same" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        try
        {
            User u = securitySystem.getUserManager().findUser( userName );

            String previousEncodedPassword = u.getEncodedPassword();

            // check oldPassword with the current one

            PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

            if ( !encoder.isPasswordValid( previousEncodedPassword, previousPassword ) )
            {

                throw new RedbackServiceException( new ErrorMessage( "password.provided.does.not.match.existing" ),
                                                   Response.Status.BAD_REQUEST.getStatusCode() );
            }

            u.setPassword( password );

            u = securitySystem.getUserManager().updateUser( u );
            return new org.apache.archiva.redback.rest.api.model.User( u );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "user.not.found" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        catch ( UserManagerException e )
        {
            log.info( "UserManagerException: {}", e.getMessage() );
            List<ErrorMessage> errorMessages =
                Arrays.asList( new ErrorMessage().message( "UserManagerException: " + e.getMessage() ) );
            throw new RedbackServiceException( errorMessages );
        }

    }
}
