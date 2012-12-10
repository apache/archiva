package org.apache.archiva.redback.rest.services.interceptors;

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


import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticationException;
import org.apache.archiva.redback.integration.filter.authentication.basic.HttpBasicAuthentication;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

/**
 * This interceptor will check if the user is already logged in the session.
 * If not ask the redback system to authentication trough BASIC http
 * If the user is logged the AuthenticationResult will in the cxf message with the key AuthenticationResult.class
 *
 * @author Olivier Lamy
 * @since 1.3
 */
@Service("authenticationInterceptor#rest")
public class AuthenticationInterceptor
    extends AbstractInterceptor
    implements RequestHandler
{
    @Inject
    @Named(value = "userManager#configurable")
    private UserManager userManager;

    @Inject
    @Named(value = "httpAuthenticator#basic")
    private HttpBasicAuthentication httpAuthenticator;

    private Logger log = LoggerFactory.getLogger( getClass() );

    public Response handleRequest( Message message, ClassResourceInfo classResourceInfo )
    {

        RedbackAuthorization redbackAuthorization = getRedbackAuthorization( message );
        if ( redbackAuthorization == null )
        {
            log.warn( "http path {} doesn't contain any informations regarding permissions ",
                      message.get( Message.REQUEST_URI ) );
            // here we failed to authenticate so 403 as there is no detail on karma for this
            // it must be marked as it's exposed
            return Response.status( Response.Status.FORBIDDEN ).build();
        }
        HttpServletRequest request = getHttpServletRequest( message );
        HttpServletResponse response = getHttpServletResponse( message );

        if ( redbackAuthorization.noRestriction() )
        {
            // maybe session exists so put it in threadLocal
            // some services need the current user if logged
            SecuritySession securitySession = httpAuthenticator.getSecuritySession( request.getSession( true ) );

            if ( securitySession != null )
            {
                RedbackRequestInformation redbackRequestInformation =
                    new RedbackRequestInformation( securitySession.getUser(), request.getRemoteAddr() );
                RedbackAuthenticationThreadLocal.set( redbackRequestInformation );
            }
            else
            {
                // maybe there is some authz in the request so try it but not fail so catch Exception !
                try
                {
                    AuthenticationResult authenticationResult =
                        httpAuthenticator.getAuthenticationResult( request, response );

                    if ( ( authenticationResult == null ) || ( !authenticationResult.isAuthenticated() ) )
                    {
                        return null;
                    }

                    User user = authenticationResult.getUser() == null ? userManager.findUser(
                        authenticationResult.getPrincipal() ) : authenticationResult.getUser();
                    RedbackRequestInformation redbackRequestInformation =
                        new RedbackRequestInformation( user, request.getRemoteAddr() );

                    RedbackAuthenticationThreadLocal.set( redbackRequestInformation );
                    message.put( AuthenticationResult.class, authenticationResult );
                }
                catch ( Exception e )
                {
                    // ignore here
                }
            }
            return null;
        }

        try
        {
            AuthenticationResult authenticationResult = httpAuthenticator.getAuthenticationResult( request, response );

            if ( ( authenticationResult == null ) || ( !authenticationResult.isAuthenticated() ) )
            {
                throw new HttpAuthenticationException( "You are not authenticated." );
            }

            User user = authenticationResult.getUser() == null
                ? userManager.findUser( authenticationResult.getPrincipal() )
                : authenticationResult.getUser();

            RedbackRequestInformation redbackRequestInformation =
                new RedbackRequestInformation( user, request.getRemoteAddr() );

            RedbackAuthenticationThreadLocal.set( redbackRequestInformation );
            message.put( AuthenticationResult.class, authenticationResult );

            return null;
        }
        catch ( UserNotFoundException e )
        {
            log.debug( "UserNotFoundException for path {}", message.get( Message.REQUEST_URI ) );
            return Response.status( Response.Status.FORBIDDEN ).build();
        }
        catch ( AccountLockedException e )
        {
            log.debug( "account locked for path {}", message.get( Message.REQUEST_URI ) );
            return Response.status( Response.Status.FORBIDDEN ).build();

        }
        catch ( MustChangePasswordException e )
        {
            log.debug( "must change password for path {}", message.get( Message.REQUEST_URI ) );
            return Response.status( Response.Status.FORBIDDEN ).build();

        }
        catch ( AuthenticationException e )
        {
            log.debug( "failed to authenticate for path {}", message.get( Message.REQUEST_URI ) );
            return Response.status( Response.Status.FORBIDDEN ).build();
        }
        catch ( UserManagerException e )
        {
            log.debug( "UserManagerException: {} for path", e.getMessage(), message.get( Message.REQUEST_URI ) );
            return Response.status( Response.Status.FORBIDDEN ).build();
        }
    }
}
