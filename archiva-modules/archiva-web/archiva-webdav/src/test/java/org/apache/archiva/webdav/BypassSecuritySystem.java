package org.apache.archiva.webdav;

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

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.keys.memory.MemoryKeyManager;
import org.apache.archiva.redback.policy.DefaultUserSecurityPolicy;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.DefaultSecuritySystem;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.memory.MemoryUserManager;
import org.springframework.stereotype.Service;

/**
 * BypassSecuritySystem - used to bypass the security system for testing reasons and allow
 * for every request to respond as success / true. 
 *
 * @version $Id$
 */
@Service("securitySystem#bypass")
public class BypassSecuritySystem
    extends DefaultSecuritySystem
    implements SecuritySystem
{
    private KeyManager bypassKeyManager;
    private UserSecurityPolicy bypassPolicy;
    private UserManager bypassUserManager;
    
    public BypassSecuritySystem()
    {
        bypassKeyManager = new MemoryKeyManager();
        bypassPolicy = new DefaultUserSecurityPolicy();
        bypassUserManager = new MemoryUserManager();
    }
    
    public SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        AuthenticationResult result = new AuthenticationResult( true, source.getPrincipal(), null );
        return new DefaultSecuritySession( result );
    }

    public AuthorizationResult authorize( SecuritySession session, Object permission )
        throws AuthorizationException
    {
        return new AuthorizationResult( true, session.getUser(), null );
    }

    public AuthorizationResult authorize( SecuritySession session, Object permission, Object resource )
        throws AuthorizationException
    {
        return new AuthorizationResult( true, session.getUser(), null );
    }

    public String getAuthenticatorId()
    {
        return "bypass-authenticator";
    }

    public String getAuthorizerId()
    {
        return "bypass-authorizer";
    }

    public KeyManager getKeyManager()
    {
        return bypassKeyManager;
    }

    public UserSecurityPolicy getPolicy()
    {
        return bypassPolicy;
    }

    public String getUserManagementId()
    {
        return "bypass-managementid";
    }

    public UserManager getUserManager()
    {
        return bypassUserManager;
    }

    public boolean isAuthenticated( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        // Always true
        return true;
    }

    public boolean isAuthorized( SecuritySession session, Object permission )
        throws AuthorizationException
    {
        // Always true
        return true;
    }

    public boolean isAuthorized( SecuritySession session, Object permission, Object resource )
        throws AuthorizationException
    {
        // Always true
        return true;
    }
}
