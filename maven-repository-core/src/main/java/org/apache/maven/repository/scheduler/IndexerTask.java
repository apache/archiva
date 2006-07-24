package org.apache.maven.repository.scheduler;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.MetadataDiscoverer;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.MetadataRepositoryIndex;
import org.apache.maven.repository.indexing.PomRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Task for discovering changes in the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.repository.scheduler.RepositoryTask" role-hint="indexer"
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
    private RepositoryIndexingFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repoFactory;

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.MetadataDiscoverer"
     */
    private Map metadataDiscoverers;

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
            String blacklistedPatterns = configuration.getDiscoveryBlackListPatterns();
            boolean includeSnapshots = configuration.isDiscoverSnapshots();

            ArtifactRepository defaultRepository = repoFactory.createRepository( configuration );

            String layoutProperty = configuration.getRepositoryLayout();
            ArtifactDiscoverer discoverer = (ArtifactDiscoverer) artifactDiscoverers.get( layoutProperty );
            List artifacts = discoverer.discoverArtifacts( defaultRepository, blacklistedPatterns, includeSnapshots );
            if ( !artifacts.isEmpty() )
            {
                getLogger().info( "Indexing " + artifacts.size() + " new artifacts" );
                indexArtifact( artifacts, indexPath, defaultRepository );
            }

            // TODO: I believe this is incorrect, since it only discovers standalone POMs, not the individual artifacts!
            List models = discoverer.discoverStandalonePoms( defaultRepository, blacklistedPatterns, includeSnapshots );
            if ( !models.isEmpty() )
            {
                getLogger().info( "Indexing " + models.size() + " new POMs" );
                indexPom( models, indexPath, defaultRepository );
            }

            MetadataDiscoverer metadataDiscoverer = (MetadataDiscoverer) metadataDiscoverers.get( layoutProperty );
            List metadataList =
                metadataDiscoverer.discoverMetadata( new File( defaultRepository.getBasedir() ), blacklistedPatterns );
            if ( !metadataList.isEmpty() )
            {
                getLogger().info( "Indexing " + metadataList.size() + " new metadata files" );
                indexMetadata( metadataList, indexPath, defaultRepository );
            }
        }
        catch ( RepositoryIndexException e )
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
            ArtifactRepository repository = repoFactory.createRepository( configuration );
            ArtifactRepositoryIndex artifactIndex = indexFactory.createArtifactRepositoryIndex( indexPath, repository );
            if ( !artifactIndex.indexExists() )
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
     * Index the artifacts in the list
     *
     * @param artifacts  the artifacts to be indexed
     * @param indexPath  the path to the index file
     * @param repository the repository where the artifacts are located
     */
    protected void indexArtifact( List artifacts, File indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        ArtifactRepositoryIndex artifactIndex = indexFactory.createArtifactRepositoryIndex( indexPath, repository );
        artifactIndex.indexArtifacts( artifacts );
        artifactIndex.optimize();
    }

    /**
     * Index the metadata in the list
     *
     * @param metadataList the metadata to be indexed
     * @param indexPath    the path to the index file
     */
    protected void indexMetadata( List metadataList, File indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        MetadataRepositoryIndex metadataIndex = indexFactory.createMetadataRepositoryIndex( indexPath, repository );
        metadataIndex.indexMetadata( metadataList );
        metadataIndex.optimize();
    }

    /**
     * Index the poms in the list
     *
     * @param models     list of poms that will be indexed
     * @param indexPath  the path to the index
     * @param repository the artifact repository where the poms were discovered
     */
    protected void indexPom( List models, File indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        PomRepositoryIndex pomIndex = indexFactory.createPomRepositoryIndex( indexPath, repository );
        pomIndex.indexPoms( models );
        pomIndex.optimize();
    }
}
