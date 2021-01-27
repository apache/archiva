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
 * Information about index update tasks running on a repository.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class IndexingTask extends RepositoryTaskInfo implements Serializable
{
    private static final long serialVersionUID = -1947200162602613310L;
    /**
     * <code>true</code>, if this task is just updating the existing index.
     */
    private boolean updateOnly;

    public boolean isUpdateOnly( )
    {
        return updateOnly;
    }

    public void setUpdateOnly( boolean updateOnly )
    {
        this.updateOnly = updateOnly;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        IndexingTask that = (IndexingTask) o;

        if ( isFullScan( ) != that.fullScan ) return false;
        if ( updateOnly != that.updateOnly ) return false;
        if ( isRunning( ) != that.isRunning( ) ) return false;
        if ( getMaxExecutionTimeMs( ) != that.getMaxExecutionTimeMs( ) ) return false;
        if ( !getRepositoryId( ).equals( that.getRepositoryId( ) ) ) return false;
        return getResource( ).equals( that.getResource( ) );
    }

    @Override
    public int hashCode( )
    {
        int result = getRepositoryId( ).hashCode( );
        result = 31 * result + ( isFullScan( ) ? 1 : 0 );
        result = 31 * result + ( updateOnly ? 1 : 0 );
        result = 31 * result + getResource( ).hashCode( );
        result = 31 * result + ( isRunning( ) ? 1 : 0 );
        result = 31 * result + (int) ( getMaxExecutionTimeMs( ) ^ ( getMaxExecutionTimeMs( ) >>> 32 ) );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "IndexingTask{" );
        sb.append( "repositoryId='" ).append( getRepositoryId( ) ).append( '\'' );
        sb.append( ", fullRepository=" ).append( isFullScan( ) );
        sb.append( ", updateOnly=" ).append( updateOnly );
        sb.append( ", resource='" ).append( getResource( ) ).append( '\'' );
        sb.append( ", running=" ).append( isRunning( ) );
        sb.append( ", maxExecutionTimeMs=" ).append( getMaxExecutionTimeMs( ) );
        sb.append( '}' );
        return sb.toString( );
    }

}
