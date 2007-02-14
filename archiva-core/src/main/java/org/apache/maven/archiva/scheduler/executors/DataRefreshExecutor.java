package org.apache.maven.archiva.scheduler.executors;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.discoverer.Discoverer;
import org.apache.maven.archiva.discoverer.DiscovererConsumer;
import org.apache.maven.archiva.discoverer.DiscovererConsumerFactory;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.DiscovererStatistics;
import org.apache.maven.archiva.scheduler.task.DataRefreshTask;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * DataRefreshExecutor 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" 
 *      role-hint="data-refresh"
 */
public class DataRefreshExecutor
    extends AbstractLogEnabled
    implements TaskExecutor
{
    /**
     * Configuration store.
     *
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repoFactory;

    /**
     * @plexus.configuration
     */
    private List consumerNames;

    /**
     * @plexus.requirement
     */
    private Discoverer discoverer;

    /**
     * @plexus.requirement
     */
    private DiscovererConsumerFactory consumerFactory;

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        DataRefreshTask indexerTask = (DataRefreshTask) task;

        getLogger().info( "Executing task from queue with job name: " + indexerTask.getJobName() );

        execute();
    }

    private String toHumanTimestamp( long timestamp )
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        return dateFormat.format( new Date( timestamp ) );
    }

    public void execute()
        throws TaskExecutionException
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        List consumers = new ArrayList();

        for ( Iterator it = consumerNames.iterator(); it.hasNext(); )
        {
            String name = (String) it.next();
            try
            {
                DiscovererConsumer consumer = consumerFactory.createConsumer( name );
                consumers.add( consumer );
            }
            catch ( DiscovererException e )
            {
                throw new TaskExecutionException( e.getMessage(), e );
            }
        }

        long time = System.currentTimeMillis();

        for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
        {
            RepositoryConfiguration repositoryConfiguration = (RepositoryConfiguration) i.next();

            if ( !repositoryConfiguration.isIndexed() )
            {
                continue;
            }

            ArtifactRepository repository = repoFactory.createRepository( repositoryConfiguration );

            List filteredConsumers = filterConsumers( consumers, repository );

            DiscovererStatistics stats = discoverer.scanRepository( repository, filteredConsumers,
                                                                    repositoryConfiguration.isIncludeSnapshots() );

            getLogger().info( "----------------------------------------------------" );
            getLogger().info( "Scan of Repository: " + repository.getId() );
            getLogger().info( "   Started : " + toHumanTimestamp( stats.getTimestampStarted() ) );
            getLogger().info( "   Finished: " + toHumanTimestamp( stats.getTimestampFinished() ) );
            // TODO: pretty print ellapsed time.
            getLogger().info( "   Duration: " + stats.getElapsedMilliseconds() + "ms" );
            getLogger().info( "   Files   : " + stats.getFilesIncluded() );
            getLogger().info( "   Consumed: " + stats.getFilesConsumed() );
            getLogger().info( "   Skipped : " + stats.getFilesSkipped() );
        }

        time = System.currentTimeMillis() - time;

        getLogger().info( "Finished data refresh process in " + time + "ms." );
    }

    /**
     * Not all consumers work with all repositories.
     * This will filter out those incompatible consumers based on the provided repository.
     * 
     * @param consumers the initial list of consumers.
     * @param repository the repository to test consumer against.
     * @return the filtered list of consumers.
     */
    private List filterConsumers( List consumers, ArtifactRepository repository )
    {
        List filtered = new ArrayList();

        for ( Iterator it = consumers.iterator(); it.hasNext(); )
        {
            DiscovererConsumer consumer = (DiscovererConsumer) it.next();
            if ( consumer.init( repository ) )
            {
                // Approved!
                filtered.add( consumer );
            }
            else
            {
                getLogger().info( "Disabling consumer [" + consumer.getName() + "] for repository " + repository );
            }
        }

        return filtered;
    }

}
