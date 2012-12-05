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
import java.util.Iterator;

/**
 * Password Rule, Checks supplied password found at {@link User#getPassword()} against
 * the {@link User#getPreviousEncodedPasswords()} to ensure that a password is not reused.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service("passwordRule#reuse")
public class ReusePasswordRule
    extends AbstractPasswordRule
{
    public static final String REUSE_VIOLATION = "user.password.violation.reuse";

    private UserSecurityPolicy securityPolicy;

    public void setUserSecurityPolicy( UserSecurityPolicy policy )
    {
        this.securityPolicy = policy;
    }

    /**
     * true if the security policy is required for this rule
     *
     * @return boolean
     */
    public boolean requiresSecurityPolicy()
    {
        return true;
    }

    public int getPreviousPasswordCount()
    {
        if ( securityPolicy == null )
        {
            throw new IllegalStateException( "The security policy has not yet been set." );
        }

        return securityPolicy.getPreviousPasswordsCount();
    }

    private boolean hasReusedPassword( User user, String password )
    {
        if ( securityPolicy == null )
        {
            throw new IllegalStateException( "The security policy has not yet been set." );
        }

        if ( StringUtils.isEmpty( password ) )
        {
            return false;
        }

        String encodedPassword = securityPolicy.getPasswordEncoder().encodePassword( password );

        int checkCount = getPreviousPasswordCount();

        Iterator<String> it = user.getPreviousEncodedPasswords().iterator();

        while ( it.hasNext() && checkCount >= 0 )
        {
            String prevEncodedPassword = it.next();
            if ( encodedPassword.equals( prevEncodedPassword ) )
            {
                return true;
            }
            checkCount--;
        }

        return false;
    }

    public void setPreviousPasswordCount( int previousPasswordCount )
    {
        securityPolicy.setPreviousPasswordsCount( previousPasswordCount );
    }

    public void testPassword( PasswordRuleViolations violations, User user )
    {
        String password = user.getPassword();

        if ( hasReusedPassword( user, password ) )
        {
            violations.addViolation( REUSE_VIOLATION,
                                     new String[]{ String.valueOf( getPreviousPasswordCount() ) } ); //$NON-NLS-1$
        }
    }

    @PostConstruct
    public void initialize()
    {
        enabled = config.getBoolean( UserConfigurationKeys.POLICY_PASSWORD_RULE_REUSE_ENABLED );
    }
}
