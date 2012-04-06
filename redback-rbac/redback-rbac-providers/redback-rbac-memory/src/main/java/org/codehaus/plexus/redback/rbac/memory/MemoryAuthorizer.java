package org.codehaus.plexus.redback.rbac.memory;

/*
 * Copyright 2005 The Codehaus.
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

import org.codehaus.plexus.redback.authorization.AuthorizationDataSource;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.authorization.Authorizer;
import org.springframework.stereotype.Service;

/**
 * @author: Jesse McConnell <jesse@codehaus.org>
 * @version: $Id$
  */
@Service("authorizer#memory")
public class MemoryAuthorizer
    implements Authorizer
{
    public String getId()
    {
        return MemoryAuthorizer.class.getName();
    }

    public AuthorizationResult isAuthorized( AuthorizationDataSource source )
        throws AuthorizationException
    {
        Object principal = source.getPrincipal();

        Object permission = source.getPermission();

        // TODO: Actually use a real permission!
        if ( "foo".equals( permission.toString() ) )
        {
            return new AuthorizationResult( true, principal, null );
        }
        else
        {
            return new AuthorizationResult( false, principal, null );
        }
    }
}

