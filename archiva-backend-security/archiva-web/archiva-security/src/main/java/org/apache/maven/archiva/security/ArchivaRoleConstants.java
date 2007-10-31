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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

public class ArchivaRoleConstants
{
    public static final String DELIMITER = " - ";

    // globalish roles
    public static final String SYSTEM_ADMINISTRATOR_ROLE = "System Administrator";

    public static final String USER_ADMINISTRATOR_ROLE = "User Administrator";
    
    public static final String GLOBAL_REPOSITORY_MANAGER_ROLE = "Global Repository Manager";

    public static final String GLOBAL_REPOSITORY_OBSERVER_ROLE = "Global Repository Observer";
    
    public static final String REGISTERED_USER_ROLE = "Registered User";

    public static final String GUEST_ROLE = "Guest";

    // dynamic role prefixes
    public static final String REPOSITORY_MANAGER_ROLE_PREFIX = "Repository Manager";

    public static final String REPOSITORY_OBSERVER_ROLE_PREFIX = "Repository Observer";

    // operations
    public static final String OPERATION_MANAGE_USERS = "archiva-manage-users";

    public static final String OPERATION_MANAGE_CONFIGURATION = "archiva-manage-configuration";

    public static final String OPERATION_ACTIVE_GUEST = "archiva-guest";

    public static final String OPERATION_RUN_INDEXER = "archiva-run-indexer";

    public static final String OPERATION_REGENERATE_INDEX = "archiva-regenerate-index";

    public static final String OPERATION_ACCESS_REPORT = "archiva-access-reports";

    public static final String OPERATION_ADD_REPOSITORY = "archiva-add-repository";

    public static final String OPERATION_REPOSITORY_ACCESS = "archiva-read-repository";

    public static final String OPERATION_DELETE_REPOSITORY = "archiva-delete-repository";

    public static final String OPERATION_EDIT_REPOSITORY = "archiva-edit-repository";

    public static final String OPERATION_REPOSITORY_UPLOAD = "archiva-upload-repository";

    // Role templates
    public static final String TEMPLATE_REPOSITORY_MANAGER = "archiva-repository-manager";
    
    public static final String TEMPLATE_REPOSITORY_OBSERVER = "archiva-repository-observer";
}
