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

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class MetadataScanTask extends RepositoryTaskInfo implements Serializable
{
    private static final long serialVersionUID = -681163357370848098L;
    /**
     * <code>true</code> if related artifacts are updated too.
     */
    private boolean updateRelatedArtifacts;

    public boolean isUpdateRelatedArtifacts( )
    {
        return updateRelatedArtifacts;
    }

    public void setUpdateRelatedArtifacts( boolean updateRelatedArtifacts )
    {
        this.updateRelatedArtifacts = updateRelatedArtifacts;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        MetadataScanTask scanTask = (MetadataScanTask) o;

        if ( updateRelatedArtifacts != scanTask.updateRelatedArtifacts ) return false;
        if ( fullScan != scanTask.fullScan ) return false;
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
        result = 31 * result + ( fullScan ? 1 : 0 );
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
        sb.append( ", fullRepository=" ).append( fullScan );
        sb.append( ", running=" ).append( running );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( ", maxExecutionTimeMs=" ).append( maxExecutionTimeMs );
        sb.append( '}' );
        return sb.toString( );
    }
}
