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
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.artifact.IllegalArtifactCoordinateException;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.IndexerEngine;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.packer.IndexPacker;
import org.sonatype.nexus.index.packer.IndexPackingRequest;

/**
 * ArchivaIndexingTaskExecutor Executes all indexing tasks. Adding, updating and removing artifacts from the index are
 * all performed by this executor. Add and update artifact in index tasks are added in the indexing task queue by the
 * NexusIndexerConsumer while remove artifact from index tasks are added by the LuceneCleanupRemoveIndexedConsumer.
 * 
 * @plexus.component role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" role-hint="indexing"
 *                   instantiation-strategy="singleton"
 */
public class ArchivaIndexingTaskExecutor
    implements TaskExecutor, Initializable
{
    private Logger log = LoggerFactory.getLogger( ArchivaIndexingTaskExecutor.class );

    /**
     * @plexus.requirement
     */
    private IndexerEngine indexerEngine;

    /**
     * @plexus.requirement
     */
    private IndexPacker indexPacker;

    private ArtifactContextProducer artifactContextProducer;

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        synchronized ( indexerEngine )
        {
            ArtifactIndexingTask indexingTask = (ArtifactIndexingTask) task;

            ManagedRepositoryConfiguration repository = indexingTask.getRepository();
            IndexingContext context = indexingTask.getContext();

            if ( ArtifactIndexingTask.Action.FINISH.equals( indexingTask.getAction() )
                && indexingTask.isExecuteOnEntireRepo() )
            {
                log.debug( "Finishing indexing task on repo: " + repository.getId() );
                finishIndexingTask( indexingTask, repository, context );
            }
            else
            {
                // create context if not a repo scan request
                if( !indexingTask.isExecuteOnEntireRepo() )
                {
                    try
                    {
                        log.debug( "Creating indexing context on resource: " + indexingTask.getResourceFile().getPath() );
                        context = ArtifactIndexingTask.createContext( repository );
                    }
                    catch( IOException e )
                    {
                        log.error( "Error occurred while creating context: " + e.getMessage() );
                        throw new TaskExecutionException( "Error occurred while creating context: " + e.getMessage() );
                    }
                    catch( UnsupportedExistingLuceneIndexException e )
                    {
                        log.error( "Error occurred while creating context: " + e.getMessage() );
                        throw new TaskExecutionException( "Error occurred while creating context: " + e.getMessage() );    
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
                            IndexSearcher s = context.getIndexSearcher();
                            String uinfo = ac.getArtifactInfo().getUinfo();
                            TopDocs d = s.search( new TermQuery( new Term( ArtifactInfo.UINFO, uinfo ) ), 1 );
                            if ( d.totalHits == 0 )
                            {
                                log.debug( "Adding artifact '" + ac.getArtifactInfo() + "' to index.." );
                                indexerEngine.index( context, ac );
                                context.getIndexWriter().commit();
                            }
                            else
                            {
                                log.debug( "Updating artifact '" + ac.getArtifactInfo() + "' in index.." );
                                indexerEngine.update( context, ac );
                                context.getIndexWriter().commit();
                            }

                            // close the context if not a repo scan request
                            if( !indexingTask.isExecuteOnEntireRepo() )
                            {
                                log.debug( "Finishing indexing task on resource file : " + indexingTask.getResourceFile().getPath() );
                                finishIndexingTask( indexingTask, repository, context );   
                            }
                        }
                        else
                        {
                            log.debug( "Removing artifact '" + ac.getArtifactInfo() + "' from index.." );
                            indexerEngine.remove( context, ac );
                            context.getIndexWriter().commit();
                        }
                    }
                }
                catch ( IOException e )
                {
                    log.error( "Error occurred while executing indexing task '" + indexingTask + "': " + e.getMessage() );
                    throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask
                        + "'", e );
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

            log.debug( "Index file packaged at '" + indexLocation.getPath() + "'." );
        }
        catch ( IOException e )
        {
            log.error( "Error occurred while executing indexing task '" + indexingTask + "': " + e.getMessage() );
            throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask
                + "'", e );
        }
        finally
        {
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
        }
    }

    public void initialize()
        throws InitializationException
    {
        log.info( "Initialized " + this.getClass().getName() );

        artifactContextProducer = new DefaultArtifactContextProducer();
    }

    public void setIndexerEngine( IndexerEngine indexerEngine )
    {
        this.indexerEngine = indexerEngine;
    }

    public void setIndexPacker( IndexPacker indexPacker )
    {
        this.indexPacker = indexPacker;
    }
}
