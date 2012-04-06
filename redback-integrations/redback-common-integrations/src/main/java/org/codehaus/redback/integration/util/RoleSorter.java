package org.codehaus.redback.integration.util;

/*
 * Copyright 2005-2006 The Codehaus.
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

import java.util.Comparator;

import org.codehaus.plexus.redback.rbac.Role;

/**
 * RoleSorter
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RoleSorter
    implements Comparator<Role>
{
    public int compare( Role role1, Role role2 )
    {
        if ( ( role1 == null ) && ( role2 == null ) )
        {
            return 0;
        }

        if ( ( role1 == null ) && ( role2 != null ) )
        {
            return -1;
        }

        if ( ( role1 != null ) && ( role2 == null ) )
        {
            return 1;
        }

        return role1.getName().compareToIgnoreCase( role2.getName() );
    }
}
