package org.apache.archiva.rest.services.v2;/*
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

import org.apache.archiva.rest.api.services.v2.ErrorMessage;

import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface ErrorKeys
{

    String PREFIX = "archiva.";
    String REPOSITORY_GROUP_PREFIX = PREFIX + "repository_group.";

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

    String REPOSITORY_GROUP_NOT_EXIST = REPOSITORY_GROUP_PREFIX+"notexist";
    String REPOSITORY_GROUP_ADD_FAILED = REPOSITORY_GROUP_PREFIX+"add.failed"  ;
    String REPOSITORY_GROUP_EXIST = REPOSITORY_GROUP_PREFIX+"exists";

}
