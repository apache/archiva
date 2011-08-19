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
 * @since 1.4
 */
@XmlRootElement( name = "managedRepository" )
public class ManagedRepository
    implements Serializable
{
    private String id;

    private String name;

    private String url;

    private String layout;

    private boolean snapshots = false;

    private boolean releases = false;

    public ManagedRepository()
    {
        // no op
    }

    public ManagedRepository( String id, String name, String url, String layout, boolean snapshots, boolean releases )
    {
        this.id = id;
        this.name = name;
        this.url = url;
        this.layout = layout;
        this.snapshots = snapshots;
        this.releases = releases;
    }


    public String getId()
    {
        return this.id;
    }

    public String getLayout()
    {
        return this.layout;
    }

    public String getName()
    {
        return this.name;
    }

    public String getUrl()
    {
        return this.url;
    }


    public boolean isReleases()
    {
        return this.releases;
    }

    /**
     * Get null
     */
    public boolean isSnapshots()
    {
        return this.snapshots;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setReleases( boolean releases )
    {
        this.releases = releases;
    }

    public void setSnapshots( boolean snapshots )
    {
        this.snapshots = snapshots;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }


    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( id != null ? id.hashCode() : 0 );
        return result;
    }

    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof ManagedRepository ) )
        {
            return false;
        }

        ManagedRepository that = (ManagedRepository) other;
        boolean result = true;
        result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
        return result;
    }

    @Override
    public String toString()
    {
        return "ManagedRepository{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", url='" + url + '\''
            + ", layout='" + layout + '\'' + ", snapshots=" + snapshots + ", releases=" + releases + '}';
    }
}