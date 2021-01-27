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

import java.time.OffsetDateTime;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class RepositoryTaskInfo
{
    /**
     * The repository identifier
     */
    protected String repositoryId = "";
    /**
     * <code>true</code>, if all files are scanned. <code>false</code>, if only updated files are scanned
     */
    protected boolean fullScan;
    /**
     * The scanned resource, if this is not a full repository scan
     */
    protected String resource = "";
    /**
     * Running status of the task.
     */
    protected boolean running = false;
    /**
     * The maximum execution time set for this task.
     */
    protected long maxExecutionTimeMs = 0;
    /**
     * The time when the status check was built
     */
    OffsetDateTime checkTime;

    public RepositoryTaskInfo( )
    {
        this.checkTime = OffsetDateTime.now( );
    }

    public RepositoryTaskInfo( OffsetDateTime checkTime )
    {
        this.checkTime = checkTime;
    }

    public String getRepositoryId( )
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId==null?"":repositoryId;
    }

    public boolean isFullScan( )
    {
        return fullScan;
    }

    public void setFullScan( boolean fullScan )
    {
        this.fullScan = fullScan;
    }

    public String getResource( )
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource == null ? "" : resource;
    }

    public boolean isRunning( )
    {
        return running;
    }

    public void setRunning( boolean running )
    {
        this.running = running;
    }

    public long getMaxExecutionTimeMs( )
    {
        return maxExecutionTimeMs;
    }

    public void setMaxExecutionTimeMs( long maxExecutionTimeMs )
    {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }

    public OffsetDateTime getCheckTime( )
    {
        return checkTime;
    }

    public void setCheckTime( OffsetDateTime checkTime )
    {
        this.checkTime = checkTime;
    }
}
