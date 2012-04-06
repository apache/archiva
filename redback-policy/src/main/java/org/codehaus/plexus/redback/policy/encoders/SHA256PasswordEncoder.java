package org.codehaus.plexus.redback.policy.encoders;

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

import org.codehaus.plexus.redback.policy.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * SHA-256 Password Encoder.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("passwordEncoder#sha256")
public class SHA256PasswordEncoder
    extends AbstractJAASPasswordEncoder
    implements PasswordEncoder
{
    public SHA256PasswordEncoder()
    {
        super( "SHA-256" );
    }
}
