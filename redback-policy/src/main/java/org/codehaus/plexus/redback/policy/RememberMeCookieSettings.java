package org.codehaus.plexus.redback.policy;

/*
 * Copyright 2001-2006 The Codehaus.
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

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * RememberMeCookieSettings
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("cookieSettings#rememberMe")
public class RememberMeCookieSettings
    extends AbstractCookieSettings
{
    private boolean enabled;

    public boolean isEnabled()
    {
        return enabled;
    }

    @PostConstruct
    public void initialize()
    {
        this.cookieTimeout = config.getInt( "security.rememberme.timeout" );
        this.domain = config.getString( "security.rememberme.domain" );
        this.path = config.getString( "security.rememberme.path" );
        this.enabled = config.getBoolean( "security.rememberme.enabled" );
    }
}
