package org.apache.maven.archiva.scheduled;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;

/**
 * The component that takes care of scheduling in the application.
 *
 */
public interface ArchivaTaskScheduler
{
    /**
     * The Plexus component role.
     */
    public final static String ROLE = ArchivaTaskScheduler.class.getName();

    public boolean isProcessingAnyRepositoryTask()
        throws ArchivaException;

    public boolean isProcessingDatabaseTask()
        throws ArchivaException;

    public boolean isProcessingRepositoryTask( String repositoryId )
        throws ArchivaException;
    
    public boolean isProcessingRepositoryTaskWithName( String taskName )
        throws ArchivaException;

    public void queueDatabaseTask( DatabaseTask task )
        throws TaskQueueException;

    public void queueRepositoryTask( RepositoryTask task )
        throws TaskQueueException;

    public void scheduleDatabaseTasks()
        throws TaskExecutionException;

    public void startup()
        throws ArchivaException;
}
