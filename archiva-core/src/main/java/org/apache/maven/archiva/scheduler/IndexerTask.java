package org.apache.maven.archiva.scheduler;

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
import org.apache.maven.archiva.discovery.ArtifactDiscoverer;
import org.apache.maven.archiva.discovery.DiscovererException;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Task for discovering changes in the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.archiva.scheduler.RepositoryTask" role-hint="indexer"
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
     * @plexus.requirement role="org.apache.maven.archiva.discovery.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    /**
     * @plexus.requirement role-hint="standard"
     */
    private RepositoryIndexRecordFactory recordFactory;

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
        getLogger().info( "Starting repository discovery process" );

        try
        {
            for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
            {
                RepositoryConfiguration repositoryConfiguration = (RepositoryConfiguration) i.next();

                if ( repositoryConfiguration.isIndexed() )
                {
                    // TODO! include global ones
                    String blacklistedPatterns = repositoryConfiguration.getBlackListPatterns();
                    boolean includeSnapshots = repositoryConfiguration.isIncludeSnapshots();

                    ArtifactRepository repository = repoFactory.createRepository( repositoryConfiguration );

                    String layoutProperty = repositoryConfiguration.getLayout();
                    ArtifactDiscoverer discoverer = (ArtifactDiscoverer) artifactDiscoverers.get( layoutProperty );
                    List artifacts =
                        discoverer.discoverArtifacts( repository, "indexer", blacklistedPatterns, includeSnapshots );
                    if ( !artifacts.isEmpty() )
                    {
                        getLogger().info( "Indexing " + artifacts.size() + " new artifacts" );
                        indexArtifacts( artifacts, indexPath );
                    }
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

        time = System.currentTimeMillis() - time;
        getLogger().info( "Finished repository indexing process in " + time + "ms" );
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

    private void indexArtifacts( List artifacts, File indexPath )
        throws RepositoryIndexException
    {
        List records = new ArrayList();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            records.add( recordFactory.createRecord( a ) );
        }

        RepositoryArtifactIndex artifactIndex = indexFactory.createStandardIndex( indexPath );
        artifactIndex.indexRecords( records );
    }
}
