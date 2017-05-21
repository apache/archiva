package org.apache.archiva.rest.api.model;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "browseResultEntry" )
public class BrowseResultEntry
    implements Comparable<BrowseResultEntry>, Serializable
{

    private String name;

    private boolean project;

    /**
     * @since 2.0.0
     */
    private String groupId;

    /**
     * @since 2.0.0
     */
    private String artifactId;

    public BrowseResultEntry()
    {
        // no op
    }

    public BrowseResultEntry( String name, boolean project )
    {
        this.name = name;
        this.project = project;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public boolean isProject()
    {
        return project;
    }

    public void setProject( boolean project )
    {
        this.project = project;
    }

    @Override
    public int compareTo( BrowseResultEntry browseGroupResultEntry )
    {
        return this.name.compareTo( browseGroupResultEntry.name );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public BrowseResultEntry groupId( String groupId )
    {
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public BrowseResultEntry artifactId( String artifactId )
    {
        this.artifactId = artifactId;
        return this;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "BrowseResultEntry{" );
        sb.append( "name='" ).append( name ).append( '\'' );
        sb.append( ", project=" ).append( project );
        sb.append( ", groupId='" ).append( groupId ).append( '\'' );
        sb.append( ", artifactId='" ).append( artifactId ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof BrowseResultEntry ) )
        {
            return false;
        }

        BrowseResultEntry that = (BrowseResultEntry) o;

        if ( project != that.project )
        {
            return false;
        }
        if ( !name.equals( that.name ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + ( project ? 1 : 0 );
        return result;
    }
}
