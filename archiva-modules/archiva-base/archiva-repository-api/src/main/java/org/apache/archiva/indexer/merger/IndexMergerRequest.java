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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.storage.StorageAsset;

import java.nio.file.Path;
import java.util.Collection;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;

/**
 * @author Olivier Lamy
 */
public class IndexMergerRequest
{
    /**
     * repositories Ids to merge content
     */
    private Collection<String> repositoriesIds;

    /**
     * will generate a downloadable index
     */
    private boolean packIndex;

    /**
     * original groupId (repositoryGroup id)
     */
    private String groupId;

    private String mergedIndexPath = DEFAULT_INDEX_PATH;

    private int mergedIndexTtl;

    private StorageAsset mergedIndexDirectory;

    private boolean temporary;

    public IndexMergerRequest( Collection<String> repositoriesIds, boolean packIndex, String groupId )
    {
        this.repositoriesIds = repositoriesIds;
        this.packIndex = packIndex;
        this.groupId = groupId;
    }

    /**
     * @since 1.4-M4
     */
    public IndexMergerRequest( Collection<String> repositoriesIds, boolean packIndex, String groupId,
                               String mergedIndexPath, int mergedIndexTtl )
    {
        this.repositoriesIds = repositoriesIds;
        this.packIndex = packIndex;
        this.groupId = groupId;
        this.mergedIndexPath = mergedIndexPath;
        this.mergedIndexTtl = mergedIndexTtl;
    }

    public Collection<String> getRepositoriesIds()
    {
        return repositoriesIds;
    }

    public void setRepositoriesIds( Collection<String> repositoriesIds )
    {
        this.repositoriesIds = repositoriesIds;
    }

    public boolean isPackIndex()
    {
        return packIndex;
    }

    public void setPackIndex( boolean packIndex )
    {
        this.packIndex = packIndex;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getMergedIndexPath()
    {
        return mergedIndexPath;
    }

    public void setMergedIndexPath( String mergedIndexPath )
    {
        this.mergedIndexPath = mergedIndexPath;
    }

    public int getMergedIndexTtl()
    {
        return mergedIndexTtl;
    }

    public void setMergedIndexTtl( int mergedIndexTtl )
    {
        this.mergedIndexTtl = mergedIndexTtl;
    }

    public StorageAsset getMergedIndexDirectory()
    {
        return mergedIndexDirectory;
    }

    public void setMergedIndexDirectory( StorageAsset mergedIndexDirectory )
    {
        this.mergedIndexDirectory = mergedIndexDirectory;
    }

    public IndexMergerRequest mergedIndexDirectory( StorageAsset mergedIndexDirectory )
    {
        this.mergedIndexDirectory = mergedIndexDirectory;
        return this;
    }

    public boolean isTemporary()
    {
        return temporary;
    }

    public void setTemporary( boolean temporary )
    {
        this.temporary = temporary;
    }


    public IndexMergerRequest temporary( boolean temporary )
    {
        this.temporary = temporary;
        return this;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "IndexMergerRequest{" );
        sb.append( "repositoriesIds=" ).append( repositoriesIds );
        sb.append( ", packIndex=" ).append( packIndex );
        sb.append( ", groupId='" ).append( groupId ).append( '\'' );
        sb.append( ", mergedIndexPath='" ).append( mergedIndexPath ).append( '\'' );
        sb.append( ", mergedIndexTtl=" ).append( mergedIndexTtl );
        sb.append( ", mergedIndexDirectory=" ).append( mergedIndexDirectory );
        sb.append( ", temporary=" ).append( temporary );
        sb.append( '}' );
        return sb.toString();
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

        IndexMergerRequest that = (IndexMergerRequest) o;

        return groupId.equals( that.groupId );
    }

    @Override
    public int hashCode()
    {
        return groupId.hashCode();
    }
}
