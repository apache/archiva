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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.common.utils.DateUtil;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.RepositoryDAO;
import org.apache.maven.archiva.database.constraints.MostRecentRepositoryScanStatistics;
import org.apache.maven.archiva.database.updater.DatabaseUpdater;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.scanner.RepositoryScanner;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;

import java.util.List;

/**
 *
 * @author <a href="mailto:jmcconnell@apache.org">Jesse McConnell</a>
 * @version $Id:$
 * 
 * @plexus.component role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" 
 *      role-hint="archiva-task-executor"
 */
public class ArchivaScheduledTaskExecutor
    extends AbstractLogEnabled
    implements TaskExecutor
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private DatabaseUpdater databaseUpdater;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private RepositoryDAO repositoryDAO;

    /**
     * The repository scanner component.
     * 
     * @plexus.requirement
     */
    private RepositoryScanner repoScanner;

    public void executeTask( Task task )
        throws TaskExecutionException
    {

        if ( task instanceof DatabaseTask )
        {
            executeDatabaseTask( (DatabaseTask) task );
        }
        else if ( task instanceof RepositoryTask )
        {
            executeRepositoryTask( (RepositoryTask) task );
        }
        else
        {
            throw new TaskExecutionException( "Unknown Task: " + task.toString() );
        }

    }

    private void executeDatabaseTask( DatabaseTask task )
        throws TaskExecutionException
    {
        getLogger().info( "Executing task from queue with job name: " + task.getName() );
        long time = System.currentTimeMillis();

        try
        {
            getLogger().info( "Task: Updating unprocessed artifacts" );
            databaseUpdater.updateAllUnprocessed();
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new TaskExecutionException( "Error running unprocessed updater", e );
        }

        try
        {
            getLogger().info( "Task: Updating processed artifacts" );
            databaseUpdater.updateAllProcessed();
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new TaskExecutionException( "Error running processed updater", e );
        }

        time = System.currentTimeMillis() - time;

        getLogger().info( "Finished database task in " + time + "ms." );

    }

    private void executeRepositoryTask( RepositoryTask task )
        throws TaskExecutionException
    {
        getLogger().info( "Executing task from queue with job name: " + task.getName() );

        try
        {
            ArchivaRepository arepo = repositoryDAO.getRepository( task.getRepositoryId() );

            long sinceWhen = RepositoryScanner.FRESH_SCAN;

            List results = dao.query( new MostRecentRepositoryScanStatistics( arepo.getId() ) );

            if ( CollectionUtils.isNotEmpty( results ) )
            {
                RepositoryContentStatistics lastStats = (RepositoryContentStatistics) results.get( 0 );
                sinceWhen = lastStats.getWhenGathered().getTime() + lastStats.getDuration();
            }

            RepositoryContentStatistics stats = repoScanner.scan( arepo, sinceWhen );

            dao.save( stats );

            getLogger().info( "Finished repository task: " + DateUtil.getDuration( stats.getDuration() ) + "." );
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new TaskExecutionException( "Database error when executing repository job.", e );
        }
        catch ( RepositoryException e )
        {
            throw new TaskExecutionException( "Repository error when executing repository job.", e );
        }
    }
}
