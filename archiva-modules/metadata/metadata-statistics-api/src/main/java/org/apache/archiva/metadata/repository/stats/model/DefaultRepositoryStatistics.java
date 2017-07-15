package org.apache.archiva.metadata.repository.stats.model;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Default statistics implementation
 */
public class DefaultRepositoryStatistics
    implements RepositoryStatistics
{
    private Date scanEndTime;

    private Date scanStartTime;

    private long totalArtifactCount;

    private long totalArtifactFileSize;

    private long totalFileCount;

    private long totalGroupCount;

    private long totalProjectCount;

    private long newFileCount;

    public static final String SCAN_TIMESTAMP_FORMAT = "yyyy/MM/dd/HHmmss.SSS";

    private Map<String, Long> totalCountForType = new ZeroForNullHashMap<>();

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private String repositoryId;

    private Map<String, Long> customValues;

    public static final String TYPE_PREFIX = "count-type-";
    public static final String CUSTOM_PREFIX = "count-custom-";

    @Override
    public Date getScanEndTime( )
    {
        return scanEndTime;
    }

    public void setScanEndTime( Date scanEndTime )
    {
        this.scanEndTime = scanEndTime;
    }

    @Override
    public Date getScanStartTime( )
    {
        return scanStartTime;
    }

    public void setScanStartTime( Date scanStartTime )
    {
        this.scanStartTime = scanStartTime;
    }

    @Override
    public long getTotalArtifactCount( )
    {
        return totalArtifactCount;
    }

    @Override
    public void setTotalArtifactCount( long totalArtifactCount )
    {
        this.totalArtifactCount = totalArtifactCount;
    }

    @Override
    public long getTotalArtifactFileSize( )
    {
        return totalArtifactFileSize;
    }

    @Override
    public void setTotalArtifactFileSize( long totalArtifactFileSize )
    {
        this.totalArtifactFileSize = totalArtifactFileSize;
    }

    @Override
    public long getTotalFileCount( )
    {
        return totalFileCount;
    }

    @Override
    public void setTotalFileCount( long totalFileCount )
    {
        this.totalFileCount = totalFileCount;
    }

    @Override
    public long getTotalGroupCount( )
    {
        return totalGroupCount;
    }

    @Override
    public void setTotalGroupCount( long totalGroupCount )
    {
        this.totalGroupCount = totalGroupCount;
    }

    @Override
    public long getTotalProjectCount( )
    {
        return totalProjectCount;
    }

    @Override
    public void setTotalProjectCount( long totalProjectCount )
    {
        this.totalProjectCount = totalProjectCount;
    }

    @Override
    public void setNewFileCount( long newFileCount )
    {
        this.newFileCount = newFileCount;
    }

    @Override
    public long getNewFileCount( )
    {
        return newFileCount;
    }

    @Override
    public long getDuration( )
    {
        return scanEndTime.getTime() - scanStartTime.getTime();
    }

    @Override
    public String getRepositoryId( )
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    @Override
    public String getFacetId()
    {
        return FACET_ID;
    }

    @Override
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

    @Override
    public Map<String, String> toProperties()
    {
        Map<String, String> properties = new HashMap<>();
        if (scanEndTime==null) {
            properties.put("scanEndTime", "0");
        } else
        {
            properties.put( "scanEndTime", String.valueOf( scanEndTime.getTime( ) ) );
        }
        if (scanStartTime==null) {
            properties.put("scanStartTime","0");
        } else
        {
            properties.put( "scanStartTime", String.valueOf( scanStartTime.getTime( ) ) );
        }
        properties.put( "totalArtifactCount", String.valueOf( totalArtifactCount ) );
        properties.put( "totalArtifactFileSize", String.valueOf( totalArtifactFileSize ) );
        properties.put( "totalFileCount", String.valueOf( totalFileCount ) );
        properties.put( "totalGroupCount", String.valueOf( totalGroupCount ) );
        properties.put( "totalProjectCount", String.valueOf( totalProjectCount ) );
        properties.put( "newFileCount", String.valueOf( newFileCount ) );
        properties.put( "repositoryId", repositoryId );
        for ( Map.Entry<String, Long> entry : totalCountForType.entrySet() )
        {
            properties.put( TYPE_PREFIX + entry.getKey(), String.valueOf( entry.getValue() ) );
        }
        if (customValues!=null) {
            for (Map.Entry<String, Long> entry : customValues.entrySet()) {
                properties.put(CUSTOM_PREFIX+entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return properties;
    }

    @Override
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
        repositoryId = properties.get( "repositoryId" );
        totalCountForType.clear();
        for ( Map.Entry<String, String> entry : properties.entrySet() )
        {
            if ( entry.getKey().startsWith( TYPE_PREFIX ) )
            {
                totalCountForType.put( entry.getKey().substring( TYPE_PREFIX.length() ), Long.valueOf( entry.getValue() ) );
            } else if (entry.getKey().startsWith( CUSTOM_PREFIX )) {
                if (customValues==null) {
                    createCustomValueMap();
                }
                customValues.put(entry.getKey().substring( CUSTOM_PREFIX.length() ), Long.valueOf(entry.getValue()));
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

        DefaultRepositoryStatistics that = (DefaultRepositoryStatistics) o;

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
        if ( customValues==null && that.customValues!=null) {
            return false;
        }
        if ( customValues!=null && that.customValues==null) {
            return false;
        }
        if (customValues!=null && !customValues.equals(that.customValues)) {
            return false;
        }
        return repositoryId.equals( that.repositoryId );
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
        result = 31 * result + repositoryId.hashCode();
        if (customValues!=null)
            result = 31 * result + customValues.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "RepositoryStatistics{" + "scanEndTime=" + scanEndTime + ", scanStartTime=" + scanStartTime +
            ", totalArtifactCount=" + totalArtifactCount + ", totalArtifactFileSize=" + totalArtifactFileSize +
            ", totalFileCount=" + totalFileCount + ", totalGroupCount=" + totalGroupCount + ", totalProjectCount=" +
            totalProjectCount + ", newFileCount=" + newFileCount + ", totalCountForType=" + totalCountForType + ", " +
            "repositoryId=" + repositoryId +
            getCustomValueString() +
            '}';
    }

    private String getCustomValueString() {
        if (customValues==null) {
            return "";
        } else {
            return customValues.entrySet().stream().map(entry -> entry.getKey()+"="+entry.getValue()).collect(
                Collectors.joining( ",")
            );
        }
    }

    @Override
    public Map<String, Long> getTotalCountForType( )
    {
        return totalCountForType;
    }

    @Override
    public long getTotalCountForType( String type )
    {
        return totalCountForType.get( type );
    }

    @Override
    public void setTotalCountForType( String type, long count )
    {
        totalCountForType.put( type, count );
    }

    @Override
    public long getCustomValue( String fieldName )
    {
        // Lazy evaluation, because it may not be used very often.
        if (customValues==null) {
            createCustomValueMap();
        }
        return customValues.get(fieldName);
    }

    @Override
    public void setCustomValue( String fieldName, long count )
    {
        // Lazy evaluation, because it may not be used very often.
        if (customValues==null) {
            createCustomValueMap();
        }
        customValues.put(fieldName, count);
    }

    private void createCustomValueMap( )
    {
        customValues = new ZeroForNullHashMap<>();
    }


    private static final class ZeroForNullHashMap<K> extends HashMap<K, Long>
    {   
        @Override
        public Long get(Object key) {
            Long value = super.get( key );
            
            return ( value != null ) ? value : Long.valueOf( 0L );
        }
    }
}
