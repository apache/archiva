package org.codehaus.plexus.redback.policy.encoders;

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

import org.codehaus.plexus.redback.policy.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * PlainText PasswordEncoder for use in situtations where the password needs to be saved as-is.
 * See {@link PasswordEncoder#encodePassword(String)} for details.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("passwordEncoder#plaintext")
public class PlainTextPasswordEncoder
    implements PasswordEncoder
{

    public String encodePassword( String rawPass )
    {
        return rawPass;
    }

    public String encodePassword( String rawPass, Object salt )
    {
        return rawPass;
    }

    public boolean isPasswordValid( String encPass, String rawPass )
    {
        if ( encPass == null && rawPass != null )
        {
            return false;
        }

        return encPass.equals( rawPass );
    }

    public boolean isPasswordValid( String encPass, String rawPass, Object salt )
    {
        return isPasswordValid( encPass, rawPass );
    }

    public void setSystemSalt( Object salt )
    {
        // Ignore, not used in this plaintext encoder.
    }
}
