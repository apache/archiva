package org.apache.archiva.redback.rbac;

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
 * AbstractRole useful for common logic that implementors can use. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public abstract class AbstractRole
    implements Role
{

    public boolean hasChildRoles()
    {
        return ( getChildRoleNames() != null ) && !getChildRoleNames().isEmpty();
    }

    /**
     * Method equals.
     *
     * @param other
     * @return boolean
     */
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof AbstractRole ) )
        {
            return false;
        }

        AbstractRole that = (AbstractRole) other;
        boolean result = true;

        result = result && ( getName() == null ? that.getName() == null : getName().equals( that.getName() ) );

        return result;
    }
}
