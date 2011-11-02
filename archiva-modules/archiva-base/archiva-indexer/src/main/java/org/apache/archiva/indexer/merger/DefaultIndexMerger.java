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
import com.google.inject.Inject;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Service( "indexMerger#default" )
public class DefaultIndexMerger
    implements IndexMerger
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private MavenIndexerUtils mavenIndexerUtils;

    private NexusIndexer indexer;

    private IndexPacker indexPacker;

    @javax.inject.Inject
    public DefaultIndexMerger( PlexusSisuBridge plexusSisuBridge, MavenIndexerUtils mavenIndexerUtils )
        throws PlexusSisuBridgeException
    {
        this.indexer = plexusSisuBridge.lookup( NexusIndexer.class );
        this.mavenIndexerUtils = mavenIndexerUtils;
        indexPacker = plexusSisuBridge.lookup( IndexPacker.class, "default" );
    }

    public File buildMergedIndex( Collection<String> repositoriesIds, boolean packIndex )
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
            return indexingContext.getIndexDirectoryFile();
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
}
