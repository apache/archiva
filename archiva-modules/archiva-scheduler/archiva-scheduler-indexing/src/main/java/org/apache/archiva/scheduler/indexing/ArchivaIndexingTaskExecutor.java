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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.redback.components.taskqueue.Task;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutionException;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutor;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index_shaded.lucene.search.BooleanClause;
import org.apache.maven.index_shaded.lucene.search.BooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

/**
 * ArchivaIndexingTaskExecutor Executes all indexing tasks. Adding, updating and removing artifacts from the index are
 * all performed by this executor. Add and update artifact in index tasks are added in the indexing task queue by the
 * NexusIndexerConsumer while remove artifact from index tasks are added by the LuceneCleanupRemoveIndexedConsumer.
 */
@Service( "taskExecutor#indexing" )
public class ArchivaIndexingTaskExecutor
    implements TaskExecutor
{
    private Logger log = LoggerFactory.getLogger( ArchivaIndexingTaskExecutor.class );

    @Inject
    private IndexPacker indexPacker;

    @Inject
    private ArtifactContextProducer artifactContextProducer;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private NexusIndexer nexusIndexer;


    /**
     * depending on current {@link Task} you have.
     * If {@link org.apache.archiva.scheduler.indexing.ArtifactIndexingTask.Action#FINISH} &amp;&amp; isExecuteOnEntireRepo:
     * repository will be scanned.
     *
     * @param task
     * @throws TaskExecutionException
     */
    @Override
    public void executeTask( Task task )
        throws TaskExecutionException
    {
        ArtifactIndexingTask indexingTask = (ArtifactIndexingTask) task;

        ManagedRepository repository = indexingTask.getRepository();
        IndexingContext context = indexingTask.getContext();

        if ( ArtifactIndexingTask.Action.FINISH.equals( indexingTask.getAction() )
            && indexingTask.isExecuteOnEntireRepo() )
        {
            try
            {
                long start = System.currentTimeMillis();
                nexusIndexer.scan( context, null, indexingTask.isOnlyUpdate() );
                long end = System.currentTimeMillis();
                log.info( "indexed maven repository: {}, onlyUpdate: {}, time {} ms", repository.getId(),
                          indexingTask.isOnlyUpdate(), ( end - start ) );
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
                    log.debug( "Creating indexing context on resource: {}", //
                               ( indexingTask.getResourceFile() == null
                                   ? "none"
                                   : indexingTask.getResourceFile() ) );
                    context = managedRepositoryAdmin.createIndexContext( repository );
                }
                catch ( RepositoryAdminException e )
                {
                    log.error( "Error occurred while creating context: {}", e.getMessage() );
                    throw new TaskExecutionException( "Error occurred while creating context: " + e.getMessage(), e );
                }
            }

            if ( context == null || context.getIndexDirectory() == null )
            {
                throw new TaskExecutionException( "Trying to index an artifact but the context is already closed" );
            }

            try
            {
                Path artifactFile = indexingTask.getResourceFile();
                if ( artifactFile == null )
                {
                    log.debug( "no artifact pass in indexing task so skip it" );
                }
                else
                {
                    ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile.toFile() );

                    if ( ac != null )
                    {
                        // MRM-1779 pom must be indexed too
                        // TODO make that configurable?
                        if ( artifactFile.getFileName().toString().endsWith( ".pom" ) )
                        {
                            ac.getArtifactInfo().setFileExtension( "pom" );
                            ac.getArtifactInfo().setPackaging( "pom" );
                            ac.getArtifactInfo().setClassifier( "pom" );
                        }
                        if ( indexingTask.getAction().equals( ArtifactIndexingTask.Action.ADD ) )
                        {
                            //IndexSearcher s = context.getIndexSearcher();
                            //String uinfo = ac.getArtifactInfo().getUinfo();
                            //TopDocs d = s.search( new TermQuery( new Term( ArtifactInfo.UINFO, uinfo ) ), 1 );

                            BooleanQuery q = new BooleanQuery();
                            q.add( nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression(
                                ac.getArtifactInfo().getGroupId() ) ), BooleanClause.Occur.MUST );
                            q.add( nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression(
                                ac.getArtifactInfo().getArtifactId() ) ), BooleanClause.Occur.MUST );
                            q.add( nexusIndexer.constructQuery( MAVEN.VERSION, new SourcedSearchExpression(
                                ac.getArtifactInfo().getVersion() ) ), BooleanClause.Occur.MUST );
                            if ( ac.getArtifactInfo().getClassifier() != null )
                            {
                                q.add( nexusIndexer.constructQuery( MAVEN.CLASSIFIER, new SourcedSearchExpression(
                                    ac.getArtifactInfo().getClassifier() ) ), BooleanClause.Occur.MUST );
                            }
                            if ( ac.getArtifactInfo().getPackaging() != null )
                            {
                                q.add( nexusIndexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression(
                                    ac.getArtifactInfo().getPackaging() ) ), BooleanClause.Occur.MUST );
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

                            context.updateTimestamp();
                            context.commit();


                        }
                        else
                        {
                            log.debug( "Removing artifact '{}' from index..", ac.getArtifactInfo() );
                            nexusIndexer.deleteArtifactFromIndex( ac, context );
                        }
                    }
                }
                // close the context if not a repo scan request
                if ( !indexingTask.isExecuteOnEntireRepo() )
                {
                    log.debug( "Finishing indexing task on resource file : {}", indexingTask.getResourceFile() != null
                        ? indexingTask.getResourceFile()
                        : " none " );
                    finishIndexingTask( indexingTask, repository, context );
                }
            }
            catch ( IOException e )
            {
                log.error( "Error occurred while executing indexing task '{}': {}", indexingTask, e.getMessage(),
                           e );
                throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask + "'",
                                                  e );
            }
        }

    }

    private void finishIndexingTask( ArtifactIndexingTask indexingTask, ManagedRepository repository,
                                     IndexingContext context )
        throws TaskExecutionException
    {
        try
        {

            context.optimize();

            if ( !repository.isSkipPackedIndexCreation() )
            {

                IndexPackingRequest request = new IndexPackingRequest( context, //
                                                                       context.acquireIndexSearcher().getIndexReader(),
                                                                       //
                                                                       context.getIndexDirectoryFile() );

                indexPacker.packIndex( request );
                context.updateTimestamp( true );

                log.debug( "Index file packaged at '{}'.", context.getIndexDirectoryFile() );
            }
            else
            {
                log.debug( "skip packed index creation" );
            }
        }
        catch ( IOException e )
        {
            log.error( "Error occurred while executing indexing task '{}': {}", indexingTask, e.getMessage() );
            throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask + "'",
                                              e );
        }
    }

    public void setIndexPacker( IndexPacker indexPacker )
    {
        this.indexPacker = indexPacker;
    }

}
