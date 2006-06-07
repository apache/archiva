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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.DefaultArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.MetadataDiscoverer;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.MetadataRepositoryIndex;
import org.apache.maven.repository.indexing.PomRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.apache.maven.repository.manager.web.job.Configuration;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
     * @plexus.requirement
     */
    private Configuration config;

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
     * Executes discoverer and indexer if an index does not exist yet
     *
     * @throws MalformedURLException
     * @throws RepositoryIndexException
     */
    public void executeDiscovererIfIndexDoesNotExist()
        throws MalformedURLException, RepositoryIndexException
    {
        Properties props = config.getProperties();
        String indexPath = props.getProperty( "index.path" );

        File indexDir = new File( indexPath );
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
        Properties props = config.getProperties();
        String indexPath = props.getProperty( "index.path" );
        String blacklistedPatterns = props.getProperty( "blacklist.patterns" );
        boolean includeSnapshots = Boolean.valueOf( props.getProperty( "include.snapshots" ) ).booleanValue();
        boolean convertSnapshots = Boolean.valueOf( props.getProperty( "convert.snapshots" ) ).booleanValue();

        ArtifactRepository defaultRepository = getDefaultRepository();

        getLogger().info( "[DiscovererExecution] Started discovery and indexing.." );
        String layoutProperty = props.getProperty( "layout" );
        ArtifactDiscoverer discoverer = (ArtifactDiscoverer) artifactDiscoverers.get( layoutProperty );
        List artifacts = discoverer.discoverArtifacts( defaultRepository, blacklistedPatterns, includeSnapshots );
        indexArtifact( artifacts, indexPath, defaultRepository );

        List models = discoverer.discoverStandalonePoms( defaultRepository, blacklistedPatterns, convertSnapshots );
        indexPom( models, indexPath, defaultRepository );

        MetadataDiscoverer metadataDiscoverer = (MetadataDiscoverer) metadataDiscoverers.get( layoutProperty );
        List metadataList =
            metadataDiscoverer.discoverMetadata( new File( defaultRepository.getBasedir() ), blacklistedPatterns );
        indexMetadata( metadataList, indexPath, new File( defaultRepository.getBasedir() ), config.getLayout() );
        getLogger().info( "[DiscovererExecution] Finished discovery and indexing." );
    }

    /**
     * Index the artifacts in the list
     *
     * @param artifacts  the artifacts to be indexed
     * @param indexPath  the path to the index file
     * @param repository the repository where the artifacts are located
     */
    protected void indexArtifact( List artifacts, String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        ArtifactRepositoryIndex artifactIndex = indexFactory.createArtifactRepositoryIndex( indexPath, repository );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            artifactIndex.indexArtifact( artifact );

            if ( artifactIndex.isOpen() )
            {
                artifactIndex.optimize();
                artifactIndex.close();
            }
        }
    }

    /**
     * Index the metadata in the list
     *
     * @param metadataList   the metadata to be indexed
     * @param indexPath      the path to the index file
     * @param repositoryBase the repository where the metadata are located
     */
    protected void indexMetadata( List metadataList, String indexPath, File repositoryBase,
                                  ArtifactRepositoryLayout layout )
        throws RepositoryIndexException, MalformedURLException
    {
        String repoDir = repositoryBase.toURL().toString();
        ArtifactRepository repository =
            repoFactory.createArtifactRepository( "repository", repoDir, layout, null, null );

        MetadataRepositoryIndex metadataIndex = indexFactory.createMetadataRepositoryIndex( indexPath, repository );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) iter.next();
            metadataIndex.index( repoMetadata );

            if ( metadataIndex.isOpen() )
            {
                metadataIndex.optimize();
                metadataIndex.close();
            }
        }
    }

    /**
     * Index the poms in the list
     *
     * @param models     list of poms that will be indexed
     * @param indexPath  the path to the index
     * @param repository the artifact repository where the poms were discovered
     */
    protected void indexPom( List models, String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        PomRepositoryIndex pomIndex = indexFactory.createPomRepositoryIndex( indexPath, repository );
        for ( Iterator iter = models.iterator(); iter.hasNext(); )
        {
            Model model = (Model) iter.next();
            pomIndex.indexPom( model );

            if ( pomIndex.isOpen() )
            {
                pomIndex.optimize();
                pomIndex.close();
            }
        }
    }

    /**
     * Method that creates the artifact repository
     *
     * @return an ArtifactRepository instance
     * @throws java.net.MalformedURLException
     */
    protected ArtifactRepository getDefaultRepository()
        throws MalformedURLException
    {
        File repositoryDirectory = new File( config.getRepositoryDirectory() );
        String repoDir = repositoryDirectory.toURL().toString();
        ArtifactRepositoryFactory repoFactory = new DefaultArtifactRepositoryFactory();

        return repoFactory.createArtifactRepository( "test", repoDir, config.getLayout(), null, null );
    }

    /**
     * Method that sets the configuration object
     *
     * @param config
     */
    public void setConfiguration( Configuration config )
    {
        this.config = config;
    }

    /**
     * Returns the cofiguration
     *
     * @return a Configuration object that contains the configuration values
     */
    public Configuration getConfiguration()
    {
        return config;
    }
}
