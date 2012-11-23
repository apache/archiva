package org.apache.archiva.redback.system;

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
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.users.UserManager;

/**
 * SecuritySystem:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
public interface SecuritySystem
{

    // ----------------------------------------------------------------------------
    // Authentication
    // ----------------------------------------------------------------------------

    SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException, MustChangePasswordException;

    boolean isAuthenticated( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException, MustChangePasswordException;

    // ----------------------------------------------------------------------------
    // Authorization
    // ----------------------------------------------------------------------------

    AuthorizationResult authorize( SecuritySession session, String permission )
        throws AuthorizationException;

    boolean isAuthorized( SecuritySession session, String permission )
        throws AuthorizationException;

    /**
     * return AuthorizationResult without changing authorization
     * @param session
     * @param permission
     * @param resource
     * @return
     * @throws AuthorizationException
     */
    AuthorizationResult authorize( SecuritySession session, String permission, String resource )
        throws AuthorizationException;

    boolean isAuthorized( SecuritySession session, String permission, String resource )
        throws AuthorizationException;

    // ----------------------------------------------------------------------------
    // User Management
    // ----------------------------------------------------------------------------

    UserManager getUserManager();
    
    // ----------------------------------------------------------------------------
    // Key Management
    // ----------------------------------------------------------------------------
    
    KeyManager getKeyManager();

    // ----------------------------------------------------------------------------
    // Policy Management
    // ----------------------------------------------------------------------------
    
    UserSecurityPolicy getPolicy();

    String getUserManagementId();
    String getAuthenticatorId();
    String getAuthorizerId();

    /**
     * @since 2.1
     * @return is it possible to modify user datas (some userManager cannot i.e ldap)
     */
    boolean userManagerReadOnly();
}

