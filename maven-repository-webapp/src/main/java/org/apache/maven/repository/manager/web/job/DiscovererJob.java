package org.apache.maven.repository.manager.web.job;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.DefaultArtifactDiscoverer;
import org.apache.maven.repository.discovery.DefaultMetadataDiscoverer;
import org.apache.maven.repository.discovery.LegacyArtifactDiscoverer;
import org.apache.maven.repository.discovery.MetadataDiscoverer;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.MetadataRepositoryIndex;
import org.apache.maven.repository.indexing.PomRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

/**
 * This class executes the discoverer and the indexer.
 *
 * @plexus.component role="org.apache.maven.repository.manager.web.job.DiscovererJob"
 */
public class DiscovererJob
    extends AbstractJob
{
    public static final String ROLE = DiscovererJob.class.getName();

    private ArtifactDiscoverer defaultArtifactDiscoverer;

    private ArtifactDiscoverer legacyArtifactDiscoverer;

    private MetadataDiscoverer defaultMetadataDiscoverer;

    private RepositoryIndexingFactory indexFactory;

    private ArtifactRepositoryLayout layout;

    private ArtifactRepositoryFactory repoFactory;

    public static String MAP_INDEXPATH = "INDEXPATH";

    public static String MAP_LAYOUT = "LAYOUT";

    public static String MAP_DEFAULT_REPOSITORY = "DEFAULT_REPOSITORY";

    public static String MAP_BLACKLIST = "BLACKLISTED_PATTERNS";

    public static String MAP_SNAPSHOTS = "INCLUDE_SNAPSHOTS";

    public static String MAP_CONVERT = "CONVERT_SNAPSHOTS";

    public static String MAP_DEF_ARTIFACT_DISCOVERER = "DEFAULT_ARTIFACT_DISCOVERER";

    public static String MAP_LEG_ARTIFACT_DISCOVERER = "LEGACY_ARTIFACT_DISCOVERER";

    public static String MAP_DEF_METADATA_DISCOVERER = "DEFAULT_METADATA_DISCOVERER";

    public static String MAP_IDX_FACTORY = "INDEX_FACTORY";

    public static String MAP_REPO_LAYOUT = "REPOSITORY_LAYOUT";

    public static String MAP_REPO_FACTORY = "REPOSITORY_FACTORY";

    /**
     * Execute the discoverer and the indexer.
     *
     * @param context
     * @throws org.quartz.JobExecutionException
     *
     */
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        getLogger().info( "Start execution of DiscovererJob.." );
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        //configuration values specified in properties file
        String indexPath = (String) dataMap.get( MAP_INDEXPATH );
        ArtifactRepository defaultRepository = (ArtifactRepository) dataMap.get( MAP_DEFAULT_REPOSITORY );
        String blacklistedPatterns = (String) dataMap.get( MAP_BLACKLIST );
        boolean includeSnapshots = ( (Boolean) dataMap.get( MAP_SNAPSHOTS ) ).booleanValue();
        boolean convertSnapshots = ( (Boolean) dataMap.get( MAP_CONVERT ) ).booleanValue();

        //plexus components created in BaseAction
        defaultArtifactDiscoverer = (DefaultArtifactDiscoverer) dataMap.get( MAP_DEF_ARTIFACT_DISCOVERER );
        legacyArtifactDiscoverer = (LegacyArtifactDiscoverer) dataMap.get( MAP_LEG_ARTIFACT_DISCOVERER );
        defaultMetadataDiscoverer = (DefaultMetadataDiscoverer) dataMap.get( MAP_DEF_METADATA_DISCOVERER );
        indexFactory = (RepositoryIndexingFactory) dataMap.get( MAP_IDX_FACTORY );
        layout = (ArtifactRepositoryLayout) dataMap.get( MAP_REPO_LAYOUT );
        repoFactory = (ArtifactRepositoryFactory) dataMap.get( MAP_REPO_FACTORY );

        try
        {
            List artifacts;
            if ( dataMap.get( MAP_LAYOUT ).equals( "default" ) )
            {
                artifacts = defaultArtifactDiscoverer.discoverArtifacts( defaultRepository, blacklistedPatterns,
                                                                         includeSnapshots );
                indexArtifact( artifacts, indexPath, defaultRepository );

                List models = defaultArtifactDiscoverer.discoverStandalonePoms( defaultRepository, blacklistedPatterns,
                                                                                convertSnapshots );
                indexPom( models, indexPath, defaultRepository );

                List metadataList = defaultMetadataDiscoverer.discoverMetadata(
                    new File( defaultRepository.getBasedir() ), blacklistedPatterns );
                indexMetadata( metadataList, indexPath, new File( defaultRepository.getBasedir() ) );
            }
            else if ( dataMap.get( MAP_LAYOUT ).equals( "legacy" ) )
            {
                artifacts = legacyArtifactDiscoverer.discoverArtifacts( defaultRepository, blacklistedPatterns,
                                                                        includeSnapshots );
                indexArtifact( artifacts, indexPath, defaultRepository );
            }
        }
        catch ( RepositoryIndexException e )
        {
            e.printStackTrace();
        }
        catch ( MalformedURLException me )
        {
            me.printStackTrace();
        }

        getLogger().info( "[DiscovererJob] DiscovererJob has finished executing." );
    }

    /**
     * Index the artifacts in the list
     *
     * @param artifacts  the artifacts to be indexed
     * @param indexPath  the path to the index file
     * @param repository the repository where the artifacts are located
     */
    private void indexArtifact( List artifacts, String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        ArtifactRepositoryIndex artifactIndex = indexFactory.createArtifactRepositoryIndex( indexPath, repository );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            try
            {
                artifactIndex.indexArtifact( artifact );
            }
            catch ( Exception e )
            {
                if ( e instanceof RepositoryIndexException )
                {
                    throw (RepositoryIndexException) e;
                }
            }

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
    private void indexMetadata( List metadataList, String indexPath, File repositoryBase )
        throws RepositoryIndexException, MalformedURLException
    {
        String repoDir = repositoryBase.toURL().toString();
        ArtifactRepository repository =
            repoFactory.createArtifactRepository( "repository", repoDir, layout, null, null );

        MetadataRepositoryIndex metadataIndex = indexFactory.createMetadataRepositoryIndex( indexPath, repository );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) iter.next();
            try
            {
                metadataIndex.index( repoMetadata );
            }
            catch ( Exception e )
            {
                if ( e instanceof RepositoryIndexException )
                {
                    throw (RepositoryIndexException) e;
                }
            }
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
    private void indexPom( List models, String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        PomRepositoryIndex pomIndex = indexFactory.createPomRepositoryIndex( indexPath, repository );
        for ( Iterator iter = models.iterator(); iter.hasNext(); )
        {
            Model model = (Model) iter.next();
            try
            {
                pomIndex.indexPom( model );
            }
            catch ( Exception e )
            {
                if ( e instanceof RepositoryIndexException )
                {
                    throw (RepositoryIndexException) e;
                }
            }

            if ( pomIndex.isOpen() )
            {
                pomIndex.optimize();
                pomIndex.close();
            }
        }
    }

}
