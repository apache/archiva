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

import org.codehaus.plexus.redback.rbac.Resource;

/**
 * MemoryResource 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MemoryResource
    implements Resource, java.io.Serializable
{
    /**
     * Field identifier
     */
    private String identifier;

    /**
     * Field pattern
     */
    private boolean pattern = false;
    
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

        if ( !( other instanceof MemoryResource ) )
        {
            return false;
        }

        MemoryResource that = (MemoryResource) other;
        boolean result = true;
        result = result
            && ( getIdentifier() == null ? that.getIdentifier() == null : getIdentifier().equals( that.getIdentifier() ) );
        return result;
    }

    /**
     * Get 
     *             The string identifier for an operation.
     *           
     */
    public String getIdentifier()
    {
        return this.identifier;
    }

    /**
     * Method hashCode
     */
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( identifier != null ? identifier.hashCode() : 0 );
        return result;
    }

    /**
     * Get 
     *             true if the identifer is a pattern that is to be
     * evaluated, for example x.* could match x.a or x.b and x.**
     *             could match x.foo 
     *           
     */
    public boolean isPattern()
    {
        return this.pattern;
    }

    /**
     * Set 
     *             The string identifier for an operation.
     *           
     * 
     * @param identifier
     */
    public void setIdentifier( String identifier )
    {
        this.identifier = identifier;
    }

    /**
     * Set 
     *             true if the identifer is a pattern that is to be
     * evaluated, for example x.* could match x.a or x.b and x.**
     *             could match x.foo 
     *           
     * 
     * @param pattern
     */
    public void setPattern( boolean pattern )
    {
        this.pattern = pattern;
    }

    /**
     * Method toString
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "identifier = '" ).append( getIdentifier() + "'" );
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
