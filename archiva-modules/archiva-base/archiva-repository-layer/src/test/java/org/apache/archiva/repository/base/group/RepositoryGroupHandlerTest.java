package org.apache.archiva.repository.base.group;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.indexer.merger.MergedRemoteIndexesScheduler;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.archiva.repository.validation.ErrorKeys.ISEMPTY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( {MockitoExtension.class, SpringExtension.class} )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-group.xml"} )
class RepositoryGroupHandlerTest
{

    @Inject
    @Named( "repositoryRegistry" )
    ArchivaRepositoryRegistry repositoryRegistry;

    @Inject
    ConfigurationHandler configurationHandler;

    @Mock
    // @Named( "mergedRemoteIndexesScheduler#default" )
    MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler;

    @Inject
    List<RepositoryValidator<? extends Repository>> repositoryValidatorList;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    Path repoBaseDir;

    private RepositoryGroupHandler createHandler( )
    {
        RepositoryGroupHandler groupHandler = new RepositoryGroupHandler( repositoryRegistry, configurationHandler, mergedRemoteIndexesScheduler, repositoryValidatorList );
        groupHandler.init( );
        return groupHandler;
    }

    private Path getRepoBaseDir() throws IOException
    {
        if (repoBaseDir==null) {
            this.repoBaseDir = archivaConfiguration.getRepositoryBaseDir( ).resolve( "group" );
            Files.createDirectories( this.repoBaseDir );
        }
        return repoBaseDir;
    }



    protected EditableRepositoryGroup createRepository( String id, String name, Path location ) throws IOException
    {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(location.toAbsolutePath(), lockManager);
        BasicRepositoryGroup repo = new BasicRepositoryGroup(id, name, storage);
        repo.setLocation( location.toAbsolutePath().toUri());
        return repo;
    }

    protected EditableRepositoryGroup createRepository( String id, String name) throws IOException
    {
        Path dir = getRepoBaseDir( ).resolve( id );
        Files.createDirectories( dir );
        return createRepository( id, name, dir );
    }


