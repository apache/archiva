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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.scheduled.DefaultArchivaTaskScheduler;

/**
 * TaskCreator
 * 
 * Convenience class for creating Archiva tasks.
 */
public class TaskCreator
{
    public static RepositoryTask createRepositoryTask( String repositoryId, String taskNameSuffix )
    {
        String suffix = "";
        if( !StringUtils.isEmpty( taskNameSuffix ) )
        {
            suffix = ":" + taskNameSuffix;
        }
        
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setName( DefaultArchivaTaskScheduler.REPOSITORY_JOB + ":" + repositoryId + suffix );
        task.setQueuePolicy( ArchivaTask.QUEUE_POLICY_WAIT );
                
        return task;
    }
    
    public static RepositoryTask createRepositoryTask( String repositoryId, String taskNameSuffix, boolean scanAll )
    {
        RepositoryTask task = createRepositoryTask( repositoryId, taskNameSuffix );
        task.setScanAll( scanAll );
        
        return task;
    }
        
    public static RepositoryTask createRepositoryTask( String repositoryId, String taskNameSuffix, File resourceFile,
                                                       boolean updateRelatedArtifacts )
    {
        RepositoryTask task = createRepositoryTask( repositoryId, taskNameSuffix );
        task.setResourceFile( resourceFile );
        task.setUpdateRelatedArtifacts( updateRelatedArtifacts );
        
        return task;
    }
    
    public static RepositoryTask createRepositoryTask( String repositoryId, String taskNameSuffix, File resourceFile,
                                                       boolean updateRelatedArtifacts, boolean scanAll )
    {
        RepositoryTask task = createRepositoryTask( repositoryId, taskNameSuffix, resourceFile, updateRelatedArtifacts );
        task.setScanAll( scanAll );
        
        return task;
    }
    
    public static ArtifactIndexingTask createIndexingTask( String repositoryId, File resource,
                                                           String action )
    {
        ArtifactIndexingTask task = new ArtifactIndexingTask();
        task.setRepositoryId( repositoryId );
        task.setName( DefaultArchivaTaskScheduler.INDEXING_JOB + ":" + repositoryId + ":" + resource.getName() + ":" +
            action );
        task.setAction( action );
        task.setQueuePolicy( ArchivaTask.QUEUE_POLICY_WAIT );
        task.setResourceFile( resource );
        
        return task;
    }
    
}
