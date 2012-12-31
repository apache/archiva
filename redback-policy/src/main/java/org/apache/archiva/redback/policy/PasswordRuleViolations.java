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

import org.apache.archiva.redback.users.Messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Password Rule Violations
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class PasswordRuleViolations
{
    private List<MessageReference> violations;

    public static final class MessageReference
    {
        String key;

        String[] args;

        public String getKey()
        {
            return key;
        }

        public String[] getArgs()
        {
            return args;
        }
    }

    /**
     * Construct a Password Rule Violations object.
     */
    public PasswordRuleViolations()
    {
        violations = new ArrayList<MessageReference>( 0 );
    }

    /**
     * Empty out the list of violations.
     */
    public void reset()
    {
        violations.clear();
    }

    /**
     * Add a violation to the underlying list.
     *
     * @param key the bundle/localization key for the message.
     */
    public void addViolation( String key )
    {
        addViolation( key, null );
    }

    /**
     * Add a violation to the underlying list.
     *
     * @param key  the bundle/localization key for the message.
     * @param args the arguments for the message.
     */
    public void addViolation( String key, String[] args )
    {
        MessageReference mesgref = new MessageReference();
        mesgref.key = key;
        mesgref.args = args;
        violations.add( mesgref );
    }

    /**
     * Get the List of Violations as localized and post-processed {@link String}s.
     *
     * @return the List of {@link String} objects.
     */
    public List<String> getLocalizedViolations()
    {
        List<String> msgs = new ArrayList<String>( violations.size() );

        for ( MessageReference msgref : violations )
        {
            msgs.add( Messages.getString( msgref.key, msgref.args ) );
        }

        return msgs;
    }

    /**
     * Simple test to see if there are any violations.
     *
     * @return true if there are any violations.
     */
    public boolean hasViolations()
    {
        return !violations.isEmpty();
    }

    public List<MessageReference> getViolations()
    {
        return violations;
    }
}
