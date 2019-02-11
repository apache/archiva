package org.apache.archiva.configuration;

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

/**
 * Cache configuration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class CacheConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

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


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get max elements in memory.
     * 
     * @return int
     */
    public int getMaxElementsInMemory()
    {
        return this.maxElementsInMemory;
    } //-- int getMaxElementsInMemory()

    /**
     * Get max elements on disk.
     * 
     * @return int
     */
    public int getMaxElementsOnDisk()
    {
        return this.maxElementsOnDisk;
    } //-- int getMaxElementsOnDisk()

    /**
     * Get timeToIdleSeconds.
     * 
     * @return int
     */
    public int getTimeToIdleSeconds()
    {
        return this.timeToIdleSeconds;
    } //-- int getTimeToIdleSeconds()

    /**
     * Get timeToLiveSeconds.
     * 
     * @return int
     */
    public int getTimeToLiveSeconds()
    {
        return this.timeToLiveSeconds;
    } //-- int getTimeToLiveSeconds()

    /**
     * Set max elements in memory.
     * 
     * @param maxElementsInMemory
     */
    public void setMaxElementsInMemory( int maxElementsInMemory )
    {
        this.maxElementsInMemory = maxElementsInMemory;
    } //-- void setMaxElementsInMemory( int )

    /**
     * Set max elements on disk.
     * 
     * @param maxElementsOnDisk
     */
    public void setMaxElementsOnDisk( int maxElementsOnDisk )
    {
        this.maxElementsOnDisk = maxElementsOnDisk;
    } //-- void setMaxElementsOnDisk( int )

    /**
     * Set timeToIdleSeconds.
     * 
     * @param timeToIdleSeconds
     */
    public void setTimeToIdleSeconds( int timeToIdleSeconds )
    {
        this.timeToIdleSeconds = timeToIdleSeconds;
    } //-- void setTimeToIdleSeconds( int )

    /**
     * Set timeToLiveSeconds.
     * 
     * @param timeToLiveSeconds
     */
    public void setTimeToLiveSeconds( int timeToLiveSeconds )
    {
        this.timeToLiveSeconds = timeToLiveSeconds;
    } //-- void setTimeToLiveSeconds( int )

}
