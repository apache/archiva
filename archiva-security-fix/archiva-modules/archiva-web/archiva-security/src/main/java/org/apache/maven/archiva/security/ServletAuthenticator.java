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
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public interface ServletAuthenticator
{
    public boolean isAuthenticated( HttpServletRequest request, AuthenticationResult result )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException;

    public boolean isAuthorized( HttpServletRequest request, SecuritySession securitySession, String repositoryId,
        boolean isWriteRequest ) throws AuthorizationException, UnauthorizedException;
    
    public boolean isAuthorized( String principal, String repoId, boolean isWriteRequest )
        throws UnauthorizedException;
}
