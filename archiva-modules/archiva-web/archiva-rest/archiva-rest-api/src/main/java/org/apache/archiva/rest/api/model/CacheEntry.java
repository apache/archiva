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
@XmlRootElement(name = "cacheEntry")
public class CacheEntry
    implements Serializable, Comparable
{
    private String key;

    private long size;

    private long cacheHits;

    private long cacheMiss;

    private String cacheHitRate;

    private long inMemorySize;

    public CacheEntry()
    {
        // no op
    }

    public CacheEntry( String key, long size, long cacheHits, long cacheMiss, String cacheHitRate, long inMemorySize )
    {
        this.key = key;
        this.size = size;
        this.cacheHits = cacheHits;
        this.cacheMiss = cacheMiss;
        this.cacheHitRate = cacheHitRate;
        // size is in bytes so use kb
        this.inMemorySize = inMemorySize / 1024;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public long getCacheHits()
    {
        return cacheHits;
    }

    public void setCacheHits( long cacheHits )
    {
        this.cacheHits = cacheHits;
    }

    public long getCacheMiss()
    {
        return cacheMiss;
    }

    public void setCacheMiss( long cacheMiss )
    {
        this.cacheMiss = cacheMiss;
    }

    public String getCacheHitRate()
    {
        return cacheHitRate;
    }

    public void setCacheHitRate( String cacheHitRate )
    {
        this.cacheHitRate = cacheHitRate;
    }

    /**
     * @return cache size in kb
     */
    public long getInMemorySize()
    {
        return inMemorySize;
    }

    public void setInMemorySize( long inMemorySize )
    {
        this.inMemorySize = inMemorySize;
    }

    @Override
    public int compareTo( Object o )
    {
        return this.key.compareTo( ( (CacheEntry) o ).key );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        CacheEntry that = (CacheEntry) o;

        if ( !key.equals( that.key ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return key.hashCode();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CacheEntry" );
        sb.append( "{key='" ).append( key ).append( '\'' );
        sb.append( ", size=" ).append( size );
        sb.append( ", cacheHits=" ).append( cacheHits );
        sb.append( ", cacheMiss=" ).append( cacheMiss );
        sb.append( ", cacheHitRate='" ).append( cacheHitRate ).append( '\'' );
        sb.append( ", inMemorySize=" ).append( inMemorySize );
        sb.append( '}' );
        return sb.toString();
    }
}
