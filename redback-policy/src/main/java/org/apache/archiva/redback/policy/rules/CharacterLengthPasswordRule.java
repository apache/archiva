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

import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Basic Password Rule, Checks for non-empty passwords that have between {@link #setMinimumCharacters(int)} and
 * {@link #setMaximumCharacters(int)} characters in length.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service( "passwordRule#character-length" )
public class CharacterLengthPasswordRule
    extends AbstractPasswordRule
{

    public static final int DEFAULT_CHARACTER_LENGTH_MAX = 8;

    private int minimumCharacters;

    private int maximumCharacters;

    public int getMaximumCharacters()
    {
        return maximumCharacters;
    }

    public int getMinimumCharacters()
    {
        return minimumCharacters;
    }

    public void setMaximumCharacters( int maximumCharacters )
    {
        this.maximumCharacters = maximumCharacters;
    }

    public void setMinimumCharacters( int minimumCharacters )
    {
        this.minimumCharacters = minimumCharacters;
    }

    public void setUserSecurityPolicy( UserSecurityPolicy policy )
    {
        // Ignore, policy not needed in this rule.
    }

    public void testPassword( PasswordRuleViolations violations, User user )
    {
        if ( minimumCharacters > maximumCharacters )
        {
            /* this should caught up front during the configuration of the component */
            // TODO: Throw runtime exception instead?
            violations.addViolation( UserConfigurationKeys.CHARACTER_LENGTH_MISCONFIGURED_VIOLATION,
                                     new String[]{ String.valueOf( minimumCharacters ),
                                         String.valueOf( maximumCharacters ) } ); //$NON-NLS-1$
        }

        String password = user.getPassword();

        if ( StringUtils.isEmpty( password ) || password.length() < minimumCharacters ||
            password.length() > maximumCharacters )
        {
            violations.addViolation( UserConfigurationKeys.CHARACTER_LENGTH_VIOLATION,
                                     new String[]{ String.valueOf( minimumCharacters ),
                                         String.valueOf( maximumCharacters ) } ); //$NON-NLS-1$
        }
    }

    @PostConstruct
    public void initialize()
    {
        enabled = config.getBoolean( UserConfigurationKeys.POLICY_PASSWORD_RULE_CHARACTERLENGTH_ENABLED );
        this.minimumCharacters = config.getInt( UserConfigurationKeys.CHARACTER_LENGTH_MIN );
        this.maximumCharacters = config.getInt( UserConfigurationKeys.CHARACTER_LENGTH_MAX );
    }
}
