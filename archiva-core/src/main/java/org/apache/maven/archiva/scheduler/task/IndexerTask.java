package org.apache.maven.archiva.scheduler.task;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.discoverer.ArtifactDiscoverer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.MetadataDiscoverer;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.record.IndexRecordExistsArtifactFilter;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.archiva.reporting.ArtifactReportProcessor;
import org.apache.maven.archiva.reporting.ReportingDatabase;
import org.apache.maven.archiva.reporting.ReportingStore;
import org.apache.maven.archiva.reporting.ReportingStoreException;
import org.apache.maven.archiva.scheduler.TaskExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Task for discovering changes in the repository and updating the index accordingly.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.archiva.scheduler.task.RepositoryTask" role-hint="indexer"
 */
public class IndexerTask
    extends AbstractLogEnabled
    implements RepositoryTask
{
    /**
     * Configuration store.
     *
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

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
     * @plexus.requirement role="org.apache.maven.archiva.reporting.ArtifactReportProcessor"
     */
    private List artifactReports;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.MetadataDiscoverer"
     */
    private Map metadataDiscoverers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.reporting.MetadataReportProcessor"
     */
    private List metadataReports;

    /**
     * @plexus.requirement role-hint="standard"
     */
    private RepositoryIndexRecordFactory recordFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    private static final int ARTIFACT_BUFFER_SIZE = 1000;

    /**
     * @plexus.requirement
     */
    private ReportingStore reportingStore;

    public void execute()
        throws TaskExecutionException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }

        File indexPath = new File( configuration.getIndexPath() );

        execute( configuration, indexPath );
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

                    getLogger().debug(
                        "Reading previous report database from repository " + repositoryConfiguration.getName() );
                    ReportingDatabase reporter = reportingStore.getReportsFromStore( repository );

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

                            // run the reports
                            runArtifactReports( currentArtifacts, reporter );

                            index.indexArtifacts( currentArtifacts, recordFactory );
                        }

                        // MNG-142 - the project builder retains a lot of objects in its inflexible cache. This is a hack
                        // around that. TODO: remove when it is configurable
                        flushProjectBuilderCacheHack();
                    }

                    // TODO! use reporting manager as a filter
                    MetadataDiscoverer metadataDiscoverer =
                        (MetadataDiscoverer) metadataDiscoverers.get( layoutProperty );
                    metadataDiscoverer.discoverMetadata( repository, blacklistedPatterns );

                    //TODO! metadata reporting

                    reportingStore.storeReports( reporter, repository );
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
        getLogger().info( "Finished repository indexing process in " + time + "ms" );
    }

    private void runArtifactReports( List artifacts, ReportingDatabase reporter )
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            ArtifactRepository repository = artifact.getRepository();

            Model model = null;
            try
            {
                Artifact pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                              artifact.getArtifactId(),
                                                                              artifact.getVersion() );
                MavenProject project =
                    projectBuilder.buildFromRepository( pomArtifact, Collections.EMPTY_LIST, repository );

                model = project.getModel();
            }
            catch ( ProjectBuildingException e )
            {
                reporter.addWarning( artifact, "Error reading project model: " + e );
            }
            runArtifactReports( artifact, model, reporter );
        }
    }

    private void runArtifactReports( Artifact artifact, Model model, ReportingDatabase reporter )
    {
        // TODO: should the report set be limitable by configuration?
        for ( Iterator i = artifactReports.iterator(); i.hasNext(); )
        {
            ArtifactReportProcessor report = (ArtifactReportProcessor) i.next();

            report.processArtifact( artifact, model, reporter );
        }
    }

    public void executeNowIfNeeded()
        throws TaskExecutionException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }

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

    /**
     * @todo remove when no longer needed (MNG-142)
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    private void flushProjectBuilderCacheHack()
    {
        try
        {
            if ( projectBuilder != null )
            {
                java.lang.reflect.Field f = projectBuilder.getClass().getDeclaredField( "rawProjectCache" );
                f.setAccessible( true );
                Map cache = (Map) f.get( projectBuilder );
                cache.clear();

                f = projectBuilder.getClass().getDeclaredField( "processedProjectCache" );
                f.setAccessible( true );
                cache = (Map) f.get( projectBuilder );
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
