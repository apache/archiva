package org.codehaus.plexus.redback.rbac.memory;

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

import org.codehaus.plexus.redback.rbac.AbstractUserAssignment;
import org.codehaus.plexus.redback.rbac.UserAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryUserAssignment
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MemoryUserAssignment
    extends AbstractUserAssignment
    implements UserAssignment, java.io.Serializable
{

    /**
     * Field principal
     */
    private String principal;

    /**
     * Field roles
     */
    private List<String> roles = new ArrayList<String>( 0 );

    /**
     * Field permanent
     */
    private boolean permanent = false;

    /**
     * Method equals
     *
     * @param other
     */
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof MemoryUserAssignment ) )
        {
            return false;
        }

        MemoryUserAssignment that = (MemoryUserAssignment) other;
        boolean result = true;
        result = result && ( getPrincipal() == null
            ? that.getPrincipal() == null
            : getPrincipal().equals( that.getPrincipal() ) );
        return result;
    }

    /**
     * Get null
     */
    public String getPrincipal()
    {
        return this.principal;
    }

    /**
     * Method getRoles
     */
    public List<String> getRoleNames()
    {
        if ( this.roles == null )
        {
            this.roles = new ArrayList<String>( 0 );
        }

        return this.roles;
    }

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( principal != null ? principal.hashCode() : 0 );
        return result;
    }

    /**
     * Set null
     *
     * @param principal
     */
    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    /**
     * Set null
     *
     * @param roles
     */
    public void setRoleNames( List<String> roles )
    {
        this.roles = roles;
    }

    /**
     * Method toString
     */
    public java.lang.String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "principal = '" );
        buf.append( getPrincipal() + "'" );
        return buf.toString();
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }
}
