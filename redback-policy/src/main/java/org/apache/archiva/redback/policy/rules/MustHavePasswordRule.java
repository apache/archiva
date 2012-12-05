package org.apache.archiva.redback.policy.rules;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Basic Password Rule, Checks for non-empty Passwords in non guest users.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service("passwordRule#must-have")
public class MustHavePasswordRule
    extends AbstractPasswordRule
{
    public static final String MISSING_PASSWORD_VIOLATION = "user.password.violation.missing";

    public void setUserSecurityPolicy( UserSecurityPolicy policy )
    {
        // Ignore, policy not needed in this rule.
    }

    public void testPassword( PasswordRuleViolations violations, User user )
    {
        if ( StringUtils.isEmpty( user.getPassword() ) )
        {
            violations.addViolation( MISSING_PASSWORD_VIOLATION ); //$NON-NLS-1$
        }
    }

    @PostConstruct
    public void initialize()
    {
        enabled = config.getBoolean( UserConfigurationKeys.POLICY_PASSWORD_RULE_MUSTHAVE_ENABLED );
    }
}
