package org.codehaus.plexus.redback.authentication.open;

/*
 * Copyright 2001-2006 The Codehaus.
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

import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authentication.Authenticator;
import org.codehaus.plexus.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.springframework.stereotype.Service;

/**
 * OpenAuthenticator - Does not test user / password.
 * All attempts result in access. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 */
@Service("authenticator#open")
public class OpenAuthenticator
    implements Authenticator
{

    public AuthenticationResult authenticate( AuthenticationDataSource s )
        throws AccountLockedException, AuthenticationException
    {
        PasswordBasedAuthenticationDataSource source = (PasswordBasedAuthenticationDataSource) s;
        return new AuthenticationResult( true, source.getPrincipal(), null );
    }

    public String getId()
    {
        return "Open Authenticator";
    }

    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return ( source instanceof PasswordBasedAuthenticationDataSource );
    }
}
