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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.archiva.metadata.model.MetadataFacet;

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
        return properties;
    }

    public void fromProperties( Map<String, String> properties )
    {
        scanEndTime = new Date( Long.valueOf( properties.get( "scanEndTime" ) ) );
        scanStartTime = new Date( Long.valueOf( properties.get( "scanStartTime" ) ) );
        totalArtifactCount = Long.valueOf( properties.get( "totalArtifactCount" ) );
        totalArtifactFileSize = Long.valueOf( properties.get( "totalArtifactFileSize" ) );
        totalFileCount = Long.valueOf( properties.get( "totalFileCount" ) );
        totalGroupCount = Long.valueOf( properties.get( "totalGroupCount" ) );
        totalProjectCount = Long.valueOf( properties.get( "totalProjectCount" ) );
        newFileCount = Long.valueOf( properties.get( "newFileCount" ) );
    }
}
