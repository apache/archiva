package org.apache.archiva.indexer.merger;
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

import org.apache.archiva.repository.storage.StorageAsset;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Date;

/**
 * @author Olivier Lamy
 */
public class TemporaryGroupIndex
    implements Serializable
{
    private long creationTime = new Date().getTime();

    private StorageAsset directory;

    private String indexId;

    private String groupId;

    private int mergedIndexTtl;

    public TemporaryGroupIndex(StorageAsset directory, String indexId, String groupId, int mergedIndexTtl)
    {
        this.directory = directory;
        this.indexId = indexId;
        this.groupId = groupId;
        this.mergedIndexTtl = mergedIndexTtl;
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public TemporaryGroupIndex setCreationTime( long creationTime )
    {
        this.creationTime = creationTime;
        return this;
    }

    public StorageAsset getDirectory()
    {
        return directory;
    }

    public TemporaryGroupIndex setDirectory( StorageAsset directory )
    {
        this.directory = directory;
        return this;
    }

    public String getIndexId()
    {
        return indexId;
    }

    public TemporaryGroupIndex setIndexId( String indexId )
    {
        this.indexId = indexId;
        return this;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public int getMergedIndexTtl() {
        return mergedIndexTtl;
    }

    public void setMergedIndexTtl(int mergedIndexTtl) {
        this.mergedIndexTtl = mergedIndexTtl;
    }

    @Override
    public int hashCode()
    {
        return Long.toString( creationTime ).hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof TemporaryGroupIndex ) )
        {
            return false;
        }
        return this.creationTime == ( (TemporaryGroupIndex) o ).creationTime;
    }
}
