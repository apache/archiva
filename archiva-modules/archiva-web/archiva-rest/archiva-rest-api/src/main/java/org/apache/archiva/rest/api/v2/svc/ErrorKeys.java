package org.apache.archiva.rest.api.v2.svc;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface ErrorKeys
{

    String PREFIX = "archiva.";

    String VALIDATION_ERROR = PREFIX + "validation_error";

    String REPOSITORY_GROUP_PREFIX = PREFIX + "repository_group.";
    String REPOSITORY_PREFIX = PREFIX + "repository.";

    String INVALID_RESULT_SET_ERROR = "archiva.result_set.invalid";
    String REPOSITORY_ADMIN_ERROR = "archiva.repositoryadmin.error";
    String LDAP_CF_INIT_FAILED = "archiva.ldap.cf.init.failed";
    String LDAP_USER_MAPPER_INIT_FAILED = "archiva.ldap.usermapper.init.failed";
    String LDAP_COMMUNICATION_ERROR = "archiva.ldap.communication_error";
    String LDAP_INVALID_NAME = "archiva.ldap.invalid_name";
    String LDAP_GENERIC_ERROR = "archiva.ldap.generic_error";
    String LDAP_SERVICE_UNAVAILABLE = "archiva.ldap.service_unavailable";
    String LDAP_SERVICE_AUTHENTICATION_FAILED = "archiva.ldap.authentication.failed";
    String LDAP_SERVICE_AUTHENTICATION_NOT_SUPPORTED = "archiva.ldap.authentication.not_supported";
    String LDAP_SERVICE_NO_PERMISSION = "archiva.ldap.no_permissions";

    String PROPERTY_NOT_FOUND = "archiva.property.not.found";

    String MISSING_DATA = "archiva.missing.data";

    String REPOSITORY_GROUP_NOT_FOUND = REPOSITORY_GROUP_PREFIX+"notfound";
    String REPOSITORY_GROUP_ADD_FAILED = REPOSITORY_GROUP_PREFIX+"add.failed"  ;
    String REPOSITORY_GROUP_EXIST = REPOSITORY_GROUP_PREFIX+"exists";
    String REPOSITORY_GROUP_UPDATE_FAILED = REPOSITORY_GROUP_PREFIX + "update.failed";

    String REPOSITORY_GROUP_DELETE_FAILED = REPOSITORY_GROUP_PREFIX + "delete.failed";
    String REPOSITORY_NOT_FOUND = REPOSITORY_PREFIX + "notfound";
    String REPOSITORY_MANAGED_NOT_FOUND = REPOSITORY_PREFIX + "managed.notfound";
    String REPOSITORY_REMOTE_NOT_FOUND = REPOSITORY_PREFIX + "remote.notfound";

    String REPOSITORY_METADATA_ERROR = REPOSITORY_PREFIX + "metadata_error";

    String TASK_QUEUE_FAILED = PREFIX + "task.queue_failed";
    String REPOSITORY_SCAN_FAILED = REPOSITORY_PREFIX + "scan.failed";
    String ARTIFACT_EXISTS_AT_DEST = REPOSITORY_PREFIX + "artifact.dest.exists";
    String REPOSITORY_REMOTE_INDEX_DOWNLOAD_FAILED = REPOSITORY_PREFIX + "remote.index.download_failed";
    String REPOSITORY_WRONG_TYPE = REPOSITORY_PREFIX + "wrong_type";
    String REPOSITORY_DELETE_FAILED = REPOSITORY_PREFIX + "delete.failed";
    String REPOSITORY_INVALID_ID = REPOSITORY_PREFIX + "invalid.id";
    String REPOSITORY_ID_EXISTS = REPOSITORY_PREFIX + "id.exists";
    String REPOSITORY_UPDATE_FAILED = REPOSITORY_PREFIX + "update.failed";
    String ARTIFACT_NOT_FOUND = REPOSITORY_PREFIX + "artifact.notfound";
    String REPOSITORY_LAYOUT_ERROR = REPOSITORY_PREFIX + "layout.error";

    String ARTIFACT_COPY_ERROR = REPOSITORY_PREFIX + "artifact.copy.error";

    /**
     * The given user was not found
     * Parameters:
     * - User Id
     */
    String USER_NOT_FOUND = PREFIX+"user.not_found";

    /**
     * Error from UserManager
     * Parameters:
     * - Error Message
     */
    String USER_MANAGER_ERROR = PREFIX+"user_manager.error";

    /**
     * Permission to the repository denied.
     * Parameters:
     * - Repository Id
     * - Permission ID
     */
    String PERMISSION_REPOSITORY_DENIED = PREFIX + "permission.repository.denied";

    /**
     * A generic authorization error thrown during the authorization check.
     * Parameters:
     * - Error message
     */
    String AUTHORIZATION_ERROR = PREFIX + "authorization.error";

    /**
     * When the operation needs authentication, but not authenticated user was found in the request context.
     */
    String NOT_AUTHENTICATED = PREFIX + "user.not_authenticated";

    /**
     * Repository add action failed
     */
    String REPOSITORY_ADD_FAILED = PREFIX + "add.failed";
    
}
