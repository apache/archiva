package org.apache.maven.archiva.reporting.model;

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
 * MetadataResultsKey - used by jpox for application identity for the {@link MetadataResults} object and table. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MetadataResultsKey
    implements Serializable
{
    public String groupId = "";

    public String artifactId = "";

    public String version = "";

    public MetadataResultsKey()
    {
        /* do nothing */
    }

    public MetadataResultsKey( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );
        groupId = parts[0];
        artifactId = parts[1];
        version = parts[2];
    }

    public String toString()
    {
        return StringUtils.join( new String[] { groupId, artifactId, version }, ':' );
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = PRIME * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = PRIME * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

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

        final ArtifactResultsKey other = (ArtifactResultsKey) obj;

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
