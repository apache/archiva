package org.apache.maven.archiva.scheduled.executors;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;

import java.io.File;

/**
 * IndexerTaskExecutorTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id:$
 */
public class ArchivaScheduledTaskExecutorTest
    extends PlexusTestCase
{
    private TaskExecutor taskExecutor;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        taskExecutor = (TaskExecutor) lookup( "org.codehaus.plexus.taskqueue.execution.TaskExecutor", "archiva-task-executor" );
       
    }

    public void testExecutor()
        throws TaskExecutionException
    {
        taskExecutor.executeTask( new TestDataRefreshTask() );
    }

    class TestDataRefreshTask
        extends RepositoryTask
    {
        public String getJobName()
        {
            return "TestDataRefresh";
        }
    }
}
