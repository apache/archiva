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

import org.codehaus.plexus.redback.rbac.Operation;

/**
 * MemoryOperation 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MemoryOperation
    implements Operation, java.io.Serializable
{

    /**
     * Field name
     */
    private String name;

    /**
     * Field description
     */
    private String description;

    /**
     * Field resourceRequired
     */
    private boolean resourceRequired = false;
    
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

        if ( !( other instanceof MemoryOperation ) )
        {
            return false;
        }

        MemoryOperation that = (MemoryOperation) other;
        boolean result = true;
        result = result && ( getName() == null ? that.getName() == null : getName().equals( that.getName() ) );
        return result;
    }

    /**
     * Get null
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Get null
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( name != null ? name.hashCode() : 0 );
        return result;
    }

    /**
     * Get 
     *             true if the resource is required for
     * authorization to be granted
     *           
     */
    public boolean isResourceRequired()
    {
        return this.resourceRequired;
    }

    /**
     * Set null
     * 
     * @param description
     */
    public void setDescription( String description )
    {
        this.description = description;
    }

    /**
     * Set null
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Set 
     *             true if the resource is required for
     * authorization to be granted
     *           
     * 
     * @param resourceRequired
     */
    public void setResourceRequired( boolean resourceRequired )
    {
        this.resourceRequired = resourceRequired;
    }

    /**
     * Method toString
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "name = '" );
        buf.append( getName() + "'" );
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
