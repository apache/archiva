package org.apache.archiva.redback.rest.services;

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
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.rest.api.services.UserService;

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
