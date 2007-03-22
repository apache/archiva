package org.apache.maven.archiva.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * RepositoryContentKey - the jpox application key support class for all content within the repository.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryContentKey implements Serializable
{
    /**
     * The Repository ID. (JPOX Requires this remain public)
     */
    public String repositoryId = "";

    /**
     * The Group ID. (JPOX Requires this remain public)
     */
    public String groupId = "";

    /**
     * The Artifact ID. (JPOX Requires this remain public)
     */
    public String artifactId = "";

    /**
     * The Version. (JPOX Requires this remain public)
     */
    public String version = "";

    /**
     * Default Constructor.  Required by JPOX.
     */
    public RepositoryContentKey()
    {

    }

    /**
     * Key Based Constructor.  Required by JPOX.
     * 
     * @param key the String representing this object's values.
     */
    public RepositoryContentKey( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );
        repositoryId = parts[0];
        groupId = parts[1];
        artifactId = parts[2];
        version = parts[3];
    }

    /**
     * Get the String representation of this object. - Required by JPOX.
     */
    public String toString()
    {
        return StringUtils.join( new String[] { repositoryId, groupId, artifactId, version } );
    }

    /**
     * Get the hashcode for this object's values - Required by JPOX.
     */
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( repositoryId == null ) ? 0 : repositoryId.hashCode() );
        result = PRIME * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = PRIME * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = PRIME * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    /**
     * Get the equals for this object's values - Required by JPOX.
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final RepositoryContentKey other = (RepositoryContentKey) obj;

        if ( repositoryId == null )
        {
            if ( other.repositoryId != null )
            {
                return false;
            }
        }
        else if ( !repositoryId.equals( other.repositoryId ) )
        {
            return false;
        }

        if ( groupId == null )
        {
            if ( other.groupId != null )
            {
                return false;
            }
        }
        else if ( !groupId.equals( other.groupId ) )
        {
            return false;
        }

        if ( artifactId == null )
        {
            if ( other.artifactId != null )
            {
                return false;
            }
        }
        else if ( !artifactId.equals( other.artifactId ) )
        {
            return false;
        }

        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }

        return true;
    }
}
