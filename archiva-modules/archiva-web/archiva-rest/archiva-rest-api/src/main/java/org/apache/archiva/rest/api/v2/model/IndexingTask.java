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

import java.io.Serializable;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="IndexingTask",description = "Information about indexing tasks")
public class IndexingTask implements Serializable, RestModel
{
    private static final long serialVersionUID = -1947200162602613310L;
    private String repositoryId = "";
    private boolean fullRepository;
    private boolean updateOnly;
    private String resource = "";
    private boolean running = false;
    private long maxExecutionTimeMs = 0;

    public static IndexingTask of( org.apache.archiva.admin.model.beans.IndexingTask repositoryTask ) {
        IndexingTask indexingTask = new IndexingTask( );
        indexingTask.setFullRepository( repositoryTask.isFullScan());
        indexingTask.setUpdateOnly( repositoryTask.isUpdateOnly() );
        indexingTask.setResource( repositoryTask.getResource() );
        indexingTask.setMaxExecutionTimeMs( repositoryTask.getMaxExecutionTimeMs() );
        indexingTask.setRepositoryId( repositoryTask.getRepositoryId() );
        return indexingTask;
    }

    @Schema(name="repository_id",description = "Identifier of the repository this task is running on")
    public String getRepositoryId( )
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId==null?"":repositoryId;
    }

    @Schema( name = "full_repository", description = "True, if this is a full repository index scan" )
    public boolean isFullRepository( )
    {
        return fullRepository;
    }

    public void setFullRepository( boolean fullRepository )
    {
        this.fullRepository = fullRepository;
    }

    @Schema(name="update_only", description = "True, if this is only updating the index with new files")
    public boolean isUpdateOnly( )
    {
        return updateOnly;
    }

    public void setUpdateOnly( boolean updateOnly )
    {
        this.updateOnly = updateOnly;
    }

    @Schema(name="resource",description = "The resource that should be updated, if this is not a full repo update")
    public String getResource( )
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource == null ? "" : resource;
    }

    @Schema(name="running", description = "True, if this task is currently running")
    public boolean isRunning( )
    {
        return running;
    }

    public void setRunning( boolean running )
    {
        this.running = running;
    }

    @Schema(name="max_execution_time_ms",description = "Maximum task execution time in ms")
    public long getMaxExecutionTimeMs( )
    {
        return maxExecutionTimeMs;
    }

    public void setMaxExecutionTimeMs( long maxExecutionTimeMs )
    {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        IndexingTask that = (IndexingTask) o;

        if ( fullRepository != that.fullRepository ) return false;
        if ( updateOnly != that.updateOnly ) return false;
        if ( running != that.running ) return false;
        if ( maxExecutionTimeMs != that.maxExecutionTimeMs ) return false;
        if ( !repositoryId.equals( that.repositoryId ) ) return false;
        return resource.equals( that.resource );
    }

    @Override
    public int hashCode( )
    {
        int result = repositoryId.hashCode( );
        result = 31 * result + ( fullRepository ? 1 : 0 );
        result = 31 * result + ( updateOnly ? 1 : 0 );
        result = 31 * result + resource.hashCode( );
        result = 31 * result + ( running ? 1 : 0 );
        result = 31 * result + (int) ( maxExecutionTimeMs ^ ( maxExecutionTimeMs >>> 32 ) );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "IndexingTask{" );
        sb.append( "repositoryId='" ).append( repositoryId ).append( '\'' );
        sb.append( ", fullRepository=" ).append( fullRepository );
        sb.append( ", updateOnly=" ).append( updateOnly );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( ", running=" ).append( running );
        sb.append( ", maxExecutionTimeMs=" ).append( maxExecutionTimeMs );
        sb.append( '}' );
        return sb.toString( );
    }

}
