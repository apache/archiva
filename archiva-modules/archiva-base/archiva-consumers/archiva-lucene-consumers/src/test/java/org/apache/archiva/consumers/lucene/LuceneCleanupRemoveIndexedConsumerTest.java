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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.DefaultIndexingContext;
import org.sonatype.nexus.index.context.IndexingContext;

public class LuceneCleanupRemoveIndexedConsumerTest
    extends PlexusInSpringTestCase
{
    private LuceneCleanupRemoveIndexedConsumer consumer;

    private MockControl indexerControl;

    private NexusIndexer indexer;

    private RepositoryContentFactory repoFactory;

    private MockControl repoFactoryControl;

    private ManagedRepositoryConfiguration repositoryConfig;

    private MockControl contextProducerControl;

    private ArtifactContextProducer artifactContextProducer;

    private MockControl acControl;

    private ArtifactContext ac;

    public void setUp()
        throws Exception
    {
        super.setUp();

        indexerControl = MockControl.createControl( NexusIndexer.class );
        indexerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        indexer = (NexusIndexer) indexerControl.getMock();

        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = (RepositoryContentFactory) repoFactoryControl.getMock();

        consumer = new LuceneCleanupRemoveIndexedConsumer( repoFactory, indexer );

        repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
        repositoryConfig.setIndexDir( getBasedir() + "/target/test-classes/test-repo/.cleanup-index" );

        contextProducerControl = MockControl.createControl( ArtifactContextProducer.class );
        artifactContextProducer = (ArtifactContextProducer) contextProducerControl.getMock();

        consumer.setArtifactContextProducer( artifactContextProducer );

        acControl = MockClassControl.createControl( ArtifactContext.class );
        ac = (ArtifactContext) acControl.getMock();
    }

    public void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( new File( repositoryConfig.getIndexDir() ) );

        super.tearDown();
    }

    public void testProcessArtifactArtifactDoesNotExist()
        throws Exception
    {
        ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.archiva", "archiva-lucene-consumers", "1.2", null, "jar", "test-repo" );
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( repositoryConfig );

        IndexingContext context =
            new DefaultIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getIndexDir() ), null, null,
                                        NexusIndexer.FULL_INDEX, false );

        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-lucene-consumers/1.2/archiva-lucene-consumers-1.2.jar" );
        ArtifactInfo ai = new ArtifactInfo( "test-repo", "org.apache.archiva", "archiva-lucene-consumers", "1.2", null );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( repositoryConfig.getId() ),
                                            repoContent );
        indexerControl.expectAndReturn( indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                                                    new File( repositoryConfig.getLocation() ),
                                                                    new File( repositoryConfig.getIndexDir() ), null,
                                                                    null, NexusIndexer.FULL_INDEX ), context );
        contextProducerControl.expectAndReturn( artifactContextProducer.getArtifactContext( context, artifactFile ), ac );
        acControl.expectAndReturn( ac.getArtifactInfo(), ai );

        repoFactoryControl.replay();
        indexerControl.replay();
        contextProducerControl.replay();
        acControl.replay();

        consumer.processArchivaArtifact( artifact );

        repoFactoryControl.verify();
        indexerControl.verify();
        contextProducerControl.verify();
        acControl.verify();
    }

    public void testProcessArtifactArtifactExists()
        throws Exception
    {
        ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.maven.archiva", "archiva-lucene-cleanup", "1.0", null, "jar", "test-repo" );
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( repositoryConfig );

        IndexingContext context =
            new DefaultIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getIndexDir() ), null, null,
                                        NexusIndexer.FULL_INDEX, false );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( repositoryConfig.getId() ),
                                            repoContent );
        indexerControl.expectAndReturn( indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                                                    new File( repositoryConfig.getLocation() ),
                                                                    new File( repositoryConfig.getIndexDir() ), null,
                                                                    null, NexusIndexer.FULL_INDEX ), context );

        repoFactoryControl.replay();
        indexerControl.replay();

        consumer.processArchivaArtifact( artifact );

        repoFactoryControl.verify();
        indexerControl.verify();
    }
}
