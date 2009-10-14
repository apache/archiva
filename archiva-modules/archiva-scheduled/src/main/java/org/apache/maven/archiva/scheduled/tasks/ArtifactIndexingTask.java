package org.apache.maven.archiva.scheduled.tasks;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.codehaus.plexus.taskqueue.Task;

public class ArtifactIndexingTask
    implements Task
{
    public enum Action { ADD, DELETE, FINISH }

    private String repositoryId;
    
    private File resourceFile;
    
    private Action action;
    
    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public long getMaxExecutionTime()
    {
        return 0;
    }

    public File getResourceFile()
    {
        return resourceFile;
    }

    public void setResourceFile( File resourceFile )
    {
        this.resourceFile = resourceFile;
    }

    public Action getAction()
    {
        return action;
    }

    public void setAction( Action action )
    {
        this.action = action;
    }

    @Override
    public String toString()
    {
        return "ArtifactIndexingTask [action=" + action + ", repositoryId=" + repositoryId + ", resourceFile="
            + resourceFile + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( action == null ) ? 0 : action.hashCode() );
        result = prime * result + ( ( repositoryId == null ) ? 0 : repositoryId.hashCode() );
        result = prime * result + ( ( resourceFile == null ) ? 0 : resourceFile.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ArtifactIndexingTask other = (ArtifactIndexingTask) obj;
        if ( action == null )
        {
            if ( other.action != null )
                return false;
        }
        else if ( !action.equals( other.action ) )
            return false;
        if ( repositoryId == null )
        {
            if ( other.repositoryId != null )
                return false;
        }
        else if ( !repositoryId.equals( other.repositoryId ) )
            return false;
        if ( resourceFile == null )
        {
            if ( other.resourceFile != null )
                return false;
        }
        else if ( !resourceFile.equals( other.resourceFile ) )
            return false;
        return true;
    }
}
