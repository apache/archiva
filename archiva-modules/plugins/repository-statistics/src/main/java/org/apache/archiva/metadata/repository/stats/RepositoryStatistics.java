package org.apache.archiva.metadata.repository.stats;

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

import org.apache.archiva.metadata.model.MetadataFacet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class RepositoryStatistics
    implements MetadataFacet
{
    private Date scanEndTime;

    private Date scanStartTime;

    private long totalArtifactCount;

    private long totalArtifactFileSize;

    private long totalFileCount;

    private long totalGroupCount;

    private long totalProjectCount;

    private long newFileCount;

    public static String FACET_ID = "org.apache.archiva.metadata.repository.stats";

    static final String SCAN_TIMESTAMP_FORMAT = "yyyy/MM/dd/HHmmss.SSS";

    private Map<String, Long> totalCountForType = new ZeroForNullHashMap<String, Long>();

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    public Date getScanEndTime()
    {
        return scanEndTime;
    }

    public void setScanEndTime( Date scanEndTime )
    {
        this.scanEndTime = scanEndTime;
    }

    public Date getScanStartTime()
    {
        return scanStartTime;
    }

    public void setScanStartTime( Date scanStartTime )
    {
        this.scanStartTime = scanStartTime;
    }

    public long getTotalArtifactCount()
    {
        return totalArtifactCount;
    }

    public void setTotalArtifactCount( long totalArtifactCount )
    {
        this.totalArtifactCount = totalArtifactCount;
    }

    public long getTotalArtifactFileSize()
    {
        return totalArtifactFileSize;
    }

    public void setTotalArtifactFileSize( long totalArtifactFileSize )
    {
        this.totalArtifactFileSize = totalArtifactFileSize;
    }

    public long getTotalFileCount()
    {
        return totalFileCount;
    }

    public void setTotalFileCount( long totalFileCount )
    {
        this.totalFileCount = totalFileCount;
    }

    public long getTotalGroupCount()
    {
        return totalGroupCount;
    }

    public void setTotalGroupCount( long totalGroupCount )
    {
        this.totalGroupCount = totalGroupCount;
    }

    public long getTotalProjectCount()
    {
        return totalProjectCount;
    }

    public void setTotalProjectCount( long totalProjectCount )
    {
        this.totalProjectCount = totalProjectCount;
    }

    public void setNewFileCount( long newFileCount )
    {
        this.newFileCount = newFileCount;
    }

    public long getNewFileCount()
    {
        return newFileCount;
    }

    public long getDuration()
    {
        return scanEndTime.getTime() - scanStartTime.getTime();
    }

    public String getFacetId()
    {
        return FACET_ID;
    }

    public String getName()
    {
        return createNameFormat().format( scanStartTime );
    }

    private static SimpleDateFormat createNameFormat()
    {
        SimpleDateFormat fmt = new SimpleDateFormat( SCAN_TIMESTAMP_FORMAT );
        fmt.setTimeZone( UTC_TIME_ZONE );
        return fmt;
    }

    public Map<String, String> toProperties()
    {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put( "scanEndTime", String.valueOf( scanEndTime.getTime() ) );
        properties.put( "scanStartTime", String.valueOf( scanStartTime.getTime() ) );
        properties.put( "totalArtifactCount", String.valueOf( totalArtifactCount ) );
        properties.put( "totalArtifactFileSize", String.valueOf( totalArtifactFileSize ) );
        properties.put( "totalFileCount", String.valueOf( totalFileCount ) );
        properties.put( "totalGroupCount", String.valueOf( totalGroupCount ) );
        properties.put( "totalProjectCount", String.valueOf( totalProjectCount ) );
        properties.put( "newFileCount", String.valueOf( newFileCount ) );
        for ( Map.Entry<String, Long> entry : totalCountForType.entrySet() )
        {
            properties.put( "count-" + entry.getKey(), String.valueOf( entry.getValue() ) );
        }
        return properties;
    }

    public void fromProperties( Map<String, String> properties )
    {
        scanEndTime = new Date( Long.parseLong( properties.get( "scanEndTime" ) ) );
        scanStartTime = new Date( Long.parseLong( properties.get( "scanStartTime" ) ) );
        totalArtifactCount = Long.parseLong( properties.get( "totalArtifactCount" ) );
        totalArtifactFileSize = Long.parseLong( properties.get( "totalArtifactFileSize" ) );
        totalFileCount = Long.parseLong( properties.get( "totalFileCount" ) );
        totalGroupCount = Long.parseLong( properties.get( "totalGroupCount" ) );
        totalProjectCount = Long.parseLong( properties.get( "totalProjectCount" ) );
        newFileCount = Long.parseLong( properties.get( "newFileCount" ) );
        totalCountForType.clear();
        for ( Map.Entry<String, String> entry : properties.entrySet() )
        {
            if ( entry.getKey().startsWith( "count-" ) )
            {
                totalCountForType.put( entry.getKey().substring( 6 ), Long.valueOf( entry.getValue() ) );
            }
        }
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

        RepositoryStatistics that = (RepositoryStatistics) o;

        if ( newFileCount != that.newFileCount )
        {
            return false;
        }
        if ( totalArtifactCount != that.totalArtifactCount )
        {
            return false;
        }
        if ( totalArtifactFileSize != that.totalArtifactFileSize )
        {
            return false;
        }
        if ( totalFileCount != that.totalFileCount )
        {
            return false;
        }
        if ( totalGroupCount != that.totalGroupCount )
        {
            return false;
        }
        if ( totalProjectCount != that.totalProjectCount )
        {
            return false;
        }
        if ( !scanEndTime.equals( that.scanEndTime ) )
        {
            return false;
        }
        if ( !scanStartTime.equals( that.scanStartTime ) )
        {
            return false;
        }
        if ( !totalCountForType.equals( that.totalCountForType ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = scanEndTime.hashCode();
        result = 31 * result + scanStartTime.hashCode();
        result = 31 * result + (int) ( totalArtifactCount ^ ( totalArtifactCount >>> 32 ) );
        result = 31 * result + (int) ( totalArtifactFileSize ^ ( totalArtifactFileSize >>> 32 ) );
        result = 31 * result + (int) ( totalFileCount ^ ( totalFileCount >>> 32 ) );
        result = 31 * result + (int) ( totalGroupCount ^ ( totalGroupCount >>> 32 ) );
        result = 31 * result + (int) ( totalProjectCount ^ ( totalProjectCount >>> 32 ) );
        result = 31 * result + (int) ( newFileCount ^ ( newFileCount >>> 32 ) );
        result = 31 * result + totalCountForType.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "RepositoryStatistics{" + "scanEndTime=" + scanEndTime + ", scanStartTime=" + scanStartTime +
            ", totalArtifactCount=" + totalArtifactCount + ", totalArtifactFileSize=" + totalArtifactFileSize +
            ", totalFileCount=" + totalFileCount + ", totalGroupCount=" + totalGroupCount + ", totalProjectCount=" +
            totalProjectCount + ", newFileCount=" + newFileCount + ", totalCountForType=" + totalCountForType + '}';
    }

    public Map<String, Long> getTotalCountForType()
    {
        return totalCountForType;
    }

    public long getTotalCountForType( String type )
    {
        return totalCountForType.get( type );
    }

    public void setTotalCountForType( String type, long count )
    {
        totalCountForType.put( type, count );
    }
    
    private static final class ZeroForNullHashMap<K, V extends Long> extends HashMap<K, V>
    {   
        @Override
        public V get(Object key) {
            V value = super.get( key );
            
            return value != null ? value : ( V ) Long.valueOf( 0L );
        }
    }
}
