package org.apache.archiva.web.xmlrpc.services;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.filter.IncludesFilter;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.stagerepository.merge.RepositoryMerger;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.content.ManagedLegacyRepositoryContent;
import org.apache.maven.archiva.repository.content.PathParser;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * AdministrationServiceImplTest
 * 
 * @version $Id: AdministrationServiceImplTest.java
 */
public class AdministrationServiceImplTest
    extends PlexusInSpringTestCase
{
    private MockControl archivaConfigControl;

    private ArchivaConfiguration archivaConfig;

    private MockControl configControl;

    private Configuration config;

    private AdministrationServiceImpl service;

    private MockControl repositoryTaskSchedulerControl;

    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    // repository consumers
    private MockControl repoConsumerUtilsControl;

    private RepositoryContentConsumers repoConsumersUtil;

    private MockControl knownContentConsumerControl;

    private MockControl invalidContentConsumerControl;

    private KnownRepositoryContentConsumer indexArtifactConsumer;

    private KnownRepositoryContentConsumer indexPomConsumer;

    private InvalidRepositoryContentConsumer checkPomConsumer;

    private InvalidRepositoryContentConsumer checkMetadataConsumer;

    // delete artifact
    private MockControl repoFactoryControl;

    private RepositoryContentFactory repositoryFactory;

    private MockControl listenerControl;

    private RepositoryListener listener;

    private MockControl metadataRepositoryControl;

    private MetadataRepository metadataRepository;

    private MockControl repositoryStatisticsManagerControl;

    private RepositoryStatisticsManager repositoryStatisticsManager;

    private MockControl repositoryMergerControl;

    private RepositoryMerger repositoryMerger;

    private MockControl auditListenerControl;

    private AuditListener auditListener;

    private static final String STAGE = "-stage";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        configControl = MockClassControl.createControl( Configuration.class );
        config = (Configuration) configControl.getMock();

        repositoryTaskSchedulerControl = MockClassControl.createControl( RepositoryArchivaTaskScheduler.class );
        repositoryTaskScheduler = (RepositoryArchivaTaskScheduler) repositoryTaskSchedulerControl.getMock();

        // repo consumers
        repoConsumerUtilsControl = MockClassControl.createControl( RepositoryContentConsumers.class );
        repoConsumersUtil = (RepositoryContentConsumers) repoConsumerUtilsControl.getMock();

        knownContentConsumerControl = MockControl.createControl( KnownRepositoryContentConsumer.class );
        indexArtifactConsumer = (KnownRepositoryContentConsumer) knownContentConsumerControl.getMock();
        indexPomConsumer = (KnownRepositoryContentConsumer) knownContentConsumerControl.getMock();

        invalidContentConsumerControl = MockControl.createControl( InvalidRepositoryContentConsumer.class );
        checkPomConsumer = (InvalidRepositoryContentConsumer) invalidContentConsumerControl.getMock();
        checkMetadataConsumer = (InvalidRepositoryContentConsumer) invalidContentConsumerControl.getMock();

        // delete artifact
        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repositoryFactory = (RepositoryContentFactory) repoFactoryControl.getMock();

        metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();

        listenerControl = MockControl.createControl( RepositoryListener.class );
        listener = (RepositoryListener) listenerControl.getMock();

        repositoryStatisticsManagerControl = MockControl.createControl( RepositoryStatisticsManager.class );
        repositoryStatisticsManager = (RepositoryStatisticsManager) repositoryStatisticsManagerControl.getMock();

        repositoryMergerControl = MockControl.createControl( RepositoryMerger.class );
        repositoryMerger = (RepositoryMerger) repositoryMergerControl.getMock();

        auditListenerControl = MockControl.createControl( AuditListener.class );
        auditListener = (AuditListener) auditListenerControl.getMock();

        service =
            new AdministrationServiceImpl( archivaConfig, repoConsumersUtil, repositoryFactory, metadataRepository,
                                           repositoryTaskScheduler, Collections.singletonList( listener ),
                                           repositoryStatisticsManager, repositoryMerger, auditListener );
    }

    /* Tests for repository consumers */

    public void testGetAllRepoConsumers()
        throws Exception
    {
        recordRepoConsumers();

        repoConsumerUtilsControl.replay();
        knownContentConsumerControl.replay();
        invalidContentConsumerControl.replay();

        List<String> repoConsumers = service.getAllRepositoryConsumers();

        repoConsumerUtilsControl.verify();
        knownContentConsumerControl.verify();
        invalidContentConsumerControl.verify();

        assertNotNull( repoConsumers );
        assertEquals( 4, repoConsumers.size() );
        assertTrue( repoConsumers.contains( "index-artifact" ) );
        assertTrue( repoConsumers.contains( "index-pom" ) );
        assertTrue( repoConsumers.contains( "check-pom" ) );
        assertTrue( repoConsumers.contains( "check-metadata" ) );
    }

    public void testConfigureValidRepositoryConsumer()
        throws Exception
    {
        RepositoryScanningConfiguration repoScanning = new RepositoryScanningConfiguration();
        repoScanning.addKnownContentConsumer( "index-artifact" );
        repoScanning.addKnownContentConsumer( "index-pom" );
        repoScanning.addInvalidContentConsumer( "check-pom" );

        // test enable "check-metadata" consumer
        recordRepoConsumers();

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRepositoryScanning(), repoScanning );

        config.setRepositoryScanning( repoScanning );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();

        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();

        repoConsumerUtilsControl.replay();
        knownContentConsumerControl.replay();
        invalidContentConsumerControl.replay();
        archivaConfigControl.replay();
        configControl.replay();

        try
        {
            boolean success = service.configureRepositoryConsumer( null, "check-metadata", true );
            assertTrue( success );
        }
        catch ( Exception e )
        {
            fail( "An exception should not have been thrown." );
        }

        repoConsumerUtilsControl.verify();
        knownContentConsumerControl.verify();
        invalidContentConsumerControl.verify();
        archivaConfigControl.verify();
        configControl.verify();

        // test disable "check-metadata" consumer
        repoConsumerUtilsControl.reset();
        knownContentConsumerControl.reset();
        invalidContentConsumerControl.reset();
        archivaConfigControl.reset();
        configControl.reset();

        repoScanning.addInvalidContentConsumer( "check-metadata" );

        recordRepoConsumers();

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRepositoryScanning(), repoScanning );

        config.setRepositoryScanning( repoScanning );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();

        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();

        repoConsumerUtilsControl.replay();
        knownContentConsumerControl.replay();
        invalidContentConsumerControl.replay();
        archivaConfigControl.replay();
        configControl.replay();

        try
        {
            boolean success = service.configureRepositoryConsumer( null, "check-metadata", false );

            repoConsumerUtilsControl.verify();
            knownContentConsumerControl.verify();
            invalidContentConsumerControl.verify();
            archivaConfigControl.verify();
            configControl.verify();

            assertTrue( success );
        }
        catch ( Exception e )
        {
            fail( "An excecption should not have been thrown." );
        }
    }

    public void testConfigureInvalidRepositoryConsumer()
        throws Exception
    {
        recordRepoConsumers();

        repoConsumerUtilsControl.replay();
        knownContentConsumerControl.replay();
        invalidContentConsumerControl.replay();

        try
        {
            service.configureRepositoryConsumer( null, "invalid-consumer", true );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Invalid repository consumer.", e.getMessage() );
        }

        repoConsumerUtilsControl.verify();
        knownContentConsumerControl.verify();
        invalidContentConsumerControl.verify();
    }

    /* Tests for delete artifact */

    public void testDeleteM2ArtifactArtifactExists()
        throws Exception
    {
        ManagedRepositoryConfiguration managedRepo = createManagedRepo( "default", "default-repo" );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ), managedRepo );

        ManagedDefaultRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( managedRepo );

        repoFactoryControl.expectAndReturn( repositoryFactory.getManagedRepositoryContent( "internal" ), repoContent );

        List<ArtifactMetadata> artifacts = getArtifacts();
        ArtifactMetadata artifact = artifacts.get( 0 );

        metadataRepositoryControl.expectAndReturn( metadataRepository.getArtifacts( repoContent.getId(),
                                                                                    artifact.getNamespace(),
                                                                                    artifact.getProject(),
                                                                                    artifact.getVersion() ), artifacts );
        metadataRepository.deleteArtifact( repoContent.getId(), artifact.getNamespace(), artifact.getProject(),
                                           artifact.getVersion(), artifact.getId() );

        listener.deleteArtifact( repoContent.getId(), artifact.getNamespace(), artifact.getProject(),
                                 artifact.getVersion(), artifact.getId() );
        listenerControl.setVoidCallable( 1 );

        archivaConfigControl.replay();
        configControl.replay();
        repoFactoryControl.replay();
        metadataRepositoryControl.replay();
        listenerControl.replay();

        boolean success = service.deleteArtifact( "internal", "org.apache.archiva", "archiva-test", "1.0" );
        assertTrue( success );

        archivaConfigControl.verify();
        configControl.verify();
        repoFactoryControl.verify();
        metadataRepositoryControl.verify();
        listenerControl.verify();

        assertFalse( new File( managedRepo.getLocation(), "org/apache/archiva/archiva-test/1.0" ).exists() );
        assertTrue( new File( managedRepo.getLocation(), "org/apache/archiva/archiva-test/1.1" ).exists() );
    }

    public void testDeleteM1ArtifactArtifactExists()
        throws Exception
    {
        MockControl fileTypesControl = MockClassControl.createControl( FileTypes.class );
        FileTypes fileTypes = (FileTypes) fileTypesControl.getMock();

        MockControl pathParserControl = MockClassControl.createControl( PathParser.class );
        PathParser parser = (PathParser) pathParserControl.getMock();

        ManagedRepositoryConfiguration managedRepo = createManagedRepo( "legacy", "legacy-repo" );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ), managedRepo );

        ManagedLegacyRepositoryContent repoContent = new ManagedLegacyRepositoryContent();
        repoContent.setRepository( managedRepo );
        repoContent.setFileTypes( fileTypes );
        repoContent.setLegacyPathParser( parser );

        repoFactoryControl.expectAndReturn( repositoryFactory.getManagedRepositoryContent( "internal" ), repoContent );

        recordInManagedLegacyRepoContent( fileTypesControl, fileTypes, pathParserControl, parser );

        List<ArtifactMetadata> artifacts = getArtifacts();
        ArtifactMetadata artifact = artifacts.get( 0 );

        metadataRepositoryControl.expectAndReturn( metadataRepository.getArtifacts( repoContent.getId(),
                                                                                    artifact.getNamespace(),
                                                                                    artifact.getProject(),
                                                                                    artifact.getVersion() ), artifacts );
        metadataRepository.deleteArtifact( repoContent.getId(), artifact.getNamespace(), artifact.getProject(),
                                           artifact.getVersion(), artifact.getId() );

        listener.deleteArtifact( repoContent.getId(), artifact.getNamespace(), artifact.getProject(),
                                 artifact.getVersion(), artifact.getId() );
        listenerControl.setVoidCallable( 1 );

        archivaConfigControl.replay();
        configControl.replay();
        repoFactoryControl.replay();
        metadataRepositoryControl.replay();
        listenerControl.replay();
        fileTypesControl.replay();
        pathParserControl.replay();

        boolean success = service.deleteArtifact( "internal", "org.apache.archiva", "archiva-test", "1.0" );
        assertTrue( success );

        archivaConfigControl.verify();
        configControl.verify();
        repoFactoryControl.verify();
        metadataRepositoryControl.verify();
        listenerControl.verify();
        fileTypesControl.verify();
        pathParserControl.verify();

        File repo = new File( managedRepo.getLocation() );
        assertFalse( new File( repo, "org.apache.archiva/jars/archiva-test-1.0.jar" ).exists() );
        assertFalse( new File( repo, "org.apache.archiva/poms/archiva-test-1.0.pom" ).exists() );

        assertTrue( new File( repo, "org.apache.archiva/jars/archiva-test-1.1.jar" ).exists() );
        assertTrue( new File( repo, "org.apache.archiva/jars/archiva-diff-1.0.jar" ).exists() );
        assertTrue( new File( repo, "org.apache.archiva/poms/archiva-test-1.1.pom" ).exists() );
        assertTrue( new File( repo, "org.apache.archiva/poms/archiva-diff-1.0.pom" ).exists() );
    }

    public void testDeleteArtifactArtifactDoesNotExist()
        throws Exception
    {
        ManagedRepositoryConfiguration managedRepo = createManagedRepo( "default", "default-repo" );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ), managedRepo );

        ManagedDefaultRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( managedRepo );

        repoFactoryControl.expectAndReturn( repositoryFactory.getManagedRepositoryContent( "internal" ), repoContent );

        archivaConfigControl.replay();
        configControl.replay();
        repoFactoryControl.replay();

        try
        {
            service.deleteArtifact( "internal", "org.apache.archiva", "archiva-non-existing", "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Artifact does not exist.", e.getMessage() );
        }

        archivaConfigControl.verify();
        configControl.verify();
        repoFactoryControl.verify();
    }

    private ManagedRepositoryConfiguration createManagedRepo( String layout, String directory )
        throws IOException
    {
        File srcDir = new File( getBasedir(), "src/test/repositories/" + directory );

        File repoDir = getTestFile( "target/test-repos/" + directory );

        FileUtils.deleteDirectory( repoDir );

        FileUtils.copyDirectory( srcDir, repoDir, FileFilterUtils.makeSVNAware( null ) );

        ManagedRepositoryConfiguration managedRepo =
            createManagedRepo( "internal", layout, "Internal Repository", true, false );
        managedRepo.setLocation( repoDir.getAbsolutePath() );
        return managedRepo;
    }

    public void testDeleteArtifacRepositoryDoesNotExist()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "non-existing-repo" ), null );

        archivaConfigControl.replay();
        configControl.replay();

        try
        {
            service.deleteArtifact( "non-existing-repo", "org.apache.archiva", "archiva-test", "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Repository does not exist.", e.getMessage() );
        }

        archivaConfigControl.verify();
        configControl.verify();
    }

    /* Tests for repository scanning */

    public void testExecuteRepoScannerRepoExistsAndNotBeingScanned()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ),
                                       createManagedRepo( "internal", "default", "Internal Repository", true, false ) );

        RepositoryTask task = new RepositoryTask();

        repositoryTaskSchedulerControl.expectAndReturn( repositoryTaskScheduler.isProcessingRepositoryTask( "internal" ),
                                                        false );

        repositoryTaskScheduler.queueTask( task );
        repositoryTaskSchedulerControl.setMatcher( MockControl.ALWAYS_MATCHER );
        repositoryTaskSchedulerControl.setVoidCallable();

        archivaConfigControl.replay();
        configControl.replay();
        repositoryTaskSchedulerControl.replay();

        try
        {
            boolean success = service.executeRepositoryScanner( "internal" );
            assertTrue( success );
        }
        catch ( Exception e )
        {
            fail( "An exception should not have been thrown." );
        }

        archivaConfigControl.verify();
        configControl.verify();
        repositoryTaskSchedulerControl.verify();
    }

    public void testExecuteRepoScannerRepoExistsButBeingScanned()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ),
                                       createManagedRepo( "internal", "default", "Internal Repository", true, false ) );

        repositoryTaskSchedulerControl.expectAndReturn( repositoryTaskScheduler.isProcessingRepositoryTask( "internal" ),
                                                        true );

        archivaConfigControl.replay();
        configControl.replay();
        repositoryTaskSchedulerControl.replay();

        try
        {
            boolean success = service.executeRepositoryScanner( "internal" );
            assertFalse( success );
        }
        catch ( Exception e )
        {
            fail( "An exception should not have been thrown." );
        }

        archivaConfigControl.verify();
        configControl.verify();
        repositoryTaskSchedulerControl.verify();
    }

    public void testExecuteRepoScannerRepoDoesNotExist()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ), null );

        archivaConfigControl.replay();
        configControl.replay();

        try
        {
            service.executeRepositoryScanner( "internal" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Repository does not exist.", e.getMessage() );
        }

        archivaConfigControl.verify();
        configControl.verify();
    }

    /* Tests for querying repositories */

    public void testGetAllManagedRepositories()
        throws Exception
    {
        List<ManagedRepositoryConfiguration> managedRepos = new ArrayList<ManagedRepositoryConfiguration>();
        managedRepos.add( createManagedRepo( "internal", "default", "Internal Repository", true, false ) );
        managedRepos.add( createManagedRepo( "snapshots", "default", "Snapshots Repository", false, true ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getManagedRepositories(), managedRepos );

        archivaConfigControl.replay();
        configControl.replay();

        List<ManagedRepository> repos = service.getAllManagedRepositories();

        archivaConfigControl.verify();
        configControl.verify();

        assertNotNull( repos );
        assertEquals( 2, repos.size() );

        assertManagedRepo( repos.get( 0 ), managedRepos.get( 0 ) );
        assertManagedRepo( repos.get( 1 ), managedRepos.get( 1 ) );
    }

    public void testGetAllRemoteRepositories()
        throws Exception
    {
        List<RemoteRepositoryConfiguration> remoteRepos = new ArrayList<RemoteRepositoryConfiguration>();
        remoteRepos.add( createRemoteRepository( "central", "Central Repository", "default",
                                                 "http://repo1.maven.org/maven2" ) );
        remoteRepos.add( createRemoteRepository( "dummy", "Dummy Remote Repository", "legacy", "http://dummy.com/dummy" ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRemoteRepositories(), remoteRepos );

        archivaConfigControl.replay();
        configControl.replay();

        List<RemoteRepository> repos = service.getAllRemoteRepositories();

        archivaConfigControl.verify();
        configControl.verify();

        assertNotNull( repos );
        assertEquals( 2, repos.size() );

        assertRemoteRepo( repos.get( 0 ), remoteRepos.get( 0 ) );
        assertRemoteRepo( repos.get( 1 ), remoteRepos.get( 1 ) );
    }

    /* Merge method */

    public void testMergeRepositoryWithInvalidRepository()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "invalid" ), null );

        archivaConfigControl.replay();
        configControl.replay();

        try
        {
            service.merge( "invalid", true );
        }
        catch ( Exception e )
        {
            assertEquals( "Repository Id : invalid not found.", e.getMessage() );
        }

        archivaConfigControl.verify();
        configControl.verify();
    }

    public void testMergeWithNoStagingRepository()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "repo" ),
                                       createManagedRepo( "repo", "default", "repo", true, false ) );
        configControl.expectAndReturn( config.findManagedRepositoryById( "repo-stage" ), null );

        archivaConfigControl.replay();
        configControl.replay();

        try
        {
            service.merge( "repo", true );
        }
        catch ( Exception e )
        {
            assertEquals( "Staging Id : repo-stage not found.", e.getMessage() );
        }

        archivaConfigControl.verify();
        configControl.verify();
    }

    public void testMergeRepositoriesAndScan()
        throws Exception
    {
        List<ArtifactMetadata> sources = new ArrayList<ArtifactMetadata>();

        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( "artifact" );
        artifact.setFileLastModified( System.currentTimeMillis() );

        sources.add( artifact );

        ManagedRepositoryConfiguration merge = createManagedRepo( "merge", "default", "merge", true, true );
        merge.setLocation( "target/test-repository/merge" );
        ManagedRepositoryConfiguration staging = createStagingRepo( merge );

        RepositoryTask task = new RepositoryTask();
        task.setScanAll( true );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "merge" ), merge );
        configControl.expectAndReturn( config.findManagedRepositoryById( "merge-stage" ), staging );

        metadataRepositoryControl.expectAndReturn( metadataRepository.getArtifacts( staging.getId() ), sources );
        repositoryMergerControl.expectAndDefaultReturn( repositoryMerger.getConflictingArtifacts( staging.getId(),
                                                                                                  merge.getId() ),
                                                        sources );
        repositoryMerger.merge( staging.getId(), merge.getId() );
        repositoryMergerControl.setVoidCallable();
        repositoryTaskSchedulerControl.expectAndReturn( repositoryTaskScheduler.isProcessingRepositoryTask( "merge" ),
                                                        false );

        // scanning after merge
        repositoryTaskScheduler.queueTask( task );
        repositoryTaskSchedulerControl.setMatcher( MockControl.ALWAYS_MATCHER );
        repositoryTaskSchedulerControl.setVoidCallable();

        // audit logs
        metadataRepository.addMetadataFacet( merge.getId(), createAuditEvent( merge ) );
        metadataRepositoryControl.setMatcher( MockControl.ALWAYS_MATCHER );
        metadataRepositoryControl.setVoidCallable();

        archivaConfigControl.replay();
        metadataRepositoryControl.replay();
        configControl.replay();
        repositoryMergerControl.replay();
        repositoryTaskSchedulerControl.replay();

        boolean a = service.merge( "merge", false );
        assertTrue( a );

        archivaConfigControl.verify();
        configControl.verify();
        configControl.verify();
        metadataRepositoryControl.verify();
        repositoryMergerControl.verify();
        repositoryTaskSchedulerControl.verify();
    }

    public void testMergeRepositoriesWithConflictsAndScan()
        throws Exception
    {
        List<ArtifactMetadata> sources = new ArrayList<ArtifactMetadata>();
        ArtifactMetadata one = new ArtifactMetadata();
        one.setId( "one" );
        one.setVersion( "1.0" );

        ArtifactMetadata two = new ArtifactMetadata();
        two.setId( "two" );
        two.setVersion( "1.0-SNAPSHOT" );

        sources.add( one );
        sources.add( two );

        List<ArtifactMetadata> conflicts = new ArrayList<ArtifactMetadata>();
        conflicts.add( one );

        sources.removeAll( conflicts );

        Filter<ArtifactMetadata> artifactsWithOutConflicts = new IncludesFilter<ArtifactMetadata>( sources );

        RepositoryTask task = new RepositoryTask();
        task.setScanAll( true );

        ManagedRepositoryConfiguration repo = createManagedRepo( "repo", "default", "repo", true, true );
        repo.setLocation( "target/test-repository/one" );
        ManagedRepositoryConfiguration staging = createStagingRepo( repo );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "repo" ), repo );
        configControl.expectAndReturn( config.findManagedRepositoryById( "repo-stage" ), staging );

        metadataRepositoryControl.expectAndReturn( metadataRepository.getArtifacts( staging.getId() ), sources );
        repositoryMergerControl.expectAndDefaultReturn( repositoryMerger.getConflictingArtifacts( staging.getId(),
                                                                                                  repo.getId() ),
                                                        conflicts );
        repositoryMerger.merge( staging.getId(), repo.getId(), artifactsWithOutConflicts );
        repositoryMergerControl.setMatcher( MockControl.ALWAYS_MATCHER );
        repositoryMergerControl.setVoidCallable();
        repositoryTaskSchedulerControl.expectAndReturn( repositoryTaskScheduler.isProcessingRepositoryTask( "repo" ),
                                                        false );
        repositoryTaskScheduler.queueTask( task );
        repositoryTaskSchedulerControl.setMatcher( MockControl.ALWAYS_MATCHER );
        repositoryTaskSchedulerControl.setVoidCallable();

        // audit logs
        metadataRepository.addMetadataFacet( repo.getId(), createAuditEvent( repo ) );
        metadataRepositoryControl.setMatcher( MockControl.ALWAYS_MATCHER );
        metadataRepositoryControl.setVoidCallable();

        archivaConfigControl.replay();
        metadataRepositoryControl.replay();
        configControl.replay();
        repositoryMergerControl.replay();
        repositoryTaskSchedulerControl.replay();

        boolean a = service.merge( "repo", true );
        assertTrue( a );

        archivaConfigControl.verify();
        configControl.verify();
        configControl.verify();
        metadataRepositoryControl.verify();
        repositoryMergerControl.verify();
        repositoryTaskSchedulerControl.verify();
    }

    public void testAddManagedRepository()
        throws Exception
    {
        String projId = "org.apache.archiva";
        String repoId = projId + ".releases";
        String layout = "default";
        String name = projId + " Releases";
        String releaseLocation = "target/test-repository/" + projId + ".releases";
        String stageLocation = releaseLocation + "-stage";

        ManagedRepositoryConfiguration managedRepo = createManagedRepo( "repo1", "default", "repo", true, false );
        RemoteRepositoryConfiguration remoteRepo =
            createRemoteRepository( "central", "Central Repository", "default", "http://repo1.maven.org/maven2" );
        List<String> repositories = new ArrayList<String>();
        repositories.add( managedRepo.getName() );
        RepositoryGroupConfiguration repoGroup = createRepoGroupConfig( "repoGroup", repositories );
        Map<String, ManagedRepositoryConfiguration> managedRepoMap =
            new HashMap<String, ManagedRepositoryConfiguration>();
        Map<String, RemoteRepositoryConfiguration> remoteRepoMap = new HashMap<String, RemoteRepositoryConfiguration>();
        Map<String, RepositoryGroupConfiguration> repoGroupMap = new HashMap<String, RepositoryGroupConfiguration>();
        managedRepoMap.put( "repo1", managedRepo );
        remoteRepoMap.put( "repo1", remoteRepo );
        repoGroupMap.put( "repo1", repoGroup );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        configControl.expectAndReturn( config.getManagedRepositoriesAsMap(), managedRepoMap );
        configControl.expectAndReturn( config.getRemoteRepositoriesAsMap(), remoteRepoMap );
        configControl.expectAndReturn( config.getRepositoryGroupsAsMap(), repoGroupMap );
        config.addManagedRepository( managedRepo );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        config.addManagedRepository( managedRepo );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();

        archivaConfigControl.replay();
        configControl.replay();
        assertFalse( new File( releaseLocation ).isDirectory() );
        assertFalse( new File( stageLocation ).isDirectory() );
        boolean success =
            service.addManagedRepository( repoId, layout, name, releaseLocation, true, true, false, true,
                                          "0 15 3 * * ? *" );
        assertTrue( success );
        assertTrue( new File( releaseLocation ).isDirectory() );
        assertTrue( new File( stageLocation ).isDirectory() );
        new File( releaseLocation ).delete();
        new File( stageLocation ).delete();
    }

    /* private methods */

    private void assertRemoteRepo( RemoteRepository remoteRepo, RemoteRepositoryConfiguration expectedRepoConfig )
    {
        assertEquals( expectedRepoConfig.getId(), remoteRepo.getId() );
        assertEquals( expectedRepoConfig.getLayout(), remoteRepo.getLayout() );
        assertEquals( expectedRepoConfig.getName(), remoteRepo.getName() );
        assertEquals( expectedRepoConfig.getUrl(), remoteRepo.getUrl() );
    }

    private RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String layout, String url )
    {
        RemoteRepositoryConfiguration remoteConfig = new RemoteRepositoryConfiguration();
        remoteConfig.setId( id );
        remoteConfig.setName( name );
        remoteConfig.setLayout( layout );
        remoteConfig.setUrl( url );

        return remoteConfig;
    }

    private void assertManagedRepo( ManagedRepository managedRepo, ManagedRepositoryConfiguration expectedRepoConfig )
    {
        assertEquals( expectedRepoConfig.getId(), managedRepo.getId() );
        assertEquals( expectedRepoConfig.getLayout(), managedRepo.getLayout() );
        assertEquals( expectedRepoConfig.getName(), managedRepo.getName() );

        // TODO enable assert once fixed in AdministrationServiceImpl!
        // assertEquals( "http://localhost:8080/archiva/repository/" + expectedRepoConfig.getId(), managedRepo.getUrl()
        // );
        assertEquals( expectedRepoConfig.isReleases(), managedRepo.isReleases() );
        assertEquals( expectedRepoConfig.isSnapshots(), managedRepo.isSnapshots() );
    }

    private ManagedRepositoryConfiguration createManagedRepo( String id, String layout, String name,
                                                              boolean hasReleases, boolean hasSnapshots )
    {
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( id );
        repoConfig.setLayout( layout );
        repoConfig.setName( name );
        repoConfig.setReleases( hasReleases );
        repoConfig.setSnapshots( hasSnapshots );

        return repoConfig;
    }

    private ManagedRepositoryConfiguration createStagingRepo( ManagedRepositoryConfiguration repoConfig )
    {
        ManagedRepositoryConfiguration stagingRepo = new ManagedRepositoryConfiguration();
        stagingRepo.setId( repoConfig.getId() + STAGE );
        stagingRepo.setLayout( repoConfig.getLayout() );
        stagingRepo.setName( repoConfig + STAGE );
        stagingRepo.setReleases( repoConfig.isReleases() );
        stagingRepo.setSnapshots( repoConfig.isSnapshots() );
        stagingRepo.setLocation( repoConfig.getLocation() );

        return stagingRepo;
    }

    private AuditEvent createAuditEvent( ManagedRepositoryConfiguration repoConfig )
    {
        AuditEvent auditEvent = new AuditEvent();

        auditEvent.setAction( AuditEvent.MERGE_REPO_REMOTE );
        auditEvent.setRepositoryId( repoConfig.getId() );
        auditEvent.setResource( repoConfig.getLocation() );
        auditEvent.setTimestamp( new Date() );

        return auditEvent;
    }

    private void recordRepoConsumers()
    {
        List<KnownRepositoryContentConsumer> availableKnownConsumers = new ArrayList<KnownRepositoryContentConsumer>();
        availableKnownConsumers.add( indexArtifactConsumer );
        availableKnownConsumers.add( indexPomConsumer );

        List<InvalidRepositoryContentConsumer> availableInvalidConsumers =
            new ArrayList<InvalidRepositoryContentConsumer>();
        availableInvalidConsumers.add( checkPomConsumer );
        availableInvalidConsumers.add( checkMetadataConsumer );

        repoConsumerUtilsControl.expectAndReturn( repoConsumersUtil.getAvailableKnownConsumers(),
                                                  availableKnownConsumers );
        knownContentConsumerControl.expectAndReturn( indexArtifactConsumer.getId(), "index-artifact" );
        knownContentConsumerControl.expectAndReturn( indexPomConsumer.getId(), "index-pom" );

        repoConsumerUtilsControl.expectAndReturn( repoConsumersUtil.getAvailableInvalidConsumers(),
                                                  availableInvalidConsumers );
        invalidContentConsumerControl.expectAndReturn( checkPomConsumer.getId(), "check-pom" );
        invalidContentConsumerControl.expectAndReturn( checkMetadataConsumer.getId(), "check-metadata" );
    }

    private void recordInManagedLegacyRepoContent( MockControl fileTypesControl, FileTypes fileTypes,
                                                   MockControl pathParserControl, PathParser parser )
        throws LayoutException
    {
        String sep = File.separator;
        String ad10p = "org.apache.archiva" + sep + "poms" + sep + "archiva-diff-1.0.pom";
        String at10p = "org.apache.archiva" + sep + "poms" + sep + "archiva-test-1.0.pom";
        String at11p = "org.apache.archiva" + sep + "poms" + sep + "archiva-test-1.1.pom";
        String ad10j = "org.apache.archiva" + sep + "jars" + sep + "archiva-diff-1.0.jar";
        String at10j = "org.apache.archiva" + sep + "jars" + sep + "archiva-test-1.0.jar";
        String at11j = "org.apache.archiva" + sep + "jars" + sep + "archiva-test-1.1.jar";

        fileTypesControl.expectAndReturn( fileTypes.matchesArtifactPattern( at10p ), true );
        fileTypesControl.expectAndReturn( fileTypes.matchesArtifactPattern( at11p ), true );
        fileTypesControl.expectAndReturn( fileTypes.matchesArtifactPattern( ad10p ), true );
        fileTypesControl.expectAndReturn( fileTypes.matchesArtifactPattern( ad10j ), true );
        fileTypesControl.expectAndReturn( fileTypes.matchesArtifactPattern( at10j ), true );
        fileTypesControl.expectAndReturn( fileTypes.matchesArtifactPattern( at11j ), true );

        ArtifactReference aRef = createArtifactReference( "archiva-test", "org.apache.archiva", "1.1", "pom" );
        pathParserControl.expectAndReturn( parser.toArtifactReference( at11p ), aRef );

        aRef = createArtifactReference( "archiva-test", "org.apache.archiva", "1.0", "pom" );
        pathParserControl.expectAndReturn( parser.toArtifactReference( at10p ), aRef );

        aRef = createArtifactReference( "archiva-diff", "org.apache.archiva", "1.0", "pom" );
        pathParserControl.expectAndReturn( parser.toArtifactReference( ad10p ), aRef );

        aRef = createArtifactReference( "archiva-diff", "org.apache.archiva", "1.0", "jar" );
        pathParserControl.expectAndReturn( parser.toArtifactReference( ad10j ), aRef );

        aRef = createArtifactReference( "archiva-test", "org.apache.archiva", "1.0", "jar" );
        pathParserControl.expectAndReturn( parser.toArtifactReference( at10j ), aRef );

        aRef = createArtifactReference( "archiva-test", "org.apache.archiva", "1.1", "jar" );
        pathParserControl.expectAndReturn( parser.toArtifactReference( at11j ), aRef );
    }

    private List<ArtifactMetadata> getArtifacts()
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();

        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( "archiva-test-1.0.jar" );
        artifact.setProject( "archiva-test" );
        artifact.setVersion( "1.0" );
        artifact.setProjectVersion( "1.0" );
        artifact.setNamespace( "org.apache.archiva" );
        artifact.setRepositoryId( "internal" );
        artifacts.add( artifact );
        return artifacts;
    }

    private ArtifactReference createArtifactReference( String artifactId, String groupId, String version, String type )
    {
        ArtifactReference aRef = new ArtifactReference();
        aRef.setArtifactId( artifactId );
        aRef.setGroupId( groupId );
        aRef.setType( type );
        aRef.setVersion( version );

        return aRef;
    }

    private RepositoryGroupConfiguration createRepoGroupConfig( String id, List<String> repositories )
    {
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( id );
        repoGroup.setRepositories( repositories );
        return repoGroup;
    }
}
