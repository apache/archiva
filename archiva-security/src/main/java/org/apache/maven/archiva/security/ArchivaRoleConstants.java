package org.apache.maven.archiva.security;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

public class ArchivaRoleConstants
{
    // globalish roles
    public static final String SYSTEM_ADMINISTRATOR_ROLE = "System Administrator";
    public static final String USER_ADMINISTRATOR_ROLE = "User Administrator";
    public static final String REGISTERED_USER_ROLE = "Registered User";
    public static final String GUEST_ROLE = "Guest";

    // operations
    public static final String OPERATION_MANAGE_USERS = "archiva-manage-users";
    public static final String OPERATION_MANAGE_CONFIGURATION = "archiva-manage-configuration";
    public static final String OPERATION_ACTIVE_GUEST = "archiva-guest";
}
