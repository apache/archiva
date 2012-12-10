package org.apache.archiva.redback.authentication.keystore;

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

import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.TokenBasedAuthenticationDataSource;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.keys.KeyNotFoundException;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * KeyStoreAuthenticator:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@Service("authenticator#keystore")
public class KeyStoreAuthenticator
    implements Authenticator
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Resource(name = "keyManager#cached")
    private KeyManager keystore;

    @Resource(name = "userManager#configurable")
    private UserManager userManager;

    public String getId()
    {
        return getClass().getName();
    }

    public AuthenticationResult authenticate( AuthenticationDataSource source )
        throws AccountLockedException, AuthenticationException, MustChangePasswordException
    {
        TokenBasedAuthenticationDataSource dataSource = (TokenBasedAuthenticationDataSource) source;

        String key = dataSource.getToken();
        try
        {
            AuthenticationKey authKey = keystore.findKey( key );

            // if we find a key (exception was probably thrown if not) then we should be authentic
            if ( authKey != null )
            {
                User user = userManager.findUser( dataSource.getPrincipal() );

                if ( user.isLocked() )
                {
                    throw new AccountLockedException( "Account " + source.getPrincipal() + " is locked.", user );
                }

                if ( user.isPasswordChangeRequired() && source.isEnforcePasswordChange() )
                {
                    throw new MustChangePasswordException( "Password expired.", user );
                }

                return new AuthenticationResult( true, dataSource.getPrincipal(), null );
            }
            else
            {
                return new AuthenticationResult( false, dataSource.getPrincipal(),
                                                 new AuthenticationException( "unable to find key" ) );
            }
        }
        catch ( KeyNotFoundException ne )
        {
            return new AuthenticationResult( false, null, ne );
        }
        catch ( KeyManagerException ke )
        {
            throw new AuthenticationException( "underlaying keymanager issue", ke );
        }
        catch ( UserNotFoundException e )
        {
            log.warn( "Login for user {} failed. user not found.", source.getPrincipal() );
            return new AuthenticationResult( false, null, e );
        }
        catch ( UserManagerException e )
        {
            log.warn( "Login fail for user {} failed. message: {}", source.getPrincipal(), e.getMessage() );
            return new AuthenticationResult( false, null, e );
        }
    }

    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return source instanceof TokenBasedAuthenticationDataSource;
    }
}