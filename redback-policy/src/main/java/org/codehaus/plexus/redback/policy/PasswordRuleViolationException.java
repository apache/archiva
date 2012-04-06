package org.codehaus.plexus.redback.policy;

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

/**
 * Password Rule Violations Exception
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PasswordRuleViolationException
    extends RuntimeException
{
    private static final long serialVersionUID = -4686338829234880328L;

    private PasswordRuleViolations violations;

    public PasswordRuleViolationException()
    {
    }

    public PasswordRuleViolationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public PasswordRuleViolationException( String message )
    {
        super( message );
    }

    public PasswordRuleViolationException( Throwable cause )
    {
        super( cause );
    }

    public PasswordRuleViolations getViolations()
    {
        return violations;
    }

    public void setViolations( PasswordRuleViolations violations )
    {
        this.violations = violations;
    }
}
