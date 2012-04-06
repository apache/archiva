package org.codehaus.plexus.redback.system;

/*
 * Copyright 2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.users.User;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * @author Jason van Zyl
 */
@Service( "securitySession" )
public class DefaultSecuritySession
    implements SecuritySession, Serializable
{
    private AuthenticationResult authenticationResult;

    private User user;

    // TODO: ambiguity between this authenticated and authentication result's authenticated is dangerous
    private boolean authenticated;

    public DefaultSecuritySession()
    {
        this.authenticationResult = new AuthenticationResult();
        this.user = null;
        this.authenticated = false;
    }

    public DefaultSecuritySession( AuthenticationResult authResult )
    {
        this.authenticationResult = authResult;
        this.user = null;
        this.authenticated = false;
    }

    public DefaultSecuritySession( AuthenticationResult authenticationResult, User user )
    {
        this.authenticationResult = authenticationResult;
        this.user = user;
        this.authenticated = true;
    }

    public AuthenticationResult getAuthenticationResult()
    {
        return authenticationResult;
    }

    public User getUser()
    {
        return user;
    }

    public boolean isAuthenticated()
    {
        return ( ( user != null ) && authenticated );
    }
}
