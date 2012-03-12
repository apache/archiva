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
import java.util.Date;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "archivaRepositoryStatistics" )
public class ArchivaRepositoryStatistics
    implements Serializable
{
    private Date scanEndTime;

    private Date scanStartTime;

    private long totalArtifactCount;

    private long totalArtifactFileSize;

    private long totalFileCount;

    private long totalGroupCount;

    private long totalProjectCount;

    private long newFileCount;

    private long duration;

    private String lastScanDate;

    public ArchivaRepositoryStatistics()
    {
        // no op
    }

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

    public long getNewFileCount()
    {
        return newFileCount;
    }

    public void setNewFileCount( long newFileCount )
    {
        this.newFileCount = newFileCount;
    }

    public void setDuration( long duration )
    {
        this.duration = duration;
    }

    public long getDuration()
    {
        return duration;
    }

    public String getLastScanDate()
    {
        return lastScanDate;
    }

    public void setLastScanDate( String lastScanDate )
    {
        this.lastScanDate = lastScanDate;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ArchivaRepositoryStatistics" );
        sb.append( "{scanEndTime=" ).append( scanEndTime );
        sb.append( ", scanStartTime=" ).append( scanStartTime );
        sb.append( ", totalArtifactCount=" ).append( totalArtifactCount );
        sb.append( ", totalArtifactFileSize=" ).append( totalArtifactFileSize );
        sb.append( ", totalFileCount=" ).append( totalFileCount );
        sb.append( ", totalGroupCount=" ).append( totalGroupCount );
        sb.append( ", totalProjectCount=" ).append( totalProjectCount );
        sb.append( ", newFileCount=" ).append( newFileCount );
        sb.append( ", duration=" ).append( duration );
        sb.append( ", lastScanDate='" ).append( lastScanDate ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