    @Test
    void initializeFromConfig( )
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        groupHandler.initializeFromConfig( );
        assertEquals( 1, groupHandler.getAll( ).size( ) );
        assertNotNull( groupHandler.get( "test-group-01" ).getRepositories( ) );
        assertEquals( "internal", groupHandler.get( "test-group-01" ).getRepositories( ).get( 0 ).getId( ) );
    }

    @Test
    void activateRepository( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroup repo = groupHandler.newInstance( RepositoryType.MAVEN, "test-group-02" );
        groupHandler.activateRepository( repo );
        verify( mergedRemoteIndexesScheduler ).schedule( eq( repo ), any( ) );
        assertEquals( RepositoryState.INITIALIZED, repo.getLastState( ) );
    }

    @Test
    void newInstancesFromConfig( )
    {
        RepositoryGroupHandler groupHandler = new RepositoryGroupHandler( repositoryRegistry, configurationHandler, mergedRemoteIndexesScheduler, repositoryValidatorList );
        Map<String, RepositoryGroup> instances = groupHandler.newInstancesFromConfig( );
        assertFalse( groupHandler.hasRepository( "test-group-01" ) );
        assertTrue( instances.containsKey( "test-group-01" ) );
        assertEquals( RepositoryState.REFERENCES_SET, instances.get( "test-group-01" ).getLastState( ) );
    }

    @Test
    void newInstance( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroup instance = groupHandler.newInstance( RepositoryType.MAVEN, "test-group-03" );
        assertNotNull( instance );
        assertEquals( "test-group-03", instance.getId( ) );
        assertFalse( groupHandler.hasRepository( "test-group-03" ) );
        assertEquals( RepositoryState.REFERENCES_SET, instance.getLastState( ) );
    }


    @Test
    void put( ) throws IOException, RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        EditableRepositoryGroup repositoryGroup = createRepository( "test-group-04", "n-test-group-04" );
        groupHandler.put( repositoryGroup );
        RepositoryGroup storedGroup = groupHandler.get( "test-group-04" );
        assertNotNull( storedGroup );
        assertEquals( "test-group-04", storedGroup.getId( ) );
        assertEquals( "n-test-group-04", storedGroup.getName( ) );

        EditableRepositoryGroup repositoryGroup2 = createRepository( "test-group-04", "n2-test-group-04" );
        groupHandler.put( repositoryGroup2 );
        storedGroup = groupHandler.get( "test-group-04" );
        assertNotNull( storedGroup );
        assertEquals( "test-group-04", storedGroup.getId( ) );
        assertEquals( "n2-test-group-04", storedGroup.getName( ) );

        assertNotNull( configurationHandler.getBaseConfiguration().getRepositoryGroups( ) );
        assertTrue( configurationHandler.getBaseConfiguration().getRepositoryGroups( ).stream( ).anyMatch( g -> g!=null && "test-group-04".equals( g.getId( ) ) ) );
    }

    @Test
    void testPut( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        configuration.setId( "test-group-05" );
        configuration.setName( "n-test-group-05" );
        ArrayList<String> repos = new ArrayList<>( );
        repos.add( "internal" );
        configuration.setRepositories( repos );
        groupHandler.put( configuration );

        RepositoryGroup repo = groupHandler.get( "test-group-05" );
        assertNotNull( repo );
        assertEquals( "test-group-05", repo.getId( ) );
        assertEquals( "n-test-group-05", repo.getName( ) );

        assertNotNull( repo.getRepositories( ) );
        assertEquals( 1, repo.getRepositories( ).size( ) );
        assertEquals( "internal", repo.getRepositories( ).get( 0 ).getId( ) );
        assertNotNull( configurationHandler.getBaseConfiguration().getRepositoryGroups( ) );
        assertTrue( configurationHandler.getBaseConfiguration().getRepositoryGroups( ).stream( ).anyMatch( g -> g!=null && "test-group-05".equals( g.getId( ) ) ) );
    }

    @Test
    void testPutWithoutRegister( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        Configuration aCfg = new Configuration( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        configuration.setId( "test-group-06" );
        configuration.setName( "n-test-group-06" );
        ArrayList<String> repos = new ArrayList<>( );
        repos.add( "internal" );
        configuration.setRepositories( repos );
        groupHandler.put( configuration, aCfg );

        RepositoryGroup repo = groupHandler.get( "test-group-06" );
        assertNull( repo );
        assertNotNull( configurationHandler.getBaseConfiguration().getRepositoryGroups( ) );
        assertTrue( configurationHandler.getBaseConfiguration().getRepositoryGroups( ).stream( ).noneMatch( g -> g!=null && "test-group-06".equals( g.getId( ) ) ) );
        assertTrue( aCfg.getRepositoryGroups( ).stream( ).anyMatch( g -> g!=null && "test-group-06".equals( g.getId( ) ) ) );

    }

    @Test
    void putWithCheck( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        BasicRepositoryGroupValidator checker = new BasicRepositoryGroupValidator( configurationHandler );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        configuration.setId( "" );
        configuration.setName( "n-test-group-07" );
        ArrayList<String> repos = new ArrayList<>( );
        repos.add( "internal" );
        configuration.setRepositories( repos );
        CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.putWithCheck( configuration, checker );
        assertNull( groupHandler.get( "test-group-07" ) );
        assertNotNull( result.getResult( ) );
        assertNotNull( result.getResult( ).get( "id" ) );
        assertEquals( 1, result.getResult( ).get( "id" ).size( ) );
        assertEquals( ISEMPTY, result.getResult( ).get( "id" ).get( 0 ).getType( ) );
    }

    @Test
    void remove( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        configuration.setId( "test-group-08" );
        configuration.setName( "n-test-group-08" );
        groupHandler.put( configuration );
        assertNotNull( groupHandler.get( "test-group-08" ) );
        groupHandler.remove( "test-group-08" );
        assertNull( groupHandler.get( "test-group-08" ) );
    }

    @Test
    void testRemove( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        Configuration aCfg = new Configuration( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        configuration.setId( "test-group-09" );
        configuration.setName( "n-test-group-09" );
        ArrayList<String> repos = new ArrayList<>( );
        repos.add( "internal" );
        configuration.setRepositories( repos );
        groupHandler.put( configuration, aCfg );
        assertTrue( aCfg.getRepositoryGroups( ).stream( ).anyMatch( g -> g != null && "test-group-09".equals( g.getId( ) ) ) );
        groupHandler.remove( "test-group-09", aCfg );
        assertNull( groupHandler.get( "test-group-09" ) );
        assertTrue( aCfg.getRepositoryGroups( ).stream( ).noneMatch( g -> g != null && "test-group-09".equals( g.getId( ) ) ) );
        assertNull( groupHandler.get( "test-group-09" ) );

    }


    @Test
    void validateRepository( ) throws IOException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        EditableRepositoryGroup repositoryGroup = createRepository( "test-group-10", "n-test-group-10" );
        repositoryGroup.setMergedIndexTTL( 5 );
        CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.validateRepository( repositoryGroup );
        assertNotNull( result );
        assertEquals( 0, result.getResult( ).size( ) );

        repositoryGroup = createRepository( "test-group-10", "n-test-group-10###" );
        result = groupHandler.validateRepository( repositoryGroup );
        assertNotNull( result );
        assertEquals( 2, result.getResult( ).size( ) );
        assertNotNull( result.getResult().get( "merged_index_ttl" ) );
        assertNotNull( result.getResult().get( "name" ) );

    }

    @Test
    void validateRepositoryIfExisting( ) throws IOException, RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        EditableRepositoryGroup repositoryGroup = createRepository( "test-group-11", "n-test-group-11" );
        repositoryGroup.setMergedIndexTTL( 5 );
        groupHandler.put( repositoryGroup );
        CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.validateRepository( repositoryGroup );
        assertNotNull( result );
        assertEquals( 1, result.getResult( ).size( ) );


    }

    @Test
    void validateRepositoryForUpdate( ) throws IOException, RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        EditableRepositoryGroup repositoryGroup = createRepository( "test-group-12", "n-test-group-12" );
        repositoryGroup.setMergedIndexTTL( 5 );
        groupHandler.put( repositoryGroup );
        CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.validateRepositoryForUpdate( repositoryGroup );
        assertNotNull( result );
        assertEquals( 0, result.getResult( ).size( ) );

    }

    @Test
    void has( ) throws IOException, RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        EditableRepositoryGroup repositoryGroup = createRepository( "test-group-13", "n-test-group-13" );
        repositoryGroup.setMergedIndexTTL( 5 );
        assertFalse( groupHandler.hasRepository( "test-group-13" ) );
        groupHandler.put( repositoryGroup );
        assertTrue( groupHandler.hasRepository( "test-group-13" ) );
    }

}