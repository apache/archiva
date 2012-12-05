package org.apache.archiva.redback.policy;

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
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * DefaultUserValidationSettings
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service( "userValidationSettings" )
public class DefaultUserValidationSettings
    implements UserValidationSettings
{
    @Resource( name = "userConfiguration" )
    private UserConfiguration config;

    private boolean emailValidationRequired;

    private int emailValidationTimeout;

    private String emailSubject;

    public boolean isEmailValidationRequired()
    {
        return emailValidationRequired;
    }

    public int getEmailValidationTimeout()
    {
        return emailValidationTimeout;
    }

    public String getEmailSubject()
    {
        return emailSubject;
    }

    @PostConstruct
    public void initialize()
    {
        this.emailValidationRequired = config.getBoolean( "email.validation.required" );
        this.emailValidationTimeout = config.getInt( "email.validation.timeout" );
        this.emailSubject = config.getString( UserConfigurationKeys.EMAIL_VALIDATION_SUBJECT );
    }
}
