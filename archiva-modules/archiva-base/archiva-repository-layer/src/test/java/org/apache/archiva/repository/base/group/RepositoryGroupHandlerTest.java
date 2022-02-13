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
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.model.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.model.RepositoryGroupConfiguration;
import org.apache.archiva.indexer.merger.MergedRemoteIndexesScheduler;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.base.managed.BasicManagedRepository;
import org.apache.archiva.repository.base.remote.BasicRemoteRepository;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
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
    static {
        initialize();
    }

    @Inject
    @Named( "repositoryRegistry" )
    ArchivaRepositoryRegistry repositoryRegistry;

    @Inject
    ConfigurationHandler configurationHandler;

    @Mock
    // @Named( "mergedRemoteIndexesScheduler#default" )
    MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler;

    @Mock
    RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration> managedRepositoryHandler;

    @Mock
    RepositoryHandler<RemoteRepository, RemoteRepositoryConfiguration> remoteRepositoryHandler;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    Path repoBaseDir;


    private static void initialize() {
        Path baseDir = Paths.get( FileUtils.getBasedir( ) );
        Path config = baseDir.resolve( "src/test/resources/archiva-group.xml" );
        if ( Files.exists( config ) )
        {
            Path destConfig = baseDir.resolve( "target/test-classes/archiva-group.xml" );
            try
            {
                Files.copy( config, destConfig );
            }
            catch ( IOException e )
            {
                System.err.println( "Could not copy file: " + e.getMessage( ) );
            }
        }
    }

    // Helper method that removes a group from the configuration
    private void removeGroupFromConfig(String groupId) {
        Configuration configuration = configurationHandler.getBaseConfiguration( );
        Iterator<RepositoryGroupConfiguration> groupIter = configuration.getRepositoryGroups().iterator();
        while(groupIter.hasNext()) {
            RepositoryGroupConfiguration group = groupIter.next( );
            if (groupId.equals(group.getId())) {
                groupIter.remove();
                break;
            }
        }
        try
        {
            configurationHandler.save( configuration );
        }
        catch ( Throwable e )
        {
            System.err.println( "Could not remove repo group from config "+groupId );
        }
    }

    private boolean hasGroupInConfig(String groupId) {
        assertNotNull( configurationHandler.getBaseConfiguration( ).getRepositoryGroups( ) );
        return configurationHandler.getBaseConfiguration( ).getRepositoryGroups( ).stream( ).anyMatch( g -> g != null && groupId.equals( g.getId( ) ) ) ;
    }


    private RepositoryGroupHandler createHandler( )
    {
        Mockito.when( managedRepositoryHandler.getFlavour( ) ).thenReturn( ManagedRepository.class );
        final ManagedRepository internalRepo;
        try
        {
            internalRepo = getManaged( "internal", "internal");
        }
        catch ( IOException e )
        {
            throw new Error( e );
        }
        Mockito.when( managedRepositoryHandler.get( ArgumentMatchers.eq("internal") ) ).thenReturn( internalRepo );
        repositoryRegistry.registerHandler( managedRepositoryHandler );

        Mockito.when( remoteRepositoryHandler.getFlavour( ) ).thenReturn( RemoteRepository.class );
        final RemoteRepository centralRepo;
        try
        {
            centralRepo = getRemote( "central", "central");
        }
        catch ( IOException e )
        {
            throw new Error( e );
        }
        repositoryRegistry.registerHandler( remoteRepositoryHandler );


        RepositoryGroupHandler groupHandler = new RepositoryGroupHandler( repositoryRegistry, configurationHandler, mergedRemoteIndexesScheduler);
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

    protected ManagedRepository getManaged(String id, String name) throws IOException
    {
        Path path = getRepoBaseDir().resolve( "../managed" );
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(path.toAbsolutePath(), lockManager);
        return new BasicManagedRepository( id, name, storage );
    }

    protected RemoteRepository getRemote(String id, String name) throws IOException
    {
        Path path = getRepoBaseDir().resolve( "../remote" );
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(path.toAbsolutePath(), lockManager);
        return new BasicRemoteRepository( id, name, storage );
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
        String id = "test-group-02";
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroup repo = groupHandler.newInstance( RepositoryType.MAVEN, id );
        groupHandler.activateRepository( repo );
        verify( mergedRemoteIndexesScheduler ).schedule( eq( repo ), any( ) );
        assertEquals( RepositoryState.INITIALIZED, repo.getLastState( ) );
        assertFalse(hasGroupInConfig( id ));
    }

    @Test
    void newInstancesFromConfig( )
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        Map<String, RepositoryGroup> instances = groupHandler.newInstancesFromConfig( );
        assertTrue( instances.containsKey( "test-group-01" ) );
        assertEquals( RepositoryState.REFERENCES_SET, instances.get( "test-group-01" ).getLastState( ) );
    }

    @Test
    void newInstance( ) throws RepositoryException
    {
        String id = "test-group-03";
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroup instance = groupHandler.newInstance( RepositoryType.MAVEN, id );
        assertNotNull( instance );
        assertEquals( id, instance.getId( ) );
        assertFalse( groupHandler.hasRepository( id ) );
        assertEquals( RepositoryState.REFERENCES_SET, instance.getLastState( ) );
        assertFalse( hasGroupInConfig( id ) );
    }


    @Test
    void put( ) throws IOException, RepositoryException
    {
        final String id = "test-group-04";
        try
        {
            RepositoryGroupHandler groupHandler = createHandler( );
            EditableRepositoryGroup repositoryGroup = createRepository( id, "n-"+id );
            groupHandler.put( repositoryGroup );
            RepositoryGroup storedGroup = groupHandler.get( id );
            assertNotNull( storedGroup );
            assertEquals( id, storedGroup.getId( ) );
            assertEquals( "n-"+id, storedGroup.getName( ) );

            EditableRepositoryGroup repositoryGroup2 = createRepository( id, "n2-"+id );
            groupHandler.put( repositoryGroup2 );
            storedGroup = groupHandler.get( id );
            assertNotNull( storedGroup );
            assertEquals( id, storedGroup.getId( ) );
            assertEquals( "n2-"+id, storedGroup.getName( ) );

            assertTrue( hasGroupInConfig( id ));
        } finally {
            removeGroupFromConfig( id );
        }
    }

    @Test
    void putWithConfiguration( ) throws RepositoryException
    {
        String id = "test-group-05";

        try
        {
            RepositoryGroupHandler groupHandler = createHandler( );
            RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
            configuration.setId( id );
            configuration.setName( "n-" + id );
            ArrayList<String> repos = new ArrayList<>( );
            repos.add( "internal" );
            configuration.setRepositories( repos );
            groupHandler.put( configuration );

            RepositoryGroup repo = groupHandler.get( id );
            assertNotNull( repo );
            assertEquals( id, repo.getId( ) );
            assertEquals( "n-" + id, repo.getName( ) );

            assertNotNull( repo.getRepositories( ) );
            assertEquals( 1, repo.getRepositories( ).size( ) );
            assertEquals( "internal", repo.getRepositories( ).get( 0 ).getId( ) );
            assertTrue( hasGroupInConfig( id ) );
        }
        finally
        {
            removeGroupFromConfig( id );
        }
    }

    @Test
    void testPutWithoutRegister( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        Configuration aCfg = new Configuration( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        final String id = "test-group-06";
        configuration.setId( id );
        configuration.setName( "n-"+id );
        ArrayList<String> repos = new ArrayList<>( );
        repos.add( "internal" );
        configuration.setRepositories( repos );
        groupHandler.put( configuration, aCfg );

        RepositoryGroup repo = groupHandler.get( id );
        assertNull( repo );
        assertFalse( hasGroupInConfig( id ) );
        assertTrue( aCfg.getRepositoryGroups( ).stream( ).anyMatch( g -> g!=null && id.equals( g.getId( ) ) ) );

    }

    @Test
    void putWithCheck_invalid( ) throws RepositoryException
    {
        final String id = "test-group-07";
        final String name = "n-"+id;
        try
        {
            RepositoryGroupHandler groupHandler = createHandler( );
            BasicRepositoryGroupValidator checker = new BasicRepositoryGroupValidator( configurationHandler );
            RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
            configuration.setId( "" );
            configuration.setName( name );
            ArrayList<String> repos = new ArrayList<>( );
            repos.add( "internal" );
            configuration.setRepositories( repos );
            CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.putWithCheck( configuration, checker );
            assertNull( groupHandler.get( id ) );
            assertNotNull( result.getResult( ) );
            assertNotNull( result.getResult( ).get( "id" ) );
            assertEquals( 1, result.getResult( ).get( "id" ).size( ) );
            assertEquals( ISEMPTY, result.getResult( ).get( "id" ).get( 0 ).getType( ) );
            assertFalse( hasGroupInConfig( id ) );
            assertFalse( hasGroupInConfig( "" ) );
        } finally
        {
            removeGroupFromConfig( id );
        }
    }

    @Test
    void remove( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        final String id = "test-group-08";
        configuration.setId( id );
        configuration.setName( "n-"+id );
        groupHandler.put( configuration );
        assertTrue( hasGroupInConfig( id ) );
        assertNotNull( groupHandler.get( id ) );
        groupHandler.remove( id );
        assertNull( groupHandler.get( id ) );
        assertFalse( hasGroupInConfig( id ) );
    }

    @Test
    void removeWithoutSave( ) throws RepositoryException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        Configuration aCfg = new Configuration( );
        RepositoryGroupConfiguration configuration = new RepositoryGroupConfiguration( );
        final String id = "test-group-09";
        configuration.setId( id );
        configuration.setName( "n-"+id );
        ArrayList<String> repos = new ArrayList<>( );
        repos.add( "internal" );
        configuration.setRepositories( repos );
        groupHandler.put( configuration, aCfg );
        assertTrue( aCfg.getRepositoryGroups( ).stream( ).anyMatch( g -> g != null && id.equals( g.getId( ) ) ) );
        groupHandler.remove( id, aCfg );
        assertNull( groupHandler.get( id ) );
        assertTrue( aCfg.getRepositoryGroups( ).stream( ).noneMatch( g -> g != null && id.equals( g.getId( ) ) ) );
        assertNull( groupHandler.get( id ) );

    }


    @Test
    void validateRepository( ) throws IOException
    {
        RepositoryGroupHandler groupHandler = createHandler( );
        final String id = "test-group-10";
        EditableRepositoryGroup repositoryGroup = createRepository( id, "n-"+id );
        repositoryGroup.setMergedIndexTTL( 5 );
        CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.validateRepository( repositoryGroup );
        assertNotNull( result );
        assertEquals( 0, result.getResult( ).size( ) );

        repositoryGroup = createRepository( id, "n-test-group-10###" );
        result = groupHandler.validateRepository( repositoryGroup );
        assertNotNull( result );
        assertEquals( 2, result.getResult( ).size( ) );
        assertNotNull( result.getResult().get( "merged_index_ttl" ) );
        assertNotNull( result.getResult().get( "name" ) );

    }

    @Test
    void validateRepositoryIfExisting( ) throws IOException, RepositoryException
    {
        final String id = "test-group-11";
        try
        {
            RepositoryGroupHandler groupHandler = createHandler( );
            EditableRepositoryGroup repositoryGroup = createRepository( id, "n-" + id );
            repositoryGroup.setMergedIndexTTL( 5 );
            groupHandler.put( repositoryGroup );
            CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.validateRepository( repositoryGroup );
            assertNotNull( result );
            assertEquals( 1, result.getResult( ).size( ) );
        } finally
        {
            removeGroupFromConfig( id );
        }

    }

    @Test
    void validateRepositoryForUpdate( ) throws IOException, RepositoryException
    {
        final String id = "test-group-12";
        try
        {
            RepositoryGroupHandler groupHandler = createHandler( );
            EditableRepositoryGroup repositoryGroup = createRepository( id, "n-" + id );
            repositoryGroup.setMergedIndexTTL( 5 );
            groupHandler.put( repositoryGroup );
            CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> result = groupHandler.validateRepositoryForUpdate( repositoryGroup );
            assertNotNull( result );
            assertEquals( 0, result.getResult( ).size( ) );
        } finally
        {
            removeGroupFromConfig( id );
        }
    }

    @Test
    void has( ) throws IOException, RepositoryException
    {
        final String id = "test-group-13";
        try
        {
            RepositoryGroupHandler groupHandler = createHandler( );
            EditableRepositoryGroup repositoryGroup = createRepository( id, "n-" + id );
            repositoryGroup.setMergedIndexTTL( 5 );
            assertFalse( groupHandler.hasRepository( id ) );
            groupHandler.put( repositoryGroup );
            assertTrue( groupHandler.hasRepository( id ) );
        } finally
        {
            removeGroupFromConfig( id );
        }
    }

}