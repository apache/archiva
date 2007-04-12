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

import org.apache.maven.archiva.scheduled.tasks.ArchivaTask;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class is the database job that is executed by the scheduler.
 */
public class DatabaseTaskJob
    extends AbstractJob
{
    static final String TASK_KEY = "EXECUTION";

    static final String TASK_QUEUE = "TASK_QUEUE";

    static final String TASK_QUEUE_POLICY = "TASK_QUEUE_POLICY";

    /**
     * Execute the discoverer and the indexer.
     *
     * @param context
     * @throws org.quartz.JobExecutionException
     *
     */
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        setJobDataMap( dataMap );

        TaskQueue taskQueue = (TaskQueue) dataMap.get( TASK_QUEUE );
        String queuePolicy = dataMap.get( TASK_QUEUE_POLICY ).toString();

        ArchivaTask task = new DatabaseTask();
        task.setName( context.getJobDetail().getName() );

        try
        {
            if ( taskQueue.getQueueSnapshot().size() == 0 )
            {
                taskQueue.put( task );
            }
            else
            {
                if ( ArchivaTask.QUEUE_POLICY_WAIT.equals( queuePolicy ) )
                {
                    taskQueue.put( task );
                }
                else if ( ArchivaTask.QUEUE_POLICY_SKIP.equals( queuePolicy ) )
                {
                    // do not queue anymore, policy is to skip
                }
            }
        }
        catch ( TaskQueueException e )
        {
            throw new JobExecutionException( e );
        }
    }
}
