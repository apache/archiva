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

import org.sonatype.nexus.index.ArtifactContext;

public class ArtifactIndexingTask
    implements ArchivaTask
{
    public static final String ADD = "add";
    
    public static final String DELETE = "delete";
    
    String repositoryId;
    
    String name;
    
    String queuePolicy;

    long maxExecutionTime;
    
    File resourceFile;
    
    ArtifactContext artifactContext;
    
    String action;
    
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
        return maxExecutionTime;
    }

    public void setMaxExecutionTime( long maxExecutionTime )
    {
        this.maxExecutionTime = maxExecutionTime;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getQueuePolicy()
    {
        return queuePolicy;
    }

    public void setQueuePolicy( String queuePolicy )
    {
        this.queuePolicy = queuePolicy;
    }

    public File getResourceFile()
    {
        return resourceFile;
    }

    public void setResourceFile( File resourceFile )
    {
        this.resourceFile = resourceFile;
    }

    public ArtifactContext getArtifactContext()
    {
        return artifactContext;
    }

    public void setArtifactContext( ArtifactContext artifactContext )
    {
        this.artifactContext = artifactContext;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction( String action )
    {
        this.action = action;
    }

}
