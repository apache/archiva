package org.apache.archiva.redback.policy;

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

import org.apache.archiva.redback.users.User;

/**
 * A Password Rule
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public interface PasswordRule
{

    /**
     * Tests if rule is enabled (or not)
     */
    boolean isEnabled();
    
    /**
     * Sets the User Security Policy to use.
     * 
     * The policy is set once per instance of a PasswordRule object.
     * 
     * @param policy the policy to use.
     */
    void setUserSecurityPolicy(UserSecurityPolicy policy);


    /**
     * true if the security policy has been set on the rule
     *
     * @return boolean
     */
    boolean requiresSecurityPolicy();

    /**
     * Tests the {@link User#getPassword()} for a valid password, based on rule.
     * 
     * @param violations the place to add any password rule violations that this rule has discovered.
     * @param user the User to test.    
     */
    void testPassword( PasswordRuleViolations violations, User user );

    /**
     * to use in case of configuration change.
     */
    void initialize();
}
