package org.apache.archiva.web.servlet;

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

import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.keys.memory.MemoryKeyManager;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.DefaultUserSecurityPolicy;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.DefaultSecuritySystem;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.redback.users.memory.MemoryUserManager;

/**
 * BypassSecuritySystem - used to bypass the security system for testing reasons and allow
 * for every request to respond as success / true. 
 *
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.codehaus.plexus.redback.system.SecuritySystem"
 */
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

    @Override
    public SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        AuthenticationResult result = new AuthenticationResult( true, source.getPrincipal(), null );
        return new DefaultSecuritySession( result );
    }

    @Override
    public AuthorizationResult authorize( SecuritySession session, Object permission )
        throws AuthorizationException
    {
        return new AuthorizationResult( true, session.getUser(), null );
    }

    @Override
    public AuthorizationResult authorize( SecuritySession session, Object permission, Object resource )
        throws AuthorizationException
    {
        return new AuthorizationResult( true, session.getUser(), null );
    }

    @Override
    public String getAuthenticatorId()
    {
        return "bypass-authenticator";
    }

    @Override
    public String getAuthorizerId()
    {
        return "bypass-authorizer";
    }

    @Override
    public KeyManager getKeyManager()
    {
        return bypassKeyManager;
    }

    @Override
    public UserSecurityPolicy getPolicy()
    {
        return bypassPolicy;
    }

    @Override
    public String getUserManagementId()
    {
        return "bypass-managementid";
    }

    @Override
    public UserManager getUserManager()
    {
        return bypassUserManager;
    }

    @Override
    public boolean isAuthenticated( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        // Always true
        return true;
    }

    @Override
    public boolean isAuthorized( SecuritySession session, Object permission )
        throws AuthorizationException
    {
        // Always true
        return true;
    }

    @Override
    public boolean isAuthorized( SecuritySession session, Object permission, Object resource )
        throws AuthorizationException
    {
        // Always true
        return true;
    }
}
