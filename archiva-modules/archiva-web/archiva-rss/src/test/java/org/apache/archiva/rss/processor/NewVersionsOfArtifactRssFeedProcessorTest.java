package org.apache.archiva.rss.processor;

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

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import junit.framework.TestCase;
import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

@RunWith(ArchivaBlockJUnit4ClassRunner.class)
public class NewVersionsOfArtifactRssFeedProcessorTest
    extends TestCase
{
    private NewVersionsOfArtifactRssFeedProcessor newVersionsProcessor;

    private static final String TEST_REPO = "test-repo";

    private static final String GROUP_ID = "org.apache.archiva";

    private static final String ARTIFACT_ID = "artifact-two";

    private IMocksControl metadataRepositoryControl;

    private MetadataRepository metadataRepository;

    private IMocksControl sessionFactoryControl;
    private RepositorySessionFactory sessionFactory;

    private IMocksControl sessionControl;
    private RepositorySession session;

    private IMocksControl repositoryRegistryControl;
    private RepositoryRegistry repositoryRegistry;


    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        newVersionsProcessor = new NewVersionsOfArtifactRssFeedProcessor();
        newVersionsProcessor.setGenerator( new RssFeedGenerator() );

        metadataRepositoryControl = createControl();
        metadataRepository = metadataRepositoryControl.createMock( MetadataRepository.class );

        sessionFactoryControl = EasyMock.createControl();
        sessionControl = EasyMock.createControl();
        sessionControl.resetToNice();

        sessionFactory = sessionFactoryControl.createMock( RepositorySessionFactory.class );
        session = sessionControl.createMock( RepositorySession.class );

        EasyMock.expect( sessionFactory.createSession() ).andStubReturn( session );
        EasyMock.expect( session.getRepository( ) ).andStubReturn( metadataRepository );
        sessionFactoryControl.replay();
        sessionControl.replay();

        repositoryRegistryControl = EasyMock.createControl();
        repositoryRegistry = repositoryRegistryControl.createMock( ArchivaRepositoryRegistry.class );

        List<Repository> reg = new ArrayList<>( );
        reg.add( new BasicManagedRepository( TEST_REPO, TEST_REPO, new FilesystemStorage( Paths.get("target/test-storage"), new DefaultFileLockManager() ) ) );
        EasyMock.expect( repositoryRegistry.getRepositories() ).andStubReturn( reg );
        repositoryRegistryControl.replay();

        newVersionsProcessor.setRepositorySessionFactory( sessionFactory );
        newVersionsProcessor.setRepositoryRegistry( repositoryRegistry );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcess()
        throws Exception
    {
        Date whenGatheredDate = new Date( 123456789 );
        ZonedDateTime whenGathered = ZonedDateTime.ofInstant(whenGatheredDate.toInstant(), ZoneId.systemDefault());

        ArtifactMetadata artifact1 = createArtifact( whenGathered, "1.0.1" );
        ArtifactMetadata artifact2 = createArtifact( whenGathered, "1.0.2" );

        Date whenGatheredNextDate = new Date( 345678912 );
        ZonedDateTime whenGatheredNext = ZonedDateTime.ofInstant(whenGatheredNextDate.toInstant(), ZoneId.systemDefault());

        ArtifactMetadata artifact3 = createArtifact( whenGatheredNext, "1.0.3-SNAPSHOT" );

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put( RssFeedProcessor.KEY_GROUP_ID, GROUP_ID );
        reqParams.put( RssFeedProcessor.KEY_ARTIFACT_ID, ARTIFACT_ID );

            expect(metadataRepository.getProjectVersions(session, TEST_REPO, GROUP_ID, ARTIFACT_ID)).andReturn(
                    Arrays.asList("1.0.1", "1.0.2", "1.0.3-SNAPSHOT"));
            expect(metadataRepository.getArtifacts(session, TEST_REPO, GROUP_ID, ARTIFACT_ID, "1.0.1")).andReturn(
                    Collections.singletonList(artifact1));
            expect(metadataRepository.getArtifacts(session, TEST_REPO, GROUP_ID, ARTIFACT_ID, "1.0.2")).andReturn(
                    Collections.singletonList(artifact2));
            expect(metadataRepository.getArtifacts(session, TEST_REPO, GROUP_ID, ARTIFACT_ID, "1.0.3-SNAPSHOT")).andReturn(
                    Collections.singletonList(artifact3));
        metadataRepositoryControl.replay();

        SyndFeed feed = newVersionsProcessor.process( reqParams );

        assertEquals( "New Versions of Artifact 'org.apache.archiva:artifact-two'", feed.getTitle() );
        assertEquals( "New versions of artifact 'org.apache.archiva:artifact-two' found during repository scan.",
                      feed.getDescription() );
        assertEquals( "en-us", feed.getLanguage() );
        assertEquals( whenGatheredNext.toInstant(), ZonedDateTime.ofInstant(feed.getPublishedDate().toInstant(), ZoneId.systemDefault()).toInstant() );

        List<SyndEntry> entries = feed.getEntries();

        assertEquals( 2, entries.size() );

        assertTrue( entries.get(0).getTitle().contains("New Versions of Artifact 'org.apache.archiva:artifact-two' as of "));
        assertEquals( whenGathered.toInstant(), entries.get( 0 ).getPublishedDate().toInstant() );

        assertTrue(entries.get(1).getTitle().contains("New Versions of Artifact 'org.apache.archiva:artifact-two' as of "));

        assertEquals( whenGatheredNext.toInstant(), entries.get( 1 ).getPublishedDate().toInstant() );

        metadataRepositoryControl.verify();
    }

    private ArtifactMetadata createArtifact(ZonedDateTime whenGathered, String version )
    {
        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setNamespace( GROUP_ID );
        artifact.setProject( ARTIFACT_ID );
        artifact.setProjectVersion( version );
        artifact.setVersion( version );
        artifact.setRepositoryId( TEST_REPO );
        artifact.setId( ARTIFACT_ID + "-" + version + ".jar" );
        artifact.setWhenGathered( whenGathered );
        return artifact;
    }
}
