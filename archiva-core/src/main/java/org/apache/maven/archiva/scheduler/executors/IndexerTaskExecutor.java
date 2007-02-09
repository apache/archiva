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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.archiva.discoverer.ArtifactDiscoverer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.MetadataDiscoverer;
import org.apache.maven.archiva.discoverer.filter.MetadataFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.record.IndexRecordExistsArtifactFilter;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.archiva.reporting.executor.ReportExecutor;
import org.apache.maven.archiva.reporting.filter.ReportingMetadataFilter;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.store.ReportingStoreException;
import org.apache.maven.archiva.scheduler.task.IndexerTask;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" role-hint="indexer"
 */
public class IndexerTaskExecutor
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
    private RepositoryArtifactIndexFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repoFactory;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.MetadataDiscoverer"
     */
    private Map metadataDiscoverers;

    /**
     * @plexus.requirement role-hint="standard"
     */
    private RepositoryIndexRecordFactory recordFactory;

    /**
     * @plexus.requirement
     */
    private ReportExecutor reportExecutor;

    /**
     * @plexus.requirement role-hint="health"
     */
    private ReportGroup reportGroup;

    private long lastIndexingTime = 0;

    private static final int ARTIFACT_BUFFER_SIZE = 1000;

    public long getLastIndexingTime()
    {
        return lastIndexingTime;
    }

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        IndexerTask indexerTask = (IndexerTask) task;

        getLogger().info( "Executing task from queue with job name: " + indexerTask.getJobName() );

        execute();
    }

    public void execute()
        throws TaskExecutionException
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        File indexPath = new File( configuration.getIndexPath() );

        execute( configuration, indexPath );
    }

    public void executeNowIfNeeded()
        throws TaskExecutionException
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        File indexPath = new File( configuration.getIndexPath() );

        try
        {
            RepositoryArtifactIndex artifactIndex = indexFactory.createStandardIndex( indexPath );
            if ( !artifactIndex.exists() )
            {
                execute( configuration, indexPath );
            }
        }
        catch ( RepositoryIndexException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }
    }

    private void execute( Configuration configuration, File indexPath )
        throws TaskExecutionException
    {
        long time = System.currentTimeMillis();
        getLogger().info( "Starting repository indexing process" );

        RepositoryArtifactIndex index = indexFactory.createStandardIndex( indexPath );

        try
        {
            Collection keys;
            if ( index.exists() )
            {
                keys = index.getAllRecordKeys();
            }
            else
            {
                keys = Collections.EMPTY_LIST;
            }

            for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
            {
                RepositoryConfiguration repositoryConfiguration = (RepositoryConfiguration) i.next();

                if ( repositoryConfiguration.isIndexed() )
                {
                    List blacklistedPatterns = new ArrayList();
                    if ( repositoryConfiguration.getBlackListPatterns() != null )
                    {
                        blacklistedPatterns.addAll( repositoryConfiguration.getBlackListPatterns() );
                    }
                    if ( configuration.getGlobalBlackListPatterns() != null )
                    {
                        blacklistedPatterns.addAll( configuration.getGlobalBlackListPatterns() );
                    }
                    boolean includeSnapshots = repositoryConfiguration.isIncludeSnapshots();

                    ArtifactRepository repository = repoFactory.createRepository( repositoryConfiguration );
                    ReportingDatabase reporter = reportExecutor.getReportDatabase( repository, reportGroup );

                    // keep original value in case there is another process under way
                    long origStartTime = reporter.getStartTime();
                    reporter.setStartTime( System.currentTimeMillis() );

                    // Discovery process
                    String layoutProperty = repositoryConfiguration.getLayout();
                    ArtifactDiscoverer discoverer = (ArtifactDiscoverer) artifactDiscoverers.get( layoutProperty );
                    AndArtifactFilter filter = new AndArtifactFilter();
                    filter.add( new IndexRecordExistsArtifactFilter( keys ) );
                    if ( !includeSnapshots )
                    {
                        filter.add( new SnapshotArtifactFilter() );
                    }

                    // Save some memory by not tracking paths we won't use
                    // TODO: Plexus CDC should be able to inject this configuration
                    discoverer.setTrackOmittedPaths( false );

                    getLogger().info( "Searching repository " + repositoryConfiguration.getName() );
                    List artifacts = discoverer.discoverArtifacts( repository, blacklistedPatterns, filter );

                    if ( !artifacts.isEmpty() )
                    {
                        getLogger().info( "Discovered " + artifacts.size() + " unindexed artifacts" );

                        // Work through these in batches, then flush the project cache.
                        for ( int j = 0; j < artifacts.size(); j += ARTIFACT_BUFFER_SIZE )
                        {
                            int end = j + ARTIFACT_BUFFER_SIZE;
                            List currentArtifacts =
                                artifacts.subList( j, end > artifacts.size() ? artifacts.size() : end );

                            // TODO: proper queueing of this in case it was triggered externally (not harmful to do so at present, but not optimal)

                            // run the reports. Done intermittently to avoid losing track of what is indexed since
                            // that is what the filter is based on.
                            reportExecutor.runArtifactReports( reportGroup, currentArtifacts, repository );

                            index.indexArtifacts( currentArtifacts, recordFactory );

                            // MRM-142 - the project builder retains a lot of objects in its inflexible cache. This is a hack
                            // around that. TODO: remove when it is configurable
                            flushProjectBuilderCacheHack();
                        }
                    }

                    MetadataFilter metadataFilter = new ReportingMetadataFilter( reporter );

                    MetadataDiscoverer metadataDiscoverer = (MetadataDiscoverer) metadataDiscoverers
                        .get( layoutProperty );
                    List metadata =
                        metadataDiscoverer.discoverMetadata( repository, blacklistedPatterns, metadataFilter );

                    if ( !metadata.isEmpty() )
                    {
                        getLogger().info( "Discovered " + metadata.size() + " unprocessed metadata files" );

                        // run the reports
                        reportExecutor.runMetadataReports( reportGroup, metadata, repository );
                    }

                    reporter.setStartTime( origStartTime );
                }
            }
        }
        catch ( RepositoryIndexException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }
        catch ( DiscovererException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }
        catch ( ReportingStoreException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }

        time = System.currentTimeMillis() - time;
        lastIndexingTime = System.currentTimeMillis();
        getLogger().info( "Finished repository indexing process in " + time + "ms" );
    }

    /**
     * @todo remove when no longer needed (MRM-142)
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    private void flushProjectBuilderCacheHack()
    {
        try
        {
            if ( projectBuilder != null )
            {
                getLogger().info( "projectBuilder is type " + projectBuilder.getClass().getName() );

                java.lang.reflect.Field f = projectBuilder.getClass().getDeclaredField( "rawProjectCache" );
                f.setAccessible( true );
                Map cache = (Map) f.get( projectBuilder );
                getLogger().info( "projectBuilder.raw is type " + cache.getClass().getName() );
                cache.clear();

                f = projectBuilder.getClass().getDeclaredField( "processedProjectCache" );
                f.setAccessible( true );
                cache = (Map) f.get( projectBuilder );
                getLogger().info( "projectBuilder.processed is type " + cache.getClass().getName() );
                cache.clear();
            }
        }
        catch ( NoSuchFieldException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }
}
