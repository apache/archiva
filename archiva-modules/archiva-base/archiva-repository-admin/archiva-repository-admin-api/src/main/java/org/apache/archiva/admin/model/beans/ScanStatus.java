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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about running and queued repository scans.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ScanStatus implements Serializable
{
    private List<IndexingTask> indexingQueue = new ArrayList<>(  );
    private List<MetadataScanTask> scanQueue = new ArrayList<>(  );

    public ScanStatus( )
    {
    }

    public boolean isMetadataScanRunning( )
    {
        return scanQueue.size( ) > 0 && scanQueue.get(0).isRunning();
    }

    public int getMetadataScanQueueSize() {
        int size= scanQueue.size( );
        if (size>0) {
            return size-1;
        } else {
            return 0;
        }
    }

    public boolean isIndexScanRunning( )
    {
        return indexingQueue.size()>0 && indexingQueue.get(0).isRunning();
    }

    public int getIndexScanQueueSize() {
        int size = indexingQueue.size();
        if (size>0) {
            return size-1;
        } else {
            return 0;
        }
    }

    public List<IndexingTask> getIndexingQueue( )
    {
        return indexingQueue;
    }

    public void setIndexingQueue( List<IndexingTask> indexingQueue )
    {
        this.indexingQueue = new ArrayList<>( indexingQueue );
    }

    public List<MetadataScanTask> getScanQueue( )
    {
        return scanQueue;
    }

    public void setScanQueue( List<MetadataScanTask> scanQueue )
    {
        this.scanQueue = new ArrayList<>( scanQueue );
    }
}
