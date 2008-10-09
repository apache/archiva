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
    
    private AdministrationService service;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = ( ArchivaConfiguration ) archivaConfigControl.getMock();
        
        configControl = MockClassControl.createControl( Configuration.class );
        config = ( Configuration ) configControl.getMock();      
        
        service = new AdministrationServiceImpl();
    }
    
    public void testConfigureValidDatabaseConsumer()
        throws Exception
    {
        /*DatabaseScanningConfiguration dbScanning = new DatabaseScanningConfiguration();
        dbScanning.addCleanupConsumer( "cleanup-index" );
        dbScanning.addCleanupConsumer( "cleanup-database" );
        dbScanning.addUnprocessedConsumer( "unprocessed-artifacts" );
        dbScanning.addUnprocessedConsumer( "unprocessed-poms" );
        
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
        
        boolean success = service.configureDatabaseConsumer( "new-cleanup-consumer", true ); 
        
        archivaConfigControl.verify();
        configControl.verify();
        
        assertTrue( success );
        
      // test disable 
        archivaConfigControl.reset();
        configControl.reset();
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getDatabaseScanning(), dbScanning );
        
        config.setDatabaseScanning( dbScanning );
        configControl.setMatcher( MockControl.ALWAYS_MATCHER );
        configControl.setVoidCallable();
        
        archivaConfig.save( config );
        archivaConfigControl.setVoidCallable();
        
        archivaConfigControl.replay();
        configControl.replay();
        
        success = service.configureDatabaseConsumer( "new-cleanup-consumer", false ); 
        
        archivaConfigControl.verify();
        configControl.verify();
        
        assertTrue( success );*/
    }
    
    public void testConfigureInvalidDatabaseConsumer()
        throws Exception
    {
        
    }
    
    public void testConfigureValidRepositoryConsumer()
        throws Exception
    {
        // test enable & disable
    }
    
    public void testConfigureInvalidRepositoryConsumer()
        throws Exception
    {
    
    }
    
    public void testDeleteArtifactArtifactExists()
        throws Exception
    {
    
    }
    
    public void testDeleteArtifactArtifactDoesNotExist()
        throws Exception
    {
    
    }
    
    public void testDeleteArtifacRepositoryDoesNotExist()
        throws Exception
    {
    
    }
    
    public void testExecuteRepoScannerRepoExists()
        throws Exception
    {
    
    }
    
    public void testExecuteRepoScannerRepoDoesNotExist()
        throws Exception
    {
    
    }
    
    public void testExecuteDbScanner()
        throws Exception
    {
        
    }
    
    public void testGetAllDbConsumers()
        throws Exception
    {
        /*DatabaseScanningConfiguration dbScanning = new DatabaseScanningConfiguration();
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
        assertTrue( dbConsumers.contains( "unprocessed-poms" ) );*/
    }
    
    public void testGetAllRepoConsumers()
        throws Exception
    {
        /*RepositoryScanningConfiguration repoScanning = new RepositoryScanningConfiguration();
        repoScanning.addKnownContentConsumer( "index-artifacts" );
        repoScanning.addKnownContentConsumer( "index-poms" );
        repoScanning.addKnownContentConsumer( "fix-checksums" );
        repoScanning.addInvalidContentConsumer( "check-poms" );
        repoScanning.addInvalidContentConsumer( "check-metadata" );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        configControl.expectAndReturn( config.getRepositoryScanning(), repoScanning );
        
        archivaConfigControl.replay();
        configControl.replay();
        
        List<String> repoConsumers = service.getAllDatabaseConsumers(); 
        
        archivaConfigControl.verify();
        configControl.verify();
        
        assertNotNull( repoConsumers );
        assertEquals( 5, repoConsumers.size() );
        assertTrue( repoConsumers.contains( "index-artifacts" ) );
        assertTrue( repoConsumers.contains( "index-poms" ) );
        assertTrue( repoConsumers.contains( "fix-checksums" ) );
        assertTrue( repoConsumers.contains( "check-poms" ) );
        assertTrue( repoConsumers.contains( "check-metadata" ) );*/
    }
    
    public void testGetAllManagedRepositories()
        throws Exception
    {
        /*List<ManagedRepositoryConfiguration> managedRepos = new ArrayList<ManagedRepositoryConfiguration>();        
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
        assertManagedRepo( ( ManagedRepository ) repos.get( 1 ), managedRepos.get( 1 ) );*/
    }

    public void testGetAllRemoteRepositories()
        throws Exception
    {
        /*List<RemoteRepositoryConfiguration> remoteRepos = new ArrayList<RemoteRepositoryConfiguration>(); 
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
        assertRemoteRepo( (RemoteRepository) repos.get( 1 ), remoteRepos.get( 1 ) );        */
    }

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
    
}