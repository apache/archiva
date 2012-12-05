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
    static final String USER_MANAGER_IMPL = "user.manager.impl";

    static final String DEFAULT_ADMIN = "redback.default.admin";

    static final String EMAIL_FROM_ADDRESS = "email.from.address";

    static final String EMAIL_FROM_NAME = "email.from.name";

    static final String EMAIL_FEEDBACK_PATH = "email.feedback.path";

    static final String APPLICATION_TIMESTAMP = "application.timestamp";

    static final String PASSWORD_ENCODER = "security.policy.password.encoder";


    static final String EMAIL_VALIDATION_SUBJECT = "email.validation.subject";

    static final String REMEMBER_ME_PATH = "security.rememberme.path";

    static final String REMEMBER_ME_DOMAIN = "security.rememberme.domain";

    static final String SIGNON_DOMAIN = "security.signon.domain";

    static final String SIGNON_PATH = "security.signon.path";

    static final String USER_MANAGER_IMPL = "user.manager.impl";
}
