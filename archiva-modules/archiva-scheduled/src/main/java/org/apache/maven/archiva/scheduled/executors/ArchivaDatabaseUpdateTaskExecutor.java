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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.updater.DatabaseUpdater;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArchivaDatabaseTaskExecutor 
 *
 * @version $Id$
 * 
 * @plexus.component
 *   role="org.codehaus.plexus.taskqueue.execution.TaskExecutor"
 *   role-hint="database-update"
 */
public class ArchivaDatabaseUpdateTaskExecutor
    implements TaskExecutor, Initializable
{
    private Logger log = LoggerFactory.getLogger( ArchivaDatabaseUpdateTaskExecutor.class );
    
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private DatabaseUpdater databaseUpdater;

    public void initialize()
        throws InitializationException
    {
        log.info( "Initialized " + this.getClass().getName() );
    }

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        DatabaseTask dbtask = (DatabaseTask) task;

        log.info( "Executing task from queue with job name: " + dbtask );
        long time = System.currentTimeMillis();

        try
        {
            log.info( "Task: Updating unprocessed artifacts" );
            databaseUpdater.updateAllUnprocessed();
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new TaskExecutionException( "Error running unprocessed updater", e );
        }

        time = System.currentTimeMillis() - time;

        log.info( "Finished database task in " + time + "ms." );
    }
}
