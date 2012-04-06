package org.codehaus.redback.integration.role;

/*
 * Copyright 2005-2006 The Codehaus.
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

import org.codehaus.redback.integration.security.role.RedbackRoleConstants;

/**
 * RoleConstants:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 * @version: $Id$
 * @deprecated use {@link RedbackRoleConstants}
 */
public class RoleConstants
{
    public static final String ADMINISTRATOR_ACCOUNT_NAME = RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME;

    // roles
    public static final String SYSTEM_ADMINISTRATOR_ROLE = RedbackRoleConstants.SYSTEM_ADMINISTRATOR_ROLE;

    public static final String USER_ADMINISTRATOR_ROLE = RedbackRoleConstants.USER_ADMINISTRATOR_ROLE;

    public static final String REGISTERED_USER_ROLE = RedbackRoleConstants.REGISTERED_USER_ROLE;

    public static final String GUEST_ROLE = RedbackRoleConstants.GUEST_ROLE;

    // guest access operation
    public static final String GUEST_ACCESS_OPERATION = RedbackRoleConstants.GUEST_ACCESS_OPERATION;

    // operations against configuration
    public static final String CONFIGURATION_EDIT_OPERATION = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION;

    // operations against user
    public static final String USER_MANAGEMENT_USER_CREATE_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION;

    public static final String USER_MANAGEMENT_USER_EDIT_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION;

    public static final String USER_MANAGEMENT_USER_ROLE_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_USER_ROLE_OPERATION;

    public static final String USER_MANAGEMENT_USER_DELETE_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION;

    public static final String USER_MANAGEMENT_USER_LIST_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION;

    // operations against user assignment.
    public static final String USER_MANAGEMENT_ROLE_GRANT_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_ROLE_GRANT_OPERATION;

    public static final String USER_MANAGEMENT_ROLE_DROP_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_ROLE_DROP_OPERATION;

    // operations against rbac objects.
    public static final String USER_MANAGEMENT_RBAC_ADMIN_OPERATION =
        RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION;

    public static final String USER_MANAGEMENT_MANAGE_DATA = RedbackRoleConstants.USER_MANAGEMENT_MANAGE_DATA;
}
