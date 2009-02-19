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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.creator.AbstractIndexCreator;
import org.sonatype.nexus.index.creator.IndexerEngine;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * LuceneCleanupRemoveIndexedConsumer
 * 
 * @version $Id$
 */
public class LuceneCleanupRemoveIndexedConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseCleanupConsumer
{
    private static final Logger log = LoggerFactory.getLogger( LuceneCleanupRemoveIndexedConsumer.class );

    private RepositoryContentFactory repoFactory;

    private NexusIndexer indexer;

    private ArtifactContextProducer artifactContextProducer;

    private IndexerEngine indexerEngine;

    private IndexingContext context;

    public LuceneCleanupRemoveIndexedConsumer( RepositoryContentFactory repoFactory, NexusIndexer indexer,
                                               IndexerEngine indexerEngine )
    {
        this.repoFactory = repoFactory;
        this.indexer = indexer;
        this.indexerEngine = indexerEngine;
        this.artifactContextProducer = new DefaultArtifactContextProducer();
    }

    public void beginScan()
    {
        // TODO Auto-generated method stub

    }

    public void completeScan()
    {
        try
        {
            context.getIndexWriter().close();

            //indexerEngine.endIndexing( context );
            indexer.removeIndexingContext( context, false );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
        }
    }

    public List<String> getIncludedTypes()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        try
        {
            ManagedRepositoryContent repoContent =
                repoFactory.getManagedRepositoryContent( artifact.getModel().getRepositoryId() );

            ManagedRepositoryConfiguration repository = repoContent.getRepository();
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

            synchronized ( indexer )
            {
                try
                {
                    context =
                        indexer.addIndexingContext( repository.getId(), repository.getId(), managedRepository,
                                                    indexDirectory, null, null, NexusIndexer.FULL_INDEX );
                    context.setSearchable( repository.isScanned() );

                    File artifactFile = new File( repoContent.getRepoRoot(), repoContent.toPath( artifact ) );
                    System.out.println( "artifactFile :: " + artifactFile.getAbsolutePath() );

                    if ( !artifactFile.exists() )
                    {
                        ArtifactContext artifactContext =
                            artifactContextProducer.getArtifactContext( context, artifactFile );

                        if ( artifactContext != null )
                        {
                            //indexerEngine.remove( context, artifactContext );

                            // hack for deleting documents - indexer engine's isn't working for me
                            removeDocuments( artifactContext );
                        }
                    }
                }
                catch ( UnsupportedExistingLuceneIndexException e )
                {
                    log.error( "Unsupported index format.", e );
                }
                catch ( IOException e )
                {
                    log.error( "Unable to open index at " + indexDirectory.getAbsoluteFile(), e );
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Can't run index cleanup consumer: " + e.getMessage() );
        }
    }
    
    private void removeDocuments( ArtifactContext ac )
        throws IOException
    {
        IndexWriter w = context.getIndexWriter();

        ArtifactInfo ai = ac.getArtifactInfo();
        String uinfo = AbstractIndexCreator.getGAV( ai.groupId, ai.artifactId, ai.version, ai.classifier, ai.packaging );

        Document doc = new Document();
        doc.add( new Field( ArtifactInfo.DELETED, uinfo, Field.Store.YES, Field.Index.NO ) );
        doc.add( new Field( ArtifactInfo.LAST_MODIFIED, Long.toString( System.currentTimeMillis() ), Field.Store.YES,
                            Field.Index.NO ) );

        w.addDocument( doc );

        w.deleteDocuments( new Term( ArtifactInfo.UINFO, uinfo ) );

        w.commit();

        context.updateTimestamp();
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
