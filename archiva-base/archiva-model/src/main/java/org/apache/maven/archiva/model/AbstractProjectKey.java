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
 * <p>
 * AbstractProjectKey - A versionless reference to a Project.
 * This refers to all versions, and all artifacts of a project.
 * This type of reference is typically used by {@link ArchivaRepositoryMetadata} objects.
 * </p>
 *
 * <p>
 *   If you require things like "Version" or "Type", consider the other keys below.
 * </p>
 * 
 * <table border="1" cellpadding="3">
 *   <tr>
 *     <th>Key Type</th>
 *     <th>Group ID</th>
 *     <th>Artifact ID</th>
 *     <th>Version</th>
 *     <th>Classifier</th>
 *     <th>Type</th>
 *   </tr>
 *   <tr>
 *     <td>{@link AbstractProjectKey}</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *     <td>&nbsp;</td>
 *     <td>&nbsp;</td>
 *     <td>&nbsp;</td>
 *   </tr>
 *   <tr>
 *     <td>{@link AbstractVersionedKey}</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *     <td>&nbsp;</td>
 *     <td>&nbsp;</td>
 *   </tr>
 *   <tr>
 *     <td>{@link AbstractArtifactKey}</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *     <td align="center">Yes</td>
 *   </tr>
 * </table>
 * 
 * <p>
 * NOTE: This is a jpox required compound key handler class.
 * </p>
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AbstractProjectKey
    implements CompoundKey, Serializable
{
    private static final long serialVersionUID = 4949927971768396064L;

    /**
     * The Group ID. (JPOX Requires this remain public)
     */
    public String groupId = "";

    /**
     * The Artifact ID. (JPOX Requires this remain public)
     */
    public String artifactId = "";

    /**
     * Default Constructor.  Required by JPOX.
     */
    public AbstractProjectKey()
    {
        /* do nothing */
    }

    /**
     * Key Based Constructor.  Required by JPOX.
     * 
     * @param key the String representing this object's values.
     */
    public AbstractProjectKey( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ":" );
        groupId = parts[0];
        artifactId = parts[1];
    }

    /**
     * Get the String representation of this object. - Required by JPOX.
     */
    public String toString()
    {
        return StringUtils.join( new String[] { groupId, artifactId } );
    }

    /**
     * Get the hashcode for this object's values - Required by JPOX.
     */
    public int hashCode()
    {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = PRIME * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
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

        if ( !super.equals( obj ) )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final AbstractProjectKey other = (AbstractProjectKey) obj;

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

        return true;
    }
}
