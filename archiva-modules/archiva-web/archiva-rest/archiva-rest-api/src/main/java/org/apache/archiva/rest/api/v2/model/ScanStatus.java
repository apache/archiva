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
import org.apache.archiva.admin.model.beans.MetadataScanTask;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="ScanStatus", description = "Status of repository scan tasks")
public class ScanStatus implements Serializable, RestModel
{
    private boolean scanRunning = false;
    private int scanQueued = 0;
    private boolean indexRunning = false;
    private int indexQueued = 0;
    private List<IndexingTask> indexingQueue = new ArrayList<>(  );
    private List<ScanTask> scanQueue = new ArrayList<>(  );

    public ScanStatus( )
    {
    }

    public static ScanStatus of( org.apache.archiva.admin.model.beans.ScanStatus modelStatus ) {
        ScanStatus status = new ScanStatus( );
        status.setIndexRunning( modelStatus.isIndexScanRunning() );
        status.setScanRunning( modelStatus.isMetadataScanRunning() );
        List<org.apache.archiva.admin.model.beans.IndexingTask> indexQueue = modelStatus.getIndexingQueue( );
        status.setIndexingQueue( indexQueue.stream().map(IndexingTask::of).collect( Collectors.toList()) );
        status.setIndexQueued( indexQueue.size( ) > 0 ? indexQueue.size( ) - 1 : 0 );
        List<MetadataScanTask> scanQueue = modelStatus.getScanQueue( );
        status.setScanQueue( scanQueue.stream().map( ScanTask::of ).collect( Collectors.toList()) );
        status.setScanQueued( scanQueue.size( ) > 0 ? scanQueue.size( ) - 1 : 0 );
        return status;

    }

    @Schema( name = "scan_running", description = "True, if a scan is currently running" )
    public boolean isScanRunning( )
    {
        return scanRunning;
    }

    public void setScanRunning( boolean scanRunning )
    {
        this.scanRunning = scanRunning;
    }

    @Schema(name ="scan_queued", description = "Number of scans in the task queue")
    public int getScanQueued( )
    {
        return scanQueued;
    }

    public void setScanQueued( int scanQueued )
    {
        this.scanQueued = scanQueued;
    }

    @Schema(name="index_running", description = "True, if there is a index task currently running")
    public boolean isIndexRunning( )
    {
        return indexRunning;
    }

    public void setIndexRunning( boolean indexRunning )
    {
        this.indexRunning = indexRunning;
    }

    @Schema(name="index_queued", description = "Number of queued index tasks")
    public int getIndexQueued( )
    {
        return indexQueued;
    }

    public void setIndexQueued( int indexQueued )
    {
        this.indexQueued = indexQueued;
    }

    @Schema( name = "indexing_queue", description = "List of indexing tasks waiting for execution" )
    public List<IndexingTask> getIndexingQueue( )
    {
        return indexingQueue;
    }

    public void setIndexingQueue( List<IndexingTask> indexingQueue )
    {
        this.indexingQueue = new ArrayList<>( indexingQueue );
    }

    @Schema(name="scan_queue", description = "List of scan tasks waiting for execution")
    public List<ScanTask> getScanQueue( )
    {
        return scanQueue;
    }

    public void setScanQueue( List<ScanTask> scanQueue )
    {
        this.scanQueue = new ArrayList<>( scanQueue );
    }
}
