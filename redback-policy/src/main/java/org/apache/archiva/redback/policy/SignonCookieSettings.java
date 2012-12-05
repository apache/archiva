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

import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.policy.AbstractCookieSettings;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * SignonCookieSettings
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service( "cookieSettings#signon" )
public class SignonCookieSettings
    extends AbstractCookieSettings
{
    @PostConstruct
    public void initialize()
    {
        // cookie timeouts in the configuration settings is labeled to be in minutes, so adjust to minutes
        cookieTimeout = config.getInt( UserConfigurationKeys.SIGNON_TIMEOUT ) * 60;
        domain = config.getString( UserConfigurationKeys.SIGNON_DOMAIN );
        path = config.getString( UserConfigurationKeys.SIGNON_PATH );
    }

    public boolean isEnabled()
    {
        return true;
    }
}
