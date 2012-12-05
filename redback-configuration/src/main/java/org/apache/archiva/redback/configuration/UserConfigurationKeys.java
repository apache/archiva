package org.apache.archiva.redback.configuration;
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

/**
 * @author Olivier Lamy
 * @since 2.1
 */
public interface UserConfigurationKeys
{
    String USER_MANAGER_IMPL = "user.manager.impl";

    String DEFAULT_ADMIN = "redback.default.admin";

    String EMAIL_FROM_ADDRESS = "email.from.address";

    String EMAIL_FROM_NAME = "email.from.name";

    String EMAIL_FEEDBACK_PATH = "email.feedback.path";

    String APPLICATION_TIMESTAMP = "application.timestamp";

    String PASSWORD_ENCODER = "security.policy.password.encoder";


    String EMAIL_VALIDATION_SUBJECT = "email.validation.subject";

    String REMEMBER_ME_PATH = "security.rememberme.path";

    String REMEMBER_ME_DOMAIN = "security.rememberme.domain";

    String SIGNON_DOMAIN = "security.signon.domain";

    String SIGNON_PATH = "security.signon.path";

    String LDAP_HOSTNAME = "ldap.config.hostname";

    String LDAP_CONTEX_FACTORY = "ldap.config.context.factory";

    String LDAP_PASSWORD = "ldap.config.password";

    String LDAP_AUTHENTICATION_METHOD = "ldap.config.authentication.method";

    String APPLICATION_URL = "application.url";

    String EMAIL_URL_PATH = "email.url.path";

    String LDAP_MAPPER_ATTRIBUTE_EMAIL = "ldap.config.mapper.attribute.email";

    String LDAP_MAPPER_ATTRIBUTE_FULLNAME = "ldap.config.mapper.attribute.fullname";

    String LDAP_MAPPER_ATTRIBUTE_PASSWORD = "ldap.config.mapper.attribute.password";

    String LDAP_MAPPER_ATTRIBUTE_ID = "ldap.config.mapper.attribute.user.id";

    String LDAP_MAPPER_ATTRIBUTE_OBJECT_CLASS =  "ldap.config.mapper.attribute.user.object.class";

    String LDAP_MAPPER_ATTRIBUTE_FILTER = "ldap.config.mapper.attribute.user.filter";
}
