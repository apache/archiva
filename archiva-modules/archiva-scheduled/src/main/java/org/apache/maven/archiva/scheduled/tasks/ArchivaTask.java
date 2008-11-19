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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.taskqueue.Task;

/**
 * A repository task.
 *
 */
public interface ArchivaTask
    extends Task
{
    public static final String QUEUE_POLICY_WAIT = "wait";

    public static final String QUEUE_POLICY_SKIP = "skip";

    /**
     * Gets the queue policy for this task.
     *
     * @return Queue policy for this task
     */
    public String getQueuePolicy();

    /**
     * Sets the queue policy for this task.
     *
     * @param policy
     */
    public void setQueuePolicy( String policy );

    /**
     * Sets the job name to represent a group of similar / identical job tasks.  Can be used to check the
     * task queue for similar / identical job tasks.
     */
    public void setName( String name );

    /**
     * obtains the name of the task
     * @return
     */
    public String getName();

    public long getMaxExecutionTime();

    public void setMaxExecutionTime( long maxExecutionTime );


   
}
