package org.codehaus.redback.jsecurity;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.SimpleAuthenticationInfo;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.authc.credential.CredentialsMatcher;
import org.jsecurity.authz.AuthorizationInfo;
import org.jsecurity.authz.SimpleAuthorizationInfo;
import org.jsecurity.realm.AuthorizingRealm;
import org.jsecurity.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedbackRealm extends AuthorizingRealm
{
    private Logger log = LoggerFactory.getLogger(RedbackRealm.class);

    private final UserManager userManager;

    private final RBACManager rbacManager;

    private final UserSecurityPolicy securityPolicy;

    public RedbackRealm(UserManager userManager, RBACManager rbacManager, UserSecurityPolicy securityPolicy)
    {
        this.userManager = userManager;
        this.rbacManager = rbacManager;
        this.securityPolicy = securityPolicy;
    }
    
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)
    {
        final String username = (String) principals.fromRealm(getName()).iterator().next();

        try
        {
            final UserAssignment assignment = rbacManager.getUserAssignment(username);
            final Set<String> roleNames = new HashSet<String>(assignment.getRoleNames());
            final Set<String> permissions = new HashSet<String>();

            for (Iterator<Permission> it = rbacManager.getAssignedPermissions(username).iterator(); it.hasNext();)
            {
                Permission permission = it.next();
                permissions.add(permission.getName());
            }

            SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(roleNames);
            authorizationInfo.setStringPermissions(permissions);

            return authorizationInfo;
        }
        catch (RbacManagerException e)
        {
            log.error("Could not authenticate against data source", e);
        }
        
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
        throws AuthenticationException
    {
        if (token == null)
        {
            throw new AuthenticationException("AuthenticationToken cannot be null");
        }
        
        final UsernamePasswordToken passwordToken = (UsernamePasswordToken)token;

        User user = null;
        try
        {
            user = userManager.findUser(passwordToken.getUsername());
        }
        catch (UserNotFoundException e)
        {
            log.error("Could not find user " + passwordToken.getUsername());
        }

        if (user == null)
        {
            return null;
        }

        if ( user.isLocked() && !user.isPasswordChangeRequired() )
        {
            throw new PrincipalLockedException("User " + user.getPrincipal() + " is locked.");
        }

        if ( user.isPasswordChangeRequired() )
        {
            throw new PrincipalPasswordChangeRequiredException("Password change is required for user " + user.getPrincipal());
        }

        return new RedbackAuthenticationInfo(user, getName());
    }

    @Override
    public CredentialsMatcher getCredentialsMatcher()
    {
        return new CredentialsMatcher()
        {
            public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info)
            {
                final String credentials = new String((char[])token.getCredentials());
                final boolean match = securityPolicy.getPasswordEncoder().encodePassword(credentials).equals((String)info.getCredentials());
                if (!match)
                {
                    User user = ((RedbackAuthenticationInfo)info).getUser();
                    try
                    {
                        securityPolicy.extensionExcessiveLoginAttempts( user );
                    }
                    catch (AccountLockedException e)
                    {
                        log.info("User{} has been locked", user.getUsername(), e);
                    }
                    finally
                    {
                        try
                        {
                            userManager.updateUser( user );
                        }
                        catch (UserNotFoundException e)
                        {
                            log.error("The user to be updated could not be found", e);
                        }
                    }
                }
                return match;
            }
        };
    }

    final class RedbackAuthenticationInfo extends SimpleAuthenticationInfo
    {
        private final User user;

        public RedbackAuthenticationInfo(User user, String realmName)
        {
            super(user.getPrincipal(), user.getEncodedPassword(), realmName);
            this.user = user;
        }

        public User getUser()
        {
            return user;
        }
    }
}
