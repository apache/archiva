package org.apache.archiva.scheduler.indexing.maven;
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

import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.updater.IndexUpdater;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class DownloadRemoteIndexTaskRequest
{
    private RemoteRepository remoteRepository;

    private WagonFactory wagonFactory;

    private NetworkProxy networkProxy;

    private boolean fullDownload;

    private IndexUpdater indexUpdater;

    private IndexPacker indexPacker;

    public DownloadRemoteIndexTaskRequest()
    {
        // no op
    }

    public RemoteRepository getRemoteRepository()
    {
        return remoteRepository;
    }

    public DownloadRemoteIndexTaskRequest setRemoteRepository( RemoteRepository remoteRepository )
    {
        this.remoteRepository = remoteRepository;
        return this;
    }


    public WagonFactory getWagonFactory()
    {
        return wagonFactory;
    }

    public DownloadRemoteIndexTaskRequest setWagonFactory( WagonFactory wagonFactory )
    {
        this.wagonFactory = wagonFactory;
        return this;
    }

    public NetworkProxy getNetworkProxy()
    {
        return networkProxy;
    }

    public DownloadRemoteIndexTaskRequest setNetworkProxy( NetworkProxy networkProxy )
    {
        this.networkProxy = networkProxy;
        return this;
    }

    public boolean isFullDownload()
    {
        return fullDownload;
    }

    public DownloadRemoteIndexTaskRequest setFullDownload( boolean fullDownload )
    {
        this.fullDownload = fullDownload;
        return this;
    }

    public IndexUpdater getIndexUpdater()
    {
        return indexUpdater;
    }

    public DownloadRemoteIndexTaskRequest setIndexUpdater( IndexUpdater indexUpdater )
    {
        this.indexUpdater = indexUpdater;
        return this;
    }

    public IndexPacker getIndexPacker()
    {
        return indexPacker;
    }

    public DownloadRemoteIndexTaskRequest setIndexPacker( IndexPacker indexPacker )
    {
        this.indexPacker = indexPacker;
        return this;
    }
}
