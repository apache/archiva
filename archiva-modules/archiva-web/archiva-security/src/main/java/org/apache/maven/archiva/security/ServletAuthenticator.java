package org.apache.maven.archiva.security;

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

import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;

/**
 * @version
 */
public interface ServletAuthenticator
{
    /**
     * Authentication check for users.
     * 
     * @param request
     * @param result
     * @return
     * @throws AuthenticationException
     * @throws AccountLockedException
     * @throws MustChangePasswordException
     */
    public boolean isAuthenticated( HttpServletRequest request, AuthenticationResult result )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException;

    /**
     * Authorization check for valid users.
     * 
     * @param request
     * @param securitySession
     * @param repositoryId
     * @param isWriteRequest
     * @return
     * @throws AuthorizationException
     * @throws UnauthorizedException
     */
    public boolean isAuthorized( HttpServletRequest request, SecuritySession securitySession, String repositoryId,
        String permission ) throws AuthorizationException, UnauthorizedException;
    
    /**
     * Authorization check specific for user guest, which doesn't go through 
     * HttpBasicAuthentication#getAuthenticationResult( HttpServletRequest request, HttpServletResponse response )
     * since no credentials are attached to the request. 
     * 
     * See also MRM-911
     * 
     * @param principal
     * @param repoId
     * @param isWriteRequest
     * @return
     * @throws UnauthorizedException
     */
    public boolean isAuthorized( String principal, String repoId, String permission )
        throws UnauthorizedException;
}
