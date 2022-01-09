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

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="ScanTask", description = "Repository scan task information")
public class ScanTask implements Serializable, RestModel
{
    private static final long serialVersionUID = -681163357370848098L;
    private String repositoryId="";
    private boolean updateRelatedArtifacts;
    private boolean fullRepository;
    private boolean running = false;
    private String resource = "";
    private long maxExecutionTimeMs = 0;

    public static ScanTask of( MetadataScanTask repositoryTask ) {
        ScanTask scanTask = new ScanTask( );
        scanTask.setFullRepository( repositoryTask.isFullScan());
        scanTask.setUpdateRelatedArtifacts( repositoryTask.isUpdateRelatedArtifacts() );
        scanTask.setResource( repositoryTask.getResource() );
        scanTask.setMaxExecutionTimeMs( repositoryTask.getMaxExecutionTimeMs() );
        scanTask.setRepositoryId( repositoryTask.getRepositoryId( ) );
        return scanTask;
    }

    @Schema(name="repository_id", description = "Identifier of the repository, this task is running on")
    public String getRepositoryId( )
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId==null?"":repositoryId;
    }

    @Schema(name="update_related_artifacts", description = "True, if related artifacts are updated too.")
    public boolean isUpdateRelatedArtifacts( )
    {
        return updateRelatedArtifacts;
    }

    public void setUpdateRelatedArtifacts( boolean updateRelatedArtifacts )
    {
        this.updateRelatedArtifacts = updateRelatedArtifacts;
    }

    @Schema(name="full_repository",description = "True, if this is a full repository scan")
    public boolean isFullRepository( )
    {
        return fullRepository;
    }

    public void setFullRepository( boolean fullRepository )
    {
        this.fullRepository = fullRepository;
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

    @Schema(name="resource",description = "Name of the resource to update")
    public String getResource( )
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource==null?"":resource;
    }

    @Schema(name="max_excecution_time_ms",description = "Maximum execution time in ms")
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

        ScanTask scanTask = (ScanTask) o;

        if ( updateRelatedArtifacts != scanTask.updateRelatedArtifacts ) return false;
        if ( fullRepository != scanTask.fullRepository ) return false;
        if ( running != scanTask.running ) return false;
        if ( maxExecutionTimeMs != scanTask.maxExecutionTimeMs ) return false;
        if ( !repositoryId.equals( scanTask.repositoryId ) ) return false;
        return resource.equals( scanTask.resource );
    }

    @Override
    public int hashCode( )
    {
        int result = repositoryId.hashCode( );
        result = 31 * result + ( updateRelatedArtifacts ? 1 : 0 );
        result = 31 * result + ( fullRepository ? 1 : 0 );
        result = 31 * result + ( running ? 1 : 0 );
        result = 31 * result + resource.hashCode( );
        result = 31 * result + (int) ( maxExecutionTimeMs ^ ( maxExecutionTimeMs >>> 32 ) );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "ScanTask{" );
        sb.append( "repositoryId='" ).append( repositoryId ).append( '\'' );
        sb.append( ", updateRelatedArtifacts=" ).append( updateRelatedArtifacts );
        sb.append( ", fullRepository=" ).append( fullRepository );
        sb.append( ", running=" ).append( running );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( ", maxExecutionTimeMs=" ).append( maxExecutionTimeMs );
        sb.append( '}' );
        return sb.toString( );
    }
}
