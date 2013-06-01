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

import java.util.Collection;

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

    private String mergedIndexPath = "/.indexer";

    private int mergedIndexTtl;

    public IndexMergerRequest( Collection<String> repositoriesIds, boolean packIndex, String groupId )
    {
        this.repositoriesIds = repositoriesIds;
        this.packIndex = packIndex;
        this.groupId = groupId;
    }

    /**
     * @since 1.4-M4
     */
    public IndexMergerRequest(Collection<String> repositoriesIds, boolean packIndex, String groupId,
                              String mergedIndexPath, int mergedIndexTtl)
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

    public int getMergedIndexTtl() {
        return mergedIndexTtl;
    }

    public void setMergedIndexTtl(int mergedIndexTtl) {
        this.mergedIndexTtl = mergedIndexTtl;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "IndexMergerRequest{" );
        sb.append( "repositoriesIds=" ).append( repositoriesIds );
        sb.append( ", packIndex=" ).append( packIndex );
        sb.append( ", groupId='" ).append( groupId ).append( '\'' );
        sb.append( ", mergedIndexPath='" ).append( mergedIndexPath ).append( '\'' );
        sb.append( ", mergedIndexTtl='" ).append( mergedIndexTtl ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
