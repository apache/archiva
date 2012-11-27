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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.archiva.redback.configuration.DefaultUserConfiguration;

/**
 * Base class for cookie settings. These will only differ by their configuration keys.
 *
 * @todo not sure if having the domain and path in the general configuration is a good idea - this is probably something
 * customised once for all cookies and applications. Should it be in a sharead configuration file, under a sharead key,
 * or perhaps even configured at the application server level? (ie, in Naming).
 */
public abstract class AbstractCookieSettings
    implements CookieSettings
{
    @Inject @Named(value="userConfiguration")
    protected DefaultUserConfiguration config;

    /**
     * Timeout (in minutes) for the sign on cookie.
     */
    protected int cookieTimeout;

    /**
     * The domain for the cookie.
     */
    protected String domain;

    /**
     * The path for the cookie.
     */
    protected String path;

    public int getCookieTimeout()
    {
        return cookieTimeout;
    }

    public String getDomain()
    {
        return domain;
    }

    public String getPath()
    {
        return path;
    }

}
