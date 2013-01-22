package org.apache.archiva.redback.integration.checks.security;

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

import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.apache.archiva.redback.users.UserManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * RequiredRolesEnvironmentCheck:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@Service("environmentCheck#guest-user-check")
public class GuestUserEnvironmentCheck
    implements EnvironmentCheck
{

    @Inject
    private RoleManager roleManager;

    @Inject
    private SecuritySystem securitySystem;

    @Inject
    @Named(value = "userConfiguration#default")
    private UserConfiguration config;

    /**
     * boolean detailing if this environment check has been executed
     */
    private boolean checked = false;

    /**
     * @param violations
     */
    public void validateEnvironment( List<String> violations )
    {
        if ( !checked )
        {
            UserManager userManager = securitySystem.getUserManager();
            UserSecurityPolicy policy = securitySystem.getPolicy();

            User guest = null;
            try
            {
                guest = userManager.getGuestUser();
            }
            catch ( UserManagerException e )
            {
                policy.setEnabled( false );
                try
                {
                    guest = userManager.createGuestUser();
                }
                catch ( UserManagerException ume )
                {
                    violations.add( "unable to initialize guest user properly: " + ume.getMessage() );
                    checked = true;
                    return;
                }
                policy.setEnabled( true );
            }

            if ( guest != null )
            {

                try
                {
                    roleManager.assignRole( config.getString( UserConfigurationKeys.DEFAULT_GUEST_ROLE_ID, "guest" ),
                                            guest.getUsername() );
                }
                catch ( RoleManagerException rpe )
                {
                    violations.add( "unable to initialize guest user properly: " + rpe.getMessage() );
                }
            }
            else
            {
                violations.add( "cannot find neither create guest user" );
            }
            checked = true;
        }
    }
}
