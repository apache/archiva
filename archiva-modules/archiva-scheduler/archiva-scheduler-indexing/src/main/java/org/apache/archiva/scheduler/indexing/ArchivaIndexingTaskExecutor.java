package org.apache.archiva.scheduler.indexing;

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
 * software distributed under the Li
 * cense is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.DefaultArtifactContextProducer;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.artifact.IllegalArtifactCoordinateException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ArchivaIndexingTaskExecutor Executes all indexing tasks. Adding, updating and removing artifacts from the index are
 * all performed by this executor. Add and update artifact in index tasks are added in the indexing task queue by the
 * NexusIndexerConsumer while remove artifact from index tasks are added by the LuceneCleanupRemoveIndexedConsumer.
 * <p/>
 * plexus.component role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" role-hint="indexing"
 * instantiation-strategy="singleton"
 */
@Service( "taskExecutor#indexing" )
public class ArchivaIndexingTaskExecutor
    implements TaskExecutor
{
    private Logger log = LoggerFactory.getLogger( ArchivaIndexingTaskExecutor.class );

    /**
     * plexus.requirement
     */
    private IndexPacker indexPacker;

    private ArtifactContextProducer artifactContextProducer;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private MavenIndexerUtils mavenIndexerUtils;

    private NexusIndexer nexusIndexer;

    private List<? extends IndexCreator> allIndexCreators;

    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        log.info( "Initialized {}", this.getClass().getName() );

        artifactContextProducer = new DefaultArtifactContextProducer();

        indexPacker = plexusSisuBridge.lookup( IndexPacker.class, "default" );

        nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );

        allIndexCreators = mavenIndexerUtils.getAllIndexCreators();
    }

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        synchronized ( nexusIndexer )
        {
            ArtifactIndexingTask indexingTask = (ArtifactIndexingTask) task;

            ManagedRepositoryConfiguration repository = indexingTask.getRepository();
            IndexingContext context = indexingTask.getContext();

            if ( ArtifactIndexingTask.Action.FINISH.equals( indexingTask.getAction() )
                && indexingTask.isExecuteOnEntireRepo() )
            {
                // TODO update or not !!
                // olamy currently do the full scan
                try
                {
                    nexusIndexer.scan( context, null, false );
                }
                catch ( IOException e )
                {
                    throw new TaskExecutionException( "Error scan repository " + repository, e );
                }
                log.debug( "Finishing indexing task on repo: {}", repository.getId() );
                finishIndexingTask( indexingTask, repository, context );
            }
            else
            {
                // create context if not a repo scan request
                if ( !indexingTask.isExecuteOnEntireRepo() )
                {
                    try
                    {
                        log.debug( "Creating indexing context on resource: {}",
                                   indexingTask.getResourceFile().getPath() );
                        context = ArtifactIndexingTask.createContext( repository, nexusIndexer, allIndexCreators );
                    }
                    catch ( IOException e )
                    {
                        log.error( "Error occurred while creating context: " + e.getMessage() );
                        throw new TaskExecutionException( "Error occurred while creating context: " + e.getMessage(),
                                                          e );
                    }
                    catch ( UnsupportedExistingLuceneIndexException e )
                    {
                        log.error( "Error occurred while creating context: " + e.getMessage() );
                        throw new TaskExecutionException( "Error occurred while creating context: " + e.getMessage(),
                                                          e );
                    }
                }

                if ( context == null || context.getIndexDirectory() == null )
                {
                    throw new TaskExecutionException( "Trying to index an artifact but the context is already closed" );
                }

                try
                {
                    File artifactFile = indexingTask.getResourceFile();
                    ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile );

                    if ( ac != null )
                    {
                        if ( indexingTask.getAction().equals( ArtifactIndexingTask.Action.ADD ) )
                        {
                            //IndexSearcher s = context.getIndexSearcher();
                            //String uinfo = ac.getArtifactInfo().getUinfo();
                            //TopDocs d = s.search( new TermQuery( new Term( ArtifactInfo.UINFO, uinfo ) ), 1 );

                            BooleanQuery q = new BooleanQuery();
                            q.add( nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression(
                                ac.getArtifactInfo().groupId ) ), BooleanClause.Occur.MUST );
                            q.add( nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression(
                                ac.getArtifactInfo().artifactId ) ), BooleanClause.Occur.MUST );
                            q.add( nexusIndexer.constructQuery( MAVEN.VERSION, new SourcedSearchExpression(
                                ac.getArtifactInfo().version ) ), BooleanClause.Occur.MUST );
                            if ( ac.getArtifactInfo().classifier != null )
                            {
                                q.add( nexusIndexer.constructQuery( MAVEN.CLASSIFIER, new SourcedSearchExpression(
                                    ac.getArtifactInfo().classifier ) ), BooleanClause.Occur.MUST );
                            }
                            if ( ac.getArtifactInfo().packaging != null )
                            {
                                q.add( nexusIndexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression(
                                    ac.getArtifactInfo().packaging ) ), BooleanClause.Occur.MUST );
                            }
                            FlatSearchRequest flatSearchRequest = new FlatSearchRequest( q, context );
                            FlatSearchResponse flatSearchResponse = nexusIndexer.searchFlat( flatSearchRequest );
                            if ( flatSearchResponse.getResults().isEmpty() )
                            {
                                log.debug( "Adding artifact '{}' to index..", ac.getArtifactInfo() );
                                nexusIndexer.addArtifactToIndex( ac, context );
                            }
                            else
                            {
                                log.debug( "Updating artifact '{}' in index..", ac.getArtifactInfo() );
                                // TODO check if update exists !!
                                nexusIndexer.deleteArtifactFromIndex( ac, context );
                                nexusIndexer.addArtifactToIndex( ac, context );
                            }

                            //nexusIndexer.scan( context, true );

                            context.updateTimestamp();

                            // close the context if not a repo scan request
                            if ( !indexingTask.isExecuteOnEntireRepo() )
                            {
                                log.debug( "Finishing indexing task on resource file : {}",
                                           indexingTask.getResourceFile().getPath() );
                                finishIndexingTask( indexingTask, repository, context );
                            }
                        }
                        else
                        {
                            log.debug( "Removing artifact '{}' from index..", ac.getArtifactInfo() );
                            nexusIndexer.deleteArtifactFromIndex( ac, context );
                        }
                    }
                }
                catch ( IOException e )
                {
                    log.error( "Error occurred while executing indexing task '" + indexingTask + "': " + e.getMessage(),
                               e );
                    throw new TaskExecutionException(
                        "Error occurred while executing indexing task '" + indexingTask + "'", e );
                }
                catch ( IllegalArtifactCoordinateException e )
                {
                    log.error( "Error occurred while getting artifact context: " + e.getMessage() );
                    throw new TaskExecutionException( "Error occurred while getting artifact context.", e );
                }
            }
        }
    }

    private void finishIndexingTask( ArtifactIndexingTask indexingTask, ManagedRepositoryConfiguration repository,
                                     IndexingContext context )
        throws TaskExecutionException
    {
        try
        {

            context.optimize();


            File managedRepository = new File( repository.getLocation() );
            final File indexLocation = new File( managedRepository, ".index" );
            IndexPackingRequest request = new IndexPackingRequest( context, indexLocation );
            indexPacker.packIndex( request );

            log.debug( "Index file packaged at '{}'.", indexLocation.getPath() );

        }
        catch ( IOException e )
        {
            log.error( "Error occurred while executing indexing task '" + indexingTask + "': " + e.getMessage() );
            throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask + "'",
                                              e );
        }
        finally
        {
            /*
            olamy don't close it anymore as it nullify IndexSearcher
            if ( context != null )
            {
                try
                {
                    context.close( false );
                }
                catch ( IOException e )
                {
                    log.error( "Error occurred while closing context: " + e.getMessage() );
                    throw new TaskExecutionException( "Error occurred while closing context: " + e.getMessage() );
                }
            }
            */
        }
    }

    public void setIndexPacker( IndexPacker indexPacker )
    {
        this.indexPacker = indexPacker;
    }

    public PlexusSisuBridge getPlexusSisuBridge()
    {
        return plexusSisuBridge;
    }

    public void setPlexusSisuBridge( PlexusSisuBridge plexusSisuBridge )
    {
        this.plexusSisuBridge = plexusSisuBridge;
    }
}
