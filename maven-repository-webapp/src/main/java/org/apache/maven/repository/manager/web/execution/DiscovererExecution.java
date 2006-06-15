package org.apache.maven.repository.manager.web.execution;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.lucene.index.IndexReader;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.DefaultArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.MetadataDiscoverer;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.MetadataRepositoryIndex;
import org.apache.maven.repository.indexing.PomRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * This is the class that executes the discoverer and indexer.
 *
 * @plexus.component role="org.apache.maven.repository.manager.web.execution.DiscovererExecution"
 * @todo note that a legacy repository will fail due to lack of metadata discoverer
 */
public class DiscovererExecution
    extends AbstractLogEnabled
{

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.MetadataDiscoverer"
     */
    private Map metadataDiscoverers;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexingFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repoFactory;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * Executes discoverer and indexer if an index does not exist yet
     *
     * @param indexDir
     * @throws MalformedURLException
     * @throws RepositoryIndexException
     */
    public void executeDiscovererIfIndexDoesNotExist( File indexDir )
        throws MalformedURLException, RepositoryIndexException
    {
        boolean isExisting = false;

        if ( IndexReader.indexExists( indexDir ) )
        {
            isExisting = true;
        }

        if ( !isExisting )
        {
            executeDiscoverer();
        }
    }

    /**
     * Method that executes the discoverer and indexer
     */
    public void executeDiscoverer()
        throws MalformedURLException, RepositoryIndexException
    {
        Configuration configuration = new Configuration(); // TODO!
        File indexPath = new File( configuration.getIndexPath() );
        String blacklistedPatterns = configuration.getDiscoveryBlackListPatterns();
        boolean includeSnapshots = configuration.isDiscoverSnapshots();

        ArtifactRepository defaultRepository = getDefaultRepository( configuration );

        getLogger().info( "[DiscovererExecution] Started discovery and indexing.." );
        String layoutProperty = configuration.getRepositoryLayout();
        ArtifactDiscoverer discoverer = (ArtifactDiscoverer) artifactDiscoverers.get( layoutProperty );
        List artifacts = discoverer.discoverArtifacts( defaultRepository, blacklistedPatterns, includeSnapshots );
        indexArtifact( artifacts, indexPath, defaultRepository );

        List models = discoverer.discoverStandalonePoms( defaultRepository, blacklistedPatterns, includeSnapshots );
        indexPom( models, indexPath, defaultRepository );

        MetadataDiscoverer metadataDiscoverer = (MetadataDiscoverer) metadataDiscoverers.get( layoutProperty );
        List metadataList =
            metadataDiscoverer.discoverMetadata( new File( defaultRepository.getBasedir() ), blacklistedPatterns );
        indexMetadata( metadataList, indexPath, defaultRepository );
        getLogger().info( "[DiscovererExecution] Finished discovery and indexing." );
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
        throws RepositoryIndexException, MalformedURLException
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

    /**
     * Method that creates the artifact repository
     *
     * @return an ArtifactRepository instance
     * @throws java.net.MalformedURLException
     */
    protected ArtifactRepository getDefaultRepository( Configuration configuration )
        throws MalformedURLException
    {
        // TODO! share with general search action, should only instantiate once
        File repositoryDirectory = new File( configuration.getRepositoryDirectory() );
        String repoDir = repositoryDirectory.toURL().toString();
        ArtifactRepositoryFactory repoFactory = new DefaultArtifactRepositoryFactory();

        ArtifactRepositoryLayout layout =
            (ArtifactRepositoryLayout) repositoryLayouts.get( configuration.getRepositoryLayout() );
        return repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );
    }
}
