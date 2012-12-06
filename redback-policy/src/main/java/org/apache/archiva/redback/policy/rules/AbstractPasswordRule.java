package org.apache.archiva.redback.policy.rules;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.policy.PasswordRule;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * AbstractPasswordRule
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public abstract class AbstractPasswordRule
    implements PasswordRule
{
    protected boolean enabled = true;

    @Inject @Named (value="userConfiguration#default")
    protected UserConfiguration config;

    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * true if the security policy is required for the rule to execute
     *
     * @return boolean
     */
    public boolean requiresSecurityPolicy()
    {
        return false;
    }
}
