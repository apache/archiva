package org.codehaus.plexus.redback.policy.rules;

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

import org.codehaus.plexus.redback.policy.PasswordRuleViolations;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.users.User;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Basic Password Rule. Checks that password only contains alpha-numeric characters.
 *
 * $Id$
 */
@Service("passwordRule#alpha-numeric")
public class AlphaNumericPasswordRule
    extends AbstractPasswordRule
{
    public static final String ALPHANUM_VIOLATION = "user.password.violation.alphanum.only";

    public void setUserSecurityPolicy( UserSecurityPolicy policy )
    {
        // Ignore, policy not needed in this rule.
    }

    public void testPassword( PasswordRuleViolations violations, User user )
    {
        char[] password = user.getPassword().toCharArray();

        for ( int i = 0; i < password.length; i++ )
        {
            if ( !Character.isLetterOrDigit( password[i] ) )
            {
                violations.addViolation( ALPHANUM_VIOLATION );
                return;
            }
        }
    }

    // FIXME to constructor
    @PostConstruct
    public void initialize()
    {
        enabled = config.getBoolean( "security.policy.password.rule.alphanumeric.enabled" );
    }
}
