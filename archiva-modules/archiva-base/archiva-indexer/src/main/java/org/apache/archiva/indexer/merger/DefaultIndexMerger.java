package org.apache.archiva.indexer.merger;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.io.Files;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Service( "indexMerger#default" )
public class DefaultIndexMerger
    implements IndexMerger
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private MavenIndexerUtils mavenIndexerUtils;

    private NexusIndexer indexer;

    private IndexPacker indexPacker;

    private List<TemporaryGroupIndex> temporaryGroupIndexes = new CopyOnWriteArrayList<TemporaryGroupIndex>();

    @Inject
    public DefaultIndexMerger( PlexusSisuBridge plexusSisuBridge, MavenIndexerUtils mavenIndexerUtils )
        throws PlexusSisuBridgeException
    {
        this.indexer = plexusSisuBridge.lookup( NexusIndexer.class );
        this.mavenIndexerUtils = mavenIndexerUtils;
        indexPacker = plexusSisuBridge.lookup( IndexPacker.class, "default" );
    }

    public IndexingContext buildMergedIndex( Collection<String> repositoriesIds, boolean packIndex )
        throws IndexMergerException
    {
        File tempRepoFile = Files.createTempDir();
        tempRepoFile.deleteOnExit();

        String tempRepoId = tempRepoFile.getName();

        try
        {
            File indexLocation = new File( tempRepoFile, ".indexer" );
            IndexingContext indexingContext =
                indexer.addIndexingContext( tempRepoId, tempRepoId, tempRepoFile, indexLocation, null, null,
                                            mavenIndexerUtils.getAllIndexCreators() );

            for ( String repoId : repositoriesIds )
            {
                IndexingContext idxToMerge = indexer.getIndexingContexts().get( repoId );
                if ( idxToMerge != null )
                {
                    indexingContext.merge( idxToMerge.getIndexDirectory() );
                }
            }

            indexingContext.optimize();

            if ( packIndex )
            {
                IndexPackingRequest request = new IndexPackingRequest( indexingContext, indexLocation );
                indexPacker.packIndex( request );
            }
            temporaryGroupIndexes.add( new TemporaryGroupIndex( tempRepoFile, tempRepoId ) );
            return indexingContext;
        }
        catch ( IOException e )
        {
            throw new IndexMergerException( e.getMessage(), e );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            throw new IndexMergerException( e.getMessage(), e );
        }
    }

    @Async
    public void cleanTemporaryGroupIndex( TemporaryGroupIndex temporaryGroupIndex )
    {
        if ( temporaryGroupIndex == null )
        {
            return;
        }

        try
        {
            IndexingContext indexingContext = indexer.getIndexingContexts().get( temporaryGroupIndex.getIndexId() );
            if ( indexingContext != null )
            {
                indexer.removeIndexingContext( indexingContext, true );
            }
        }
        catch ( IOException e )
        {
            log.warn( "fail to delete temporary group index {}", temporaryGroupIndex.getIndexId(), e );
        }
    }

    public Collection<TemporaryGroupIndex> getTemporaryGroupIndexes()
    {
        return this.temporaryGroupIndexes;
    }
}
