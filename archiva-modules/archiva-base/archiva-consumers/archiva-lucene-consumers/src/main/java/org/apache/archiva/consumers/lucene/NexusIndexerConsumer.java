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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.DefaultIndexingContext;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.IndexerEngine;
import org.sonatype.nexus.index.packer.IndexPacker;
import org.sonatype.nexus.index.packer.IndexPackingRequest;

/**
 * Consumer for indexing the repository to provide search and IDE integration features.
 */
public class NexusIndexerConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private static final Logger log = LoggerFactory.getLogger( NexusIndexerConsumer.class );

    private ArtifactContextProducer artifactContextProducer;

    private IndexPacker indexPacker;

    private ManagedDefaultRepositoryContent repositoryContent;

    private IndexingContext context;

    private File managedRepository;
    
    private IndexerEngine indexerEngine;
    
    //private IndexingContextMap indexingContextMap;
    
    public NexusIndexerConsumer( IndexPacker indexPacker, IndexerEngine indexerEngine )
    {
        this.indexPacker = indexPacker;
        this.indexerEngine = indexerEngine;        
        this.artifactContextProducer = new DefaultArtifactContextProducer();
    }
    
   /* public NexusIndexerConsumer( IndexPacker indexPacker, IndexerEngine indexerEngine, IndexingContextMap indexingContextMap )
    {
        this.indexPacker = indexPacker;
        this.indexerEngine = indexerEngine;
        this.indexingContextMap = indexingContextMap;
        this.artifactContextProducer = new DefaultArtifactContextProducer();
    }*/
    
    public String getDescription()
    {
        return "Indexes the repository to provide search and IDE integration features";
    }

    public String getId()
    {
        return "index-content";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {   
        //synchronized( context )
        //{            
            log.debug( "Begin indexing of repository '" + repository.getId() + "'..");
            
            managedRepository = new File( repository.getLocation() );
            String indexDir = repository.getIndexDir();
            
            File indexDirectory = null;
            if( indexDir != null && !"".equals( indexDir ) )
            {
                indexDirectory = new File( repository.getIndexDir() );
            }
            else
            {
                indexDirectory = new File( managedRepository, ".indexer" );
            }

            repositoryContent = new ManagedDefaultRepositoryContent();
            repositoryContent.setRepository( repository );
            
            try
            {   
                context =
                    new DefaultIndexingContext( repository.getId(), repository.getId(), managedRepository,
                                                indexDirectory, null, null, NexusIndexer.FULL_INDEX, false );
                
                //context = indexingContextMap.addIndexingContext( repository.getId(), repository.getId(), managedRepository,
                //                                indexDirectory, null, null, NexusIndexer.FULL_INDEX, false );
                
                context.setSearchable( repository.isScanned() );
                
                //indexerEngine.beginIndexing( context );
            }
            catch ( UnsupportedExistingLuceneIndexException e )
            {
                throw new ConsumerException( "Could not create index at " + indexDirectory.getAbsoluteFile(), e );
            }
            catch ( IOException e )
            {
                throw new ConsumerException( "Could not create index at " + indexDirectory.getAbsoluteFile(), e );
            }
        //}
    }
    
    public void processFile( String path )
        throws ConsumerException
    {
        synchronized ( indexerEngine )
        {
            if ( context == null )
            {
                // didn't start correctly, so skip
                return;
            }
            
            File artifactFile = new File( managedRepository, path );        
            ArtifactContext artifactContext = artifactContextProducer.getArtifactContext( context, artifactFile );
            
            if ( artifactContext != null )
            {
                try
                {                                           
                    indexerEngine.index( context, artifactContext );                        
                }
                catch ( ZipException e )
                {
                    // invalid JAR file
                    log.info( e.getMessage() );
                }
                catch ( IOException e )
                {
                    throw new ConsumerException( e.getMessage(), e );
                }
            }
        }
    }

    public void completeScan()
    {   
        //synchronized( context )
        //{
            log.debug( "End indexing of repository '" + context.getRepositoryId() + "'..");
            
            final File indexLocation = new File( managedRepository, ".index" );
            try
            {
                //indexerEngine.endIndexing( context );
                
                IndexPackingRequest request = new IndexPackingRequest( context, indexLocation );
                indexPacker.packIndex( request );

                //indexingContextMap.removeIndexingContext( context.getId() );
                
                context.close( false );
            }
            catch ( IOException e )
            {
                log.error( "Could not pack index" + indexLocation.getAbsolutePath(), e );
            }
        //}
    }

    public List<String> getExcludes()
    {
        return new ArrayList<String>();
    }

    public List<String> getIncludes()
    {
        return Arrays.asList( "**/*" );
    }
}
