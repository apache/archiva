package org.apache.maven.archiva.scheduled.tasks;

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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.DefaultIndexingContext;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;

/**
 * TaskCreator Convenience class for creating Archiva tasks.
 * @todo Nexus specifics shouldn't be in the archiva-scheduled module
 */
public class TaskCreator
{
    public static RepositoryTask createRepositoryTask( String repositoryId )
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        return task;
    }

    public static RepositoryTask createRepositoryTask( String repositoryId, boolean scanAll )
    {
        RepositoryTask task = createRepositoryTask( repositoryId );
        task.setScanAll( scanAll );

        return task;
    }

    public static RepositoryTask createRepositoryTask( String repositoryId, File resourceFile,
                                                       boolean updateRelatedArtifacts )
    {
        RepositoryTask task = createRepositoryTask( repositoryId );
        task.setResourceFile( resourceFile );
        task.setUpdateRelatedArtifacts( updateRelatedArtifacts );

        return task;
    }

    public static RepositoryTask createRepositoryTask( String repositoryId, File resourceFile,
                                                       boolean updateRelatedArtifacts, boolean scanAll )
    {
        RepositoryTask task = createRepositoryTask( repositoryId, resourceFile, updateRelatedArtifacts );
        task.setScanAll( scanAll );

        return task;
    }

    public static ArtifactIndexingTask createIndexingTask( ManagedRepositoryConfiguration repository, File resource,
                                                    ArtifactIndexingTask.Action action, IndexingContext context )
    {
        return new ArtifactIndexingTask( repository, resource, action, context );
    }

    public static IndexingContext createContext( ManagedRepositoryConfiguration repository )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        String indexDir = repository.getIndexDir();
        File managedRepository = new File( repository.getLocation() );

        File indexDirectory = null;
        if ( indexDir != null && !"".equals( indexDir ) )
        {
            indexDirectory = new File( repository.getIndexDir() );
        }
        else
        {
            indexDirectory = new File( managedRepository, ".indexer" );
        }

        IndexingContext context =
            new DefaultIndexingContext( repository.getId(), repository.getId(), managedRepository, indexDirectory,
                                        null, null, NexusIndexer.FULL_INDEX, false );
        context.setSearchable( repository.isScanned() );
        return context;
    }

}
