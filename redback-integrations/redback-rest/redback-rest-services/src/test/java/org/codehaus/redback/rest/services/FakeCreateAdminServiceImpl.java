package org.codehaus.redback.rest.services;

/*
 * Copyright 2011 The Codehaus.
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

import org.codehaus.plexus.redback.configuration.UserConfiguration;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.redback.integration.security.role.RedbackRoleConstants;
import org.codehaus.redback.rest.api.services.UserService;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 */
//Service( "fakeCreateAdminService" )
public class FakeCreateAdminServiceImpl
    implements FakeCreateAdminService
{
    @Inject
    @Named( value = "rBACManager#jdo" )
    private RBACManager rbacManager;

    @Inject
    @Named( value = "userManager#jdo" )
    private UserManager userManager;

    @Inject
    private UserConfiguration config;

    @Inject
    private RoleManager roleManager;

    @Inject
    private UserService userService;

    public Boolean testAuthzWithoutKarmasNeededButAuthz()
    {
        return Boolean.TRUE;
    }
}
