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

import java.util.ArrayList;
import java.util.List;

import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * 
 * @version $Id$
 */
public class AdministrationServiceImplTest
    extends PlexusInSpringTestCase
{    
    private MockControl archivaConfigControl;
    
    private ArchivaConfiguration archivaConfig;
    
    private MockControl configControl;
    
    private Configuration config;
    
    private AdministrationServiceImpl service;
    
    private MockControl taskSchedulerControl;
    
    private ArchivaTaskScheduler taskScheduler;
    
    private MockControl consumerUtilControl;
    
    private RepositoryContentConsumers consumerUtil;

    private MockControl knownContentConsumerControl;

    private MockControl invalidContentConsumerControl;

    private KnownRepositoryContentConsumer indexArtifactConsumer;

    private KnownRepositoryContentConsumer indexPomConsumer;

    private InvalidRepositoryContentConsumer checkPomConsumer;

    private InvalidRepositoryContentConsumer checkMetadataConsumer;
        
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = ( ArchivaConfiguration ) archivaConfigControl.getMock();
        
        configControl = MockClassControl.createControl( Configuration.class );
        config = ( Configuration ) configControl.getMock();      
        
        taskSchedulerControl = MockControl.createControl( ArchivaTaskScheduler.class );
        taskScheduler = ( ArchivaTaskScheduler ) taskSchedulerControl.getMock();
        
        consumerUtilControl = MockClassControl.createControl( RepositoryContentConsumers.class );
        consumerUtil = ( RepositoryContentConsumers ) consumerUtilControl.getMock();
        
        knownContentConsumerControl = MockControl.createControl( KnownRepositoryContentConsumer.class );
        indexArtifactConsumer = ( KnownRepositoryContentConsumer ) knownContentConsumerControl.getMock();
        indexPomConsumer = ( KnownRepositoryContentConsumer ) knownContentConsumerControl.getMock();
        
        invalidContentConsumerControl = MockControl.createControl( InvalidRepositoryContentConsumer.class );
        checkPomConsumer = ( InvalidRepositoryContentConsumer ) invalidContentConsumerControl.getMock();
        checkMetadataConsumer = ( InvalidRepositoryContentConsumer ) invalidContentConsumerControl.getMock();
        
        service = new AdministrationServiceImpl();
        service.setArchivaConfiguration( archivaConfig );
        service.setConsumerUtil( consumerUtil );        
    }
        
  // DATABASE CONSUMERS
    /*    public void testGetAllDbConsumers()
        throws Exception
    {   
        DatabaseScanningConfiguration dbScanning = new DatabaseScanningConfiguration();
        dbScanning.addCleanupConsumer( "cleanup-index" );
        dbScanning.addCleanupConsumer( "cleanup-database" );
        dbScanning.addUnprocessedConsumer( "unprocessed-artifacts" );
        dbScanning.addUnprocessedConsumer( "unprocessed-poms" );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getDatabaseScanning(), dbScanning );
        
        archivaConfigControl.replay();
        configControl.replay();
        
        
        
        List<String> dbConsumers = service.getAllDatabaseConsumers(); 
        
        archivaConfigControl.verify();
        configControl.verify();
        
        assertNotNull( dbConsumers );
        assertEquals( 4, dbConsumers.size() );
        assertTrue( dbConsumers.contains( "cleanup-index" ) );
        assertTrue( dbConsumers.contains( "cleanup-database" ) );
        assertTrue( dbConsumers.contains( "unprocessed-artifacts" ) );
        assertTrue( dbConsumers.contains( "unprocessed-poms" ) );
    }
    
    public void testConfigureValidDatabaseConsumer()
        throws Exception
    {
        DatabaseScanningConfiguration dbScanning = new DatabaseScanningConfiguration();
        dbScanning.addCleanupConsumer( "cleanup-index" );
        dbScanning.addCleanupConsumer( "cleanup-database" );
        dbScanning.addUnprocessedConsumer( "unprocessed-artifacts" );
        dbScanning.addUnprocessedConsumer( "unprocessed-poms" );
     
      //TODO mock checking whether the db consumer is valid or not
        
     // test enable
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getDatabaseScanning(), dbScanning );
        
        config.setDatabaseScanning( dbScanning );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        
        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();
        
        archivaConfigControl.replay();
        configControl.replay();
        
        try
        {
            boolean success = service.configureDatabaseConsumer( "new-cleanup-consumer", true );
            assertTrue( success );
        }
        catch ( Exception e )
        {
            fail( "An exception should not have been thrown." );
        }
        
        archivaConfigControl.verify();
        configControl.verify();
                
      // test disable 
        archivaConfigControl.reset();
        configControl.reset();
        
      //TODO mock checking whether the db consumer is valid or not
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getDatabaseScanning(), dbScanning );
        
        config.setDatabaseScanning( dbScanning );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        
        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();
        
        archivaConfigControl.replay();
        configControl.replay();
        
        try
        {
            boolean success = service.configureDatabaseConsumer( "new-cleanup-consumer", false );
            assertTrue( success );
        }
        catch ( Exception e )
        {
            fail( "An exception should not have been thrown." );
        }
        
        archivaConfigControl.verify();
        configControl.verify();
    }
    
    public void testConfigureInvalidDatabaseConsumer()
        throws Exception
    {
        //TODO mock checking whether the db consumer is valid or not
        
        try
        {
            service.configureDatabaseConsumer( "invalid-consumer", true );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Invalid database consumer.", e.getMessage() );
        }
    }
    
    */
    
 // REPOSITORY CONSUMERS
    public void testGetAllRepoConsumers()
        throws Exception
    {   
        recordRepoConsumerValidation();
        
        consumerUtilControl.replay();
        knownContentConsumerControl.replay();
        invalidContentConsumerControl.replay();
                
        List<String> repoConsumers = service.getAllRepositoryConsumers(); 
        
        consumerUtilControl.verify();
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
        recordRepoConsumerValidation();
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRepositoryScanning(), repoScanning );
        
        config.setRepositoryScanning( repoScanning );                
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        
        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();
                
        consumerUtilControl.replay();
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
        
        consumerUtilControl.verify();
        knownContentConsumerControl.verify();
        invalidContentConsumerControl.verify();        
        archivaConfigControl.verify();
        configControl.verify();
                
     // test disable "check-metadata" consumer 
        consumerUtilControl.reset();
        knownContentConsumerControl.reset();
        invalidContentConsumerControl.reset();        
        archivaConfigControl.reset();
        configControl.reset();
        
        repoScanning.addInvalidContentConsumer( "check-metadata" );

        recordRepoConsumerValidation();
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRepositoryScanning(), repoScanning );
        
        config.setRepositoryScanning( repoScanning );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        
        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();
                
        consumerUtilControl.replay();
        knownContentConsumerControl.replay();
        invalidContentConsumerControl.replay();
        archivaConfigControl.replay();
        configControl.replay();
        
        try
        {
            boolean success = service.configureRepositoryConsumer( null, "check-metadata", false );
            
            consumerUtilControl.verify();
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
 
    /*
    public void testConfigureInvalidRepositoryConsumer()
        throws Exception
    {
        //TODO mock checking whether the repo consumer is valid or not
        
        try
        {
            service.configureRepositoryConsumer( null, "invalid-consumer", true );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Invalid repository consumer.", e.getMessage() );
        }
    }

// DELETE ARTIFACT
    
    public void testDeleteArtifactArtifactExists()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ),
                                       createManagedRepo( "internal", "default", "Internal Repository", true, false ) );
        
        // TODO 
        // - mock checking of artifact existence in the repo
        // - mock artifact delete
        
        archivaConfigControl.replay();
        configControl.replay();
       
        try
        {
            boolean success = service.deleteArtifact( "internal", "org.apache.archiva", "archiva-test", "1.0" );
            assertTrue( success ); 
        }
        catch ( Exception e )
        {
            fail( "An exception should not have been thrown." );
        }
        
        archivaConfigControl.verify();
        configControl.verify();
    }
    
    public void testDeleteArtifactArtifactDoesNotExist()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ),
                                       createManagedRepo( "internal", "default", "Internal Repository", true, false ) );
        
        // TODO mock checking of artifact existence in the repo
        
        archivaConfigControl.replay();
        configControl.replay();
       
        try
        {
            service.deleteArtifact( "internal", "org.apache.archiva", "archiva-test", "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Artifact does not exist.", e.getMessage() );
        }
        
        archivaConfigControl.verify();
        configControl.verify();
    }
    
    public void testDeleteArtifacRepositoryDoesNotExist()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ), null );
        
        archivaConfigControl.replay();
        configControl.replay();
       
        try
        {
            service.deleteArtifact( "internal", "org.apache.archiva", "archiva-test", "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Repository does not exist.", e.getMessage() );
        }
        
        archivaConfigControl.verify();
        configControl.verify();
    }
    
// REPO SCANNING
    
    public void testExecuteRepoScannerRepoExists()
        throws Exception
    {        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.findManagedRepositoryById( "internal" ),
                                       createManagedRepo( "internal", "default", "Internal Repository", true, false ) );
        
        RepositoryTask task = new RepositoryTask();
        
        taskSchedulerControl.expectAndReturn( taskScheduler.isProcessingAnyRepositoryTask(), false );
        taskSchedulerControl.expectAndReturn( taskScheduler.isProcessingRepositoryTask( "internal" ), false );
        
        taskScheduler.queueRepositoryTask( task );
        taskSchedulerControl.setMatcher( MockControl.ALWAYS_MATCHER );
        taskSchedulerControl.setVoidCallable();
        
        archivaConfigControl.replay();
        configControl.replay();
        taskSchedulerControl.replay();

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
        taskSchedulerControl.verify();
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
    
 // DATABASE SCANNING
    
    public void testExecuteDbScanner()
        throws Exception
    {
        DatabaseTask task = new DatabaseTask();
        
        taskSchedulerControl.expectAndReturn( taskScheduler.isProcessingDatabaseTask(), false );
                
        taskScheduler.queueDatabaseTask( task );
        taskSchedulerControl.setMatcher( MockControl.ALWAYS_MATCHER );
        taskSchedulerControl.setVoidCallable();
        
        taskSchedulerControl.replay();

        boolean success = service.executeDatabaseScanner();
        
        taskSchedulerControl.verify();        
        
        assertTrue( success );
    }
     
 // REPOSITORIES
    
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
                
        assertManagedRepo( ( ManagedRepository ) repos.get( 0 ), managedRepos.get( 0 ) );
        assertManagedRepo( ( ManagedRepository ) repos.get( 1 ), managedRepos.get( 1 ) );
    }

    public void testGetAllRemoteRepositories()
        throws Exception
    {
        List<RemoteRepositoryConfiguration> remoteRepos = new ArrayList<RemoteRepositoryConfiguration>(); 
        remoteRepos.add( createRemoteRepository( "central", "Central Repository", "default", "http://repo1.maven.org/maven2") );
        remoteRepos.add( createRemoteRepository( "dummy", "Dummy Remote Repository", "legacy", "http://dummy.com/dummy") );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRemoteRepositories(), remoteRepos );
        
        archivaConfigControl.replay();
        configControl.replay();
        
        List<RemoteRepository> repos = service.getAllRemoteRepositories(); 
        
        archivaConfigControl.verify();
        configControl.verify();
        
        assertNotNull( repos );
        assertEquals( 2, repos.size() );
         
        assertRemoteRepo( (RemoteRepository) repos.get( 0 ), remoteRepos.get( 0 ) );
        assertRemoteRepo( (RemoteRepository) repos.get( 1 ), remoteRepos.get( 1 ) );        
    }
*/
    private void assertRemoteRepo( RemoteRepository remoteRepo, RemoteRepositoryConfiguration expectedRepoConfig )
    {
        assertEquals( expectedRepoConfig.getId(), remoteRepo.getId() );
        assertEquals( expectedRepoConfig.getLayout(), remoteRepo.getLayout() );
        assertEquals( expectedRepoConfig.getName(), remoteRepo.getName() );
        assertEquals( expectedRepoConfig.getUrl(), remoteRepo.getUrl() );       
    }
    
    private RemoteRepositoryConfiguration createRemoteRepository(String id, String name, String layout, String url)
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
        assertEquals( "http://localhost:8080/archiva/repository/" + expectedRepoConfig.getId(), managedRepo.getUrl() );
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
    
    private void recordRepoConsumerValidation()
    {
        List<KnownRepositoryContentConsumer> availableKnownConsumers = new ArrayList<KnownRepositoryContentConsumer>();
        availableKnownConsumers.add( indexArtifactConsumer );
        availableKnownConsumers.add( indexPomConsumer );
        
        List<InvalidRepositoryContentConsumer> availableInvalidConsumers = new ArrayList<InvalidRepositoryContentConsumer>();
        availableInvalidConsumers.add( checkPomConsumer );
        availableInvalidConsumers.add( checkMetadataConsumer );
        
        consumerUtilControl.expectAndReturn( consumerUtil.getAvailableKnownConsumers(), availableKnownConsumers );
        knownContentConsumerControl.expectAndReturn( indexArtifactConsumer.getId(), "index-artifact" );
        knownContentConsumerControl.expectAndReturn( indexPomConsumer.getId(), "index-pom" );
        
        consumerUtilControl.expectAndReturn( consumerUtil.getAvailableInvalidConsumers(), availableInvalidConsumers );
        invalidContentConsumerControl.expectAndReturn( checkPomConsumer.getId(), "check-pom" );
        invalidContentConsumerControl.expectAndReturn( checkMetadataConsumer.getId(), "check-metadata" );
    }
    
}