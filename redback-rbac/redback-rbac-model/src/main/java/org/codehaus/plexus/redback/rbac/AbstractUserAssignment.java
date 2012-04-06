package org.codehaus.plexus.redback.rbac;

import java.util.List;

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
 * AbstractUserAssignment useful for common logic that implementors can use. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractUserAssignment
    implements UserAssignment
{

    public void addRoleName( Role role )
    {
        addRoleName( role.getName() );
    }

    public void addRoleName( String roleName )
    {
        List<String> names = getRoleNames();
        if ( !names.contains( roleName ) )
        {
            names.add( roleName );
        }
        setRoleNames( names );
    }

    public void removeRoleName( Role role )
    {
        removeRoleName( role.getName() );
    }

    public void removeRoleName( String roleName )
    {
        getRoleNames().remove( roleName );
    }
}
