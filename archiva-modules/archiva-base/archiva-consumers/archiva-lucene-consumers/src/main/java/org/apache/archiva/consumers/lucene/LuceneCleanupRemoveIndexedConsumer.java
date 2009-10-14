package org.apache.archiva.consumers.lucene;

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
import java.util.List;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.ArtifactIndexingTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;

/**
 * LuceneCleanupRemoveIndexedConsumer Clean up the index of artifacts that are no longer existing in the file system
 * (managed repositories).
 * 
 * @version $Id$
 */
public class LuceneCleanupRemoveIndexedConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseCleanupConsumer
{
    private static final Logger log = LoggerFactory.getLogger( LuceneCleanupRemoveIndexedConsumer.class );

    private RepositoryContentFactory repoFactory;

    private ArchivaTaskScheduler scheduler;

    public LuceneCleanupRemoveIndexedConsumer( RepositoryContentFactory repoFactory, ArchivaTaskScheduler scheduler )
    {
        this.repoFactory = repoFactory;
        this.scheduler = scheduler;
    }

    public void beginScan()
    {
    }

    public void completeScan()
    {
    }

    public List<String> getIncludedTypes()
    {
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        ManagedRepositoryContent repoContent = null;

        try
        {
            repoContent = repoFactory.getManagedRepositoryContent( artifact.getModel().getRepositoryId() );
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Can't run index cleanup consumer: " + e.getMessage() );
        }

        ManagedRepositoryConfiguration repository = repoContent.getRepository();

        IndexingContext context = null;
        try
        {
            File artifactFile = new File( repoContent.getRepoRoot(), repoContent.toPath( artifact ) );

            if ( !artifactFile.exists() )
            {
                context = TaskCreator.createContext( repository );

                ArtifactIndexingTask task =
                    TaskCreator.createIndexingTask( repository, artifactFile, ArtifactIndexingTask.Action.DELETE,
                                                    context );

                log.debug( "Queueing indexing task '" + task + "' to remove the artifact from the index." );
                scheduler.queueIndexingTask( task );

                // note we finish immediately here since it isn't done repo-by-repo. It might be nice to ensure that is
                // the case for optimisation though
                task =
                    TaskCreator.createIndexingTask( repository, artifactFile, ArtifactIndexingTask.Action.FINISH,
                                                    context );
                log.debug( "Queueing indexing task + '" + task + "' to finish indexing." );
                scheduler.queueIndexingTask( task );
            }

        }
        catch ( TaskQueueException e )
        {
            throw new ConsumerException( e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            throw new ConsumerException( e.getMessage(), e );
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
                    log.error( e.getMessage() );
                }
            }
        }
    }

    public String getDescription()
    {
        return "Remove indexed content if not present on filesystem.";
    }

    public String getId()
    {
        return "not-present-remove-indexed";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void setRepositoryContentFactory( RepositoryContentFactory repoFactory )
    {
        this.repoFactory = repoFactory;
    }
}
