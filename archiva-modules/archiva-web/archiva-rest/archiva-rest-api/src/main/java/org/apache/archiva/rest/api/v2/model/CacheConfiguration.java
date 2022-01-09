package org.apache.archiva.rest.api.v2.model;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 3.0
 */
@XmlRootElement( name = "cacheConfiguration" )
@Schema(name="CacheConfiguration",description = "Cache configuration attributes")
public class CacheConfiguration
    implements Serializable, RestModel
{
    private static final long serialVersionUID = 5479989049980673894L;
    /**
     * TimeToIdleSeconds.
     */
    private int timeToIdleSeconds = -1;

    /**
     * TimeToLiveSeconds.
     */
    private int timeToLiveSeconds = -1;

    /**
     * max elements in memory.
     */
    private int maxEntriesInMemory = -1;

    /**
     * max elements on disk.
     */
    private int maxEntriesOnDisk = -1;

    public CacheConfiguration()
    {
        // no op
    }

    public static CacheConfiguration of( org.apache.archiva.admin.model.beans.CacheConfiguration beanConfiguration ) {
        CacheConfiguration newConfig = new CacheConfiguration( );
        newConfig.setMaxEntriesInMemory( beanConfiguration.getMaxElementsInMemory() );
        newConfig.setMaxEntriesOnDisk( beanConfiguration.getMaxElementsOnDisk() );
        newConfig.setTimeToIdleSeconds( beanConfiguration.getTimeToIdleSeconds( ) );
        newConfig.setTimeToLiveSeconds( beanConfiguration.getTimeToLiveSeconds( ) );
        return newConfig;
    }

    @Schema(name="time_to_idle_seconds", description = "The maximum number of seconds an element can exist in the cache without being accessed. "+
        "The element expires at this limit and will no longer be returned from the cache.")
    public int getTimeToIdleSeconds()
    {
        return timeToIdleSeconds;
    }

    public void setTimeToIdleSeconds( int timeToIdleSeconds )
    {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    @Schema(name="time_to_live_seconds", description = "The maximum number of seconds an element can exist in the cache regardless of use. "+
        "The element expires at this limit and will no longer be returned from the cache.")
    public int getTimeToLiveSeconds()
    {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds( int timeToLiveSeconds )
    {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    @Schema(name="max_entries_in_memory", description = "The maximum cache entries to keep in memory. If the limit is reached, older entries will be evicted, or persisted on disk.")
    public int getMaxEntriesInMemory()
    {
        return maxEntriesInMemory;
    }

    public void setMaxEntriesInMemory( int maxEntriesInMemory )
    {
        this.maxEntriesInMemory = maxEntriesInMemory;
    }

    @Schema(name="max_entries_on_disk", description = "The maximum cache entries to keep on disk. If the limit is reached, older entries will be evicted.")
    public int getMaxEntriesOnDisk()
    {
        return maxEntriesOnDisk;
    }

    public void setMaxEntriesOnDisk( int maxEntriesOnDisk )
    {
        this.maxEntriesOnDisk = maxEntriesOnDisk;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        CacheConfiguration that = (CacheConfiguration) o;

        if ( timeToIdleSeconds != that.timeToIdleSeconds ) return false;
        if ( timeToLiveSeconds != that.timeToLiveSeconds ) return false;
        if ( maxEntriesInMemory != that.maxEntriesInMemory ) return false;
        return maxEntriesOnDisk == that.maxEntriesOnDisk;
    }

    @Override
    public int hashCode( )
    {
        int result = timeToIdleSeconds;
        result = 31 * result + timeToLiveSeconds;
        result = 31 * result + maxEntriesInMemory;
        result = 31 * result + maxEntriesOnDisk;
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CacheConfiguration" );
        sb.append( "{time_to_idle_seconds=" ).append( timeToIdleSeconds );
        sb.append( ", time_to_live_seconds=" ).append( timeToLiveSeconds );
        sb.append( ", max_elements_in_memory=" ).append( maxEntriesInMemory );
        sb.append( ", max_elements_on_disk=" ).append( maxEntriesOnDisk );
        sb.append( '}' );
        return sb.toString();
    }
}
