package org.codehaus.plexus.redback.policy;

/*
 * Copyright 2006-2007 The Codehaus.
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
 * SignonCookieSettings
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("cookieSettings#signon")
public class SignonCookieSettings
    extends AbstractCookieSettings
{
    @PostConstruct
    public void initialize()
    {
        // cookie timeouts in the configuration settings is labeled to be in minutes, so adjust to minutes
        cookieTimeout = config.getInt( "security.signon.timeout" ) * 60;
        domain = config.getString( "security.signon.domain" );
        path = config.getString( "security.signon.path" );
    }

    public boolean isEnabled()
    {
        return true;
    }
}
