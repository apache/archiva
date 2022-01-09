package org.apache.archiva.rest.api.v2.model;/*
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
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@XmlRootElement(name="repositoryStatistics")
@Schema(name="RepositoryStatistics", description = "Statistics data")
public class RepositoryStatistics implements Serializable, RestModel
{
    private static final long serialVersionUID = 7943367882738452531L;

    private OffsetDateTime scanEndTime;
    private OffsetDateTime scanStartTime;
    private long scanDurationMs;
    private long totalArtifactCount;
    private long totalArtifactFileSize;
    private long totalFileCount;
    private long totalGroupCount;
    private long totalProjectCount;
    private long newFileCount;
    private Map<String, Long> totalCountForType = new TreeMap<>(  );
    private Map<String, Long> customValues = new TreeMap<>(  );

    public RepositoryStatistics( )
    {
    }

    public static RepositoryStatistics of( org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics modelStats ) {
        RepositoryStatistics newStats = new RepositoryStatistics( );
        newStats.setScanStartTime( modelStats.getScanStartTime().toInstant().atOffset( ZoneOffset.UTC ) );
        newStats.setScanEndTime( modelStats.getScanEndTime( ).toInstant( ).atOffset( ZoneOffset.UTC ) );
        newStats.setNewFileCount( modelStats.getNewFileCount() );
        newStats.setScanDurationMs( Duration.between( newStats.scanStartTime, newStats.scanEndTime ).toMillis() );
        newStats.setTotalArtifactCount( modelStats.getTotalArtifactCount() );
        newStats.setTotalArtifactFileSize( modelStats.getTotalArtifactFileSize() );
        newStats.setTotalCountForType( modelStats.getTotalCountForType() );
        newStats.setTotalFileCount( modelStats.getTotalFileCount() );
        newStats.setTotalGroupCount( modelStats.getTotalGroupCount() );
        newStats.setTotalProjectCount( modelStats.getTotalProjectCount() );
        for (String key : modelStats.getAvailableCustomValues()) {
            newStats.addCustomValue( key, modelStats.getCustomValue( key ) );
        }
        return newStats;
    }

    public static RepositoryStatistics of( RepositoryScanStatistics scanStatistics ) {
        RepositoryStatistics newStats = new RepositoryStatistics( );
        newStats.setScanStartTime( scanStatistics.getWhenGathered().toInstant().atOffset( ZoneOffset.UTC ) );
        newStats.setScanEndTime( OffsetDateTime.now(ZoneOffset.UTC) );
        newStats.setNewFileCount( scanStatistics.getNewFileCount() );
        newStats.setScanDurationMs( Duration.between( newStats.scanStartTime, newStats.scanEndTime ).toMillis() );
        newStats.setTotalArtifactCount( scanStatistics.getTotalFileCount() );
        newStats.setTotalArtifactFileSize( scanStatistics.getTotalSize() );
        newStats.setTotalFileCount( scanStatistics.getTotalFileCount() );
        newStats.setTotalGroupCount( 0 );
        newStats.setTotalProjectCount( 0 );
        return newStats;
    }

    @Schema(name="scan_end_time", description = "Date and time when the last scan finished")
    public OffsetDateTime getScanEndTime( )
    {
        return scanEndTime;
    }

    public void setScanEndTime( OffsetDateTime scanEndTime )
    {
        this.scanEndTime = scanEndTime;
    }

    @Schema( name = "scan_start_time", description = "Date and time when the last scan started" )
    public OffsetDateTime getScanStartTime( )
    {
        return scanStartTime;
    }

    public void setScanStartTime( OffsetDateTime scanStartTime )
    {
        this.scanStartTime = scanStartTime;
    }

    @Schema(name="total_artifact_count", description = "The number of artifacts scanned")
    public long getTotalArtifactCount( )
    {
        return totalArtifactCount;
    }

    public void setTotalArtifactCount( long totalArtifactCount )
    {
        this.totalArtifactCount = totalArtifactCount;
    }

    @Schema(name="total_artifact_file_size", description = "The cumulative size of all files scanned")
    public long getTotalArtifactFileSize( )
    {
        return totalArtifactFileSize;
    }

    public void setTotalArtifactFileSize( long totalArtifactFileSize )
    {
        this.totalArtifactFileSize = totalArtifactFileSize;
    }

    @Schema(name="total_file_count", description = "The total number of files scanned")
    public long getTotalFileCount( )
    {
        return totalFileCount;
    }

    public void setTotalFileCount( long totalFileCount )
    {
        this.totalFileCount = totalFileCount;
    }

    @Schema(name="total_group_count", description = "The number of groups scanned")
    public long getTotalGroupCount( )
    {
        return totalGroupCount;
    }

    public void setTotalGroupCount( long totalGroupCount )
    {
        this.totalGroupCount = totalGroupCount;
    }

    @Schema(name="total_project_count", description = "The number of projects scanned")
    public long getTotalProjectCount( )
    {
        return totalProjectCount;
    }

    public void setTotalProjectCount( long totalProjectCount )
    {
        this.totalProjectCount = totalProjectCount;
    }

    @Schema(name="new_file_count", description = "Number of files registered as new")
    public long getNewFileCount( )
    {
        return newFileCount;
    }

    public void setNewFileCount( long newFileCount )
    {
        this.newFileCount = newFileCount;
    }

    @Schema(name="scan_duration_ms", description = "The duration of the last scan in ms")
    public long getScanDurationMs( )
    {
        return scanDurationMs;
    }

    public void setScanDurationMs( long scanDurationMs )
    {
        this.scanDurationMs = scanDurationMs;
    }

    @Schema(name="total_count_for_type", description = "File counts partitioned by file types")
    public Map<String, Long> getTotalCountForType( )
    {
        return totalCountForType;
    }

    public void setTotalCountForType( Map<String, Long> totalCountForType )
    {
        this.totalCountForType = new TreeMap<>( totalCountForType );
    }

    public void addTotalCountForType( String type, Long value )
    {
        this.totalCountForType.put( type, value );
    }

    @Schema(name="custom_values", description = "Custom statistic values")
    public Map<String, Long> getCustomValues( )
    {
        return customValues;
    }

    public void setCustomValues( Map<String, Long> customValues )
    {
        this.customValues = new TreeMap<>(  customValues );
    }

    public void addCustomValue(String type, Long value) {
        this.customValues.put( type, value );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        RepositoryStatistics that = (RepositoryStatistics) o;

        if ( scanDurationMs != that.scanDurationMs ) return false;
        if ( totalArtifactCount != that.totalArtifactCount ) return false;
        if ( totalArtifactFileSize != that.totalArtifactFileSize ) return false;
        if ( totalFileCount != that.totalFileCount ) return false;
        if ( totalGroupCount != that.totalGroupCount ) return false;
        if ( totalProjectCount != that.totalProjectCount ) return false;
        if ( newFileCount != that.newFileCount ) return false;
        if ( scanEndTime != null ? !scanEndTime.equals( that.scanEndTime ) : that.scanEndTime != null ) return false;
        if ( scanStartTime != null ? !scanStartTime.equals( that.scanStartTime ) : that.scanStartTime != null )
            return false;
        if ( !totalCountForType.equals( that.totalCountForType ) ) return false;
        return customValues.equals( that.customValues );
    }

    @Override
    public int hashCode( )
    {
        int result = scanEndTime != null ? scanEndTime.hashCode( ) : 0;
        result = 31 * result + ( scanStartTime != null ? scanStartTime.hashCode( ) : 0 );
        result = 31 * result + (int) ( scanDurationMs ^ ( scanDurationMs >>> 32 ) );
        result = 31 * result + (int) ( totalArtifactCount ^ ( totalArtifactCount >>> 32 ) );
        result = 31 * result + (int) ( totalArtifactFileSize ^ ( totalArtifactFileSize >>> 32 ) );
        result = 31 * result + (int) ( totalFileCount ^ ( totalFileCount >>> 32 ) );
        result = 31 * result + (int) ( totalGroupCount ^ ( totalGroupCount >>> 32 ) );
        result = 31 * result + (int) ( totalProjectCount ^ ( totalProjectCount >>> 32 ) );
        result = 31 * result + (int) ( newFileCount ^ ( newFileCount >>> 32 ) );
        result = 31 * result + totalCountForType.hashCode( );
        result = 31 * result + customValues.hashCode( );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "RepositoryStatistics{" );
        sb.append( "scanEndTime=" ).append( scanEndTime );
        sb.append( ", scanStartTime=" ).append( scanStartTime );
        sb.append( ", scanDurationMs=" ).append( scanDurationMs );
        sb.append( ", totalArtifactCount=" ).append( totalArtifactCount );
        sb.append( ", totalArtifactFileSize=" ).append( totalArtifactFileSize );
        sb.append( ", totalFileCount=" ).append( totalFileCount );
        sb.append( ", totalGroupCount=" ).append( totalGroupCount );
        sb.append( ", totalProjectCount=" ).append( totalProjectCount );
        sb.append( ", newFileCount=" ).append( newFileCount );
        sb.append( '}' );
        return sb.toString( );
    }
}
