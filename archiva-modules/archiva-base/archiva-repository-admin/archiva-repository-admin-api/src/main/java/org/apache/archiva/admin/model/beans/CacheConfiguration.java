package org.apache.archiva.admin.model.beans;
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
 * @since 1.4-M4
 */
@XmlRootElement( name = "cacheConfiguration" )
public class CacheConfiguration
    implements Serializable
{
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
    private int maxElementsInMemory = -1;

    /**
     * max elements on disk.
     */
    private int maxElementsOnDisk = -1;

    public CacheConfiguration()
    {
        // no op
    }

    public int getTimeToIdleSeconds()
    {
        return timeToIdleSeconds;
    }

    public void setTimeToIdleSeconds( int timeToIdleSeconds )
    {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    public int getTimeToLiveSeconds()
    {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds( int timeToLiveSeconds )
    {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public int getMaxElementsInMemory()
    {
        return maxElementsInMemory;
    }

    public void setMaxElementsInMemory( int maxElementsInMemory )
    {
        this.maxElementsInMemory = maxElementsInMemory;
    }

    public int getMaxElementsOnDisk()
    {
        return maxElementsOnDisk;
    }

    public void setMaxElementsOnDisk( int maxElementsOnDisk )
    {
        this.maxElementsOnDisk = maxElementsOnDisk;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CacheConfiguration" );
        sb.append( "{timeToIdleSeconds=" ).append( timeToIdleSeconds );
        sb.append( ", timeToLiveSeconds=" ).append( timeToLiveSeconds );
        sb.append( ", maxElementsInMemory=" ).append( maxElementsInMemory );
        sb.append( ", maxElementsOnDisk=" ).append( maxElementsOnDisk );
        sb.append( '}' );
        return sb.toString();
    }
}
