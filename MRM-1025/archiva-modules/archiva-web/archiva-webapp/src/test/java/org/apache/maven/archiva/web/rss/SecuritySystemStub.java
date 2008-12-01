package org.apache.maven.archiva.web.rss;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.redback.users.jdo.JdoUser;

/**
 * SecuritySystem stub used for testing. 
 *
 * @version $Id$
 */
public class SecuritySystemStub
    implements SecuritySystem
{
    Map<String, String> users = new HashMap<String, String>();

    List<String> repoIds = new ArrayList<String>();

    public SecuritySystemStub()
    {
        users.put( "user1", "password1" );
        users.put( "user2", "password2" );
        users.put( "user3", "password3" );

        repoIds.add( "test-repo" );
    }

    public SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        AuthenticationResult result = null;
        SecuritySession session = null;

        if ( users.get( source.getPrincipal() ) != null )
        {
            result = new AuthenticationResult( true, source.getPrincipal(), null );

            User user = new JdoUser();
            user.setUsername( source.getPrincipal() );
            user.setPassword( users.get( source.getPrincipal() ) );

            session = new DefaultSecuritySession( result, user );
        }
        else
        {
            result = new AuthenticationResult( false, source.getPrincipal(), null );
            session = new DefaultSecuritySession( result );
        }
        return session;
    }

    public AuthorizationResult authorize( SecuritySession arg0, Object arg1 )
        throws AuthorizationException
    {
        return null;
    }

    public AuthorizationResult authorize( SecuritySession arg0, Object arg1, Object arg2 )
        throws AuthorizationException
    {
        AuthorizationResult result = new AuthorizationResult( true, arg1, null);
        
        return result;
    }

    public String getAuthenticatorId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAuthorizerId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public KeyManager getKeyManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UserSecurityPolicy getPolicy()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserManagementId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UserManager getUserManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAuthenticated( AuthenticationDataSource arg0 )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAuthorized( SecuritySession arg0, Object arg1 )
        throws AuthorizationException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAuthorized( SecuritySession arg0, Object arg1, Object arg2 )
        throws AuthorizationException
    {
        if ( repoIds.contains( arg2 ) )
        {
            return true;
        }

        return false;
    }

}
