package org.apache.archiva.redback.authentication;

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

import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DefaultAuthenticationManager: the goal of the authentication manager is to act as a conduit for
 * authentication requests into different authentication schemes
 * <p/>
 * For example, the default implementation can be configured with any number of authenticators and will
 * sequentially try them for an authenticated result.  This allows you to have the standard user/pass
 * auth procedure followed by authentication based on a known key for 'remember me' type functionality.
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@Service("authenticationManager")
public class DefaultAuthenticationManager
    implements AuthenticationManager
{

    private List<Authenticator> authenticators;

    @Inject
    private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void initialize()
    {
        this.authenticators =
            new ArrayList<Authenticator>( applicationContext.getBeansOfType( Authenticator.class ).values() );
    }


    public String getId()
    {
        return "Default Authentication Manager - " + this.getClass().getName() + " : managed authenticators - " +
            knownAuthenticators();
    }

    public AuthenticationResult authenticate( AuthenticationDataSource source )
        throws AccountLockedException, AuthenticationException, MustChangePasswordException
    {
        if ( authenticators == null || authenticators.size() == 0 )
        {
            return ( new AuthenticationResult( false, null, new AuthenticationException(
                "no valid authenticators, can't authenticate" ) ) );
        }

        // put AuthenticationResult exceptions in a map
        Map<String, String> authnResultExceptionsMap = new HashMap<String, String>();
        for ( Authenticator authenticator : authenticators )
        {
            if ( authenticator.supportsDataSource( source ) )
            {
                AuthenticationResult authResult = authenticator.authenticate( source );
                Map<String, String> exceptionsMap = authResult.getExceptionsMap();

                if ( authResult.isAuthenticated() )
                {
                    return authResult;
                }

                if ( exceptionsMap != null )
                {
                    authnResultExceptionsMap.putAll( exceptionsMap );
                }
                else
                {
                    if ( authResult.getException() != null )
                    {
                        authnResultExceptionsMap.put( AuthenticationConstants.AUTHN_RUNTIME_EXCEPTION,
                                                      authResult.getException().getMessage() );
                    }
                }


            }
        }

        return ( new AuthenticationResult( false, null, new AuthenticationException(
            "authentication failed on authenticators: " + knownAuthenticators() ), authnResultExceptionsMap ) );
    }

    public List<Authenticator> getAuthenticators()
    {
        return authenticators;
    }

    private String knownAuthenticators()
    {
        StringBuilder strbuf = new StringBuilder();

        for ( Authenticator authenticator : authenticators )
        {
            strbuf.append( '(' ).append( authenticator.getId() ).append( ") " );
        }

        return strbuf.toString();
    }
}
