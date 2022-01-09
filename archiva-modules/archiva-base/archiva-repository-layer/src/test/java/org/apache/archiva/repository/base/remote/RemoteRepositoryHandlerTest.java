package org.apache.archiva.repository.base.remote;

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
import org.apache.archiva.configuration.model.RemoteRepositoryConfiguration;
import org.apache.archiva.indexer.IndexManagerFactory;
import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.archiva.repository.validation.ErrorKeys.ISEMPTY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( {MockitoExtension.class, SpringExtension.class} )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-remote.xml"} )
class RemoteRepositoryHandlerTest
{
    static {
        initialize();
    }

    @Inject
    @Named( "repositoryRegistry" )
    ArchivaRepositoryRegistry repositoryRegistry;

    @Inject
    @Named( "repositoryContentFactory#default" )
    RepositoryContentFactory repositoryContentFactory;

    @Inject
    IndexManagerFactory indexManagerFactory;

    @Inject
    ConfigurationHandler configurationHandler;

    @SuppressWarnings( "unused" )
    @Inject
    List<RepositoryValidator<? extends Repository>> repositoryValidatorList;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    Path repoBaseDir;


    private static void initialize() {
        Path baseDir = Paths.get( FileUtils.getBasedir( ) );
        Path config = baseDir.resolve( "src/test/resources/archiva-remote.xml" );
        if ( Files.exists( config ) )
        {
            Path destConfig = baseDir.resolve( "target/test-classes/archiva-remote.xml" );
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

    // Helper method that removes a repo from the configuration
    private void removeRepositoryFromConfig( String id) {
        Configuration configuration = configurationHandler.getBaseConfiguration( );
        Iterator<RemoteRepositoryConfiguration> iter = configuration.getRemoteRepositories().iterator();
        while(iter.hasNext()) {
            RemoteRepositoryConfiguration repo = iter.next( );
            if (id.equals(repo.getId())) {
                iter.remove();
                break;
            }
        }
        try
        {
            configurationHandler.save( configuration );
        }
        catch ( Throwable e )
        {
            System.err.println( "Could not remove repo from config "+id );
        }
    }

    private boolean hasRepositoryInConfig( String id) {
        assertNotNull( configurationHandler.getBaseConfiguration( ).getRemoteRepositories() );
        return configurationHandler.getBaseConfiguration( ).getRemoteRepositories( ).stream( ).anyMatch( g -> g != null && id.equals( g.getId( ) ) ) ;
    }


    private RemoteRepositoryHandler createHandler( )
    {
        RemoteRepositoryHandler repositoryHandler = new RemoteRepositoryHandler( repositoryRegistry, configurationHandler, indexManagerFactory, repositoryContentFactory );
        repositoryHandler.init( );
        return repositoryHandler;
    }

    private Path getRepoBaseDir() throws IOException
    {
        if (repoBaseDir==null) {
            this.repoBaseDir = archivaConfiguration.getRepositoryBaseDir( ).resolve( "remote" );
            Files.createDirectories( this.repoBaseDir );
        }
        return repoBaseDir;
    }

    protected EditableRemoteRepository createRepository( String id, String name, Path location ) throws IOException
    {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(location.toAbsolutePath(), lockManager);
        BasicRemoteRepository repo = new BasicRemoteRepository(id, name, storage);
        repo.setLocation( location.toAbsolutePath().toUri());
        return repo;
    }

    protected EditableRemoteRepository createRepository( String id, String name) throws IOException
    {
        Path dir = getRepoBaseDir( ).resolve( id );
        Files.createDirectories( dir );
        return createRepository( id, name, dir );
    }




    @Test
    void initializeFromConfig( )
    {
        RemoteRepositoryHandler repoHandler = createHandler( );
        repoHandler.initializeFromConfig( );
        assertEquals( 2, repoHandler.getAll( ).size( ) );
        assertNotNull( repoHandler.get( "test-repo-01" ) );
        assertEquals( "Test Remote Repository", repoHandler.get( "test-repo-01" ).getName() );
    }

    @Test
    void activateRepository( ) throws RepositoryException
    {
        String id = "test-repo-02";
        RemoteRepositoryHandler repoHandler = createHandler( );
        RemoteRepository repo = repoHandler.newInstance( RepositoryType.MAVEN, id );
        repoHandler.activateRepository( repo );
        assertFalse( hasRepositoryInConfig( id ));
        assertNull( repoHandler.get( id ) );
    }

    @Test
    void newInstancesFromConfig( ) throws RepositoryException
    {
        final String id = "test-repo-01";
        RemoteRepositoryHandler repoHandler = createHandler( );
        Configuration configuration = new Configuration( );
        repoHandler.remove( "test-repo-01", configuration );
        Map<String, RemoteRepository> instances = repoHandler.newInstancesFromConfig( );
        assertFalse( repoHandler.hasRepository( id ) );
        assertTrue( instances.containsKey( id ) );
        assertEquals( RepositoryState.REFERENCES_SET, instances.get( id ).getLastState( ) );
    }

    @Test
    void newInstance( ) throws RepositoryException
    {
        String id = "test-repo-03";
        RemoteRepositoryHandler repoHandler = createHandler( );
        RemoteRepository instance = repoHandler.newInstance( RepositoryType.MAVEN, id );
        assertNotNull( instance );
        assertEquals( id, instance.getId( ) );
        assertFalse( repoHandler.hasRepository( id ) );
        assertEquals( RepositoryState.REFERENCES_SET, instance.getLastState( ) );
        assertFalse( hasRepositoryInConfig( id ) );
    }


    @Test
    void put( ) throws IOException, RepositoryException
    {
        final String id = "test-repo-04";
        try
        {
            RemoteRepositoryHandler repoHandler = createHandler( );
            EditableRemoteRepository repository = createRepository( id, "n-"+id );
            repoHandler.put( repository );
            RemoteRepository storedRepository = repoHandler.get( id );
            assertNotNull( storedRepository );
            assertEquals( id, storedRepository.getId( ) );
            assertEquals( "n-"+id, storedRepository.getName( ) );

            EditableRemoteRepository repository2 = createRepository( id, "n2-"+id );
            repoHandler.put( repository2 );
            storedRepository = repoHandler.get( id );
            assertNotNull( storedRepository );
            assertEquals( id, storedRepository.getId( ) );
            assertEquals( "n2-"+id, storedRepository.getName( ) );

            assertTrue( hasRepositoryInConfig( id ));
        } finally {
            removeRepositoryFromConfig( id );
        }
    }

    @Test
    void putWithConfiguration( ) throws RepositoryException
    {
        String id = "test-repo-05";

        try
        {
            RemoteRepositoryHandler repoHandler = createHandler( );
            RemoteRepositoryConfiguration configuration = new RemoteRepositoryConfiguration( );
            configuration.setId( id );
            configuration.setName( "n-" + id );
            repoHandler.put( configuration );

            RemoteRepository repo = repoHandler.get( id );
            assertNotNull( repo );
            assertEquals( id, repo.getId( ) );
            assertEquals( "n-" + id, repo.getName( ) );
            assertTrue( hasRepositoryInConfig( id ) );
        }
        finally
        {
            removeRepositoryFromConfig( id );
        }
    }

    @Test
    void testPutWithoutRegister( ) throws RepositoryException
    {
        final String id = "test-repo-06";
        RemoteRepositoryHandler repoHandler = createHandler( );
        Configuration aCfg = new Configuration( );
        RemoteRepositoryConfiguration configuration = new RemoteRepositoryConfiguration( );
        configuration.setId( id );
        configuration.setName( "n-"+id );
        repoHandler.put( configuration, aCfg );

        RemoteRepository repo = repoHandler.get( id );
        assertNull( repo );
        assertFalse( hasRepositoryInConfig( id ) );
        assertTrue( aCfg.getRemoteRepositories( ).stream( ).anyMatch( g -> g!=null && id.equals( g.getId( ) ) ) );

    }

    @Test
    void putWithCheck_invalid( ) throws RepositoryException
    {
        final String id = "test-repo-07";
        final String name = "n-"+id;
        try
        {
            RemoteRepositoryHandler repoHandler = createHandler( );
            BasicRemoteRepositoryValidator checker = new BasicRemoteRepositoryValidator( configurationHandler );
            checker.setRepositoryRegistry( repositoryRegistry );
            RemoteRepositoryConfiguration configuration = new RemoteRepositoryConfiguration();
            configuration.setId( "" );
            configuration.setName( name );
            CheckedResult<RemoteRepository, Map<String, List<ValidationError>>> result = repoHandler.putWithCheck( configuration, checker );
            assertNull( repoHandler.get( id ) );
            assertNotNull( result.getResult( ) );
            assertNotNull( result.getResult( ).get( "id" ) );
            assertEquals( 2, result.getResult( ).get( "id" ).size( ) );
            assertEquals( ISEMPTY, result.getResult( ).get( "id" ).get( 0 ).getType( ) );
            assertFalse( hasRepositoryInConfig( id ) );
            assertFalse( hasRepositoryInConfig( "" ) );
        } finally
        {
            removeRepositoryFromConfig( id );
        }
    }

    @Test
    void remove( ) throws RepositoryException
    {
        final String id = "test-repo-08";
        RemoteRepositoryHandler repoHandler = createHandler( );
        RemoteRepositoryConfiguration configuration = new RemoteRepositoryConfiguration( );
        configuration.setId( id );
        configuration.setName( "n-"+id );
        repoHandler.put( configuration );
        assertTrue( hasRepositoryInConfig( id ) );
        assertNotNull( repoHandler.get( id ) );
        repoHandler.remove( id );
        assertNull( repoHandler.get( id ) );
        assertFalse( hasRepositoryInConfig( id ) );
    }

    @Test
    void removeWithoutSave( ) throws RepositoryException
    {
        final String id = "test-repo-09";
        RemoteRepositoryHandler repoHandler = createHandler( );
        Configuration aCfg = new Configuration( );
        RemoteRepositoryConfiguration configuration = new RemoteRepositoryConfiguration( );
        configuration.setId( id );
        configuration.setName( "n-"+id );
        repoHandler.put( configuration, aCfg );
        assertTrue( aCfg.getRemoteRepositories( ).stream( ).anyMatch( g -> g != null && id.equals( g.getId( ) ) ) );
        repoHandler.remove( id, aCfg );
        assertNull( repoHandler.get( id ) );
        assertTrue( aCfg.getRemoteRepositories( ).stream( ).noneMatch( g -> g != null && id.equals( g.getId( ) ) ) );
        assertNull( repoHandler.get( id ) );

    }


    @Test
    void validateRepository( ) throws IOException
    {
        final String id = "test-repo-10";
        RemoteRepositoryHandler repoHandler = createHandler( );
        EditableRemoteRepository repository = createRepository( id, "n-"+id );
        CheckedResult<RemoteRepository, Map<String, List<ValidationError>>> result = repoHandler.validateRepository( repository );
        assertNotNull( result );
        assertEquals( 0, result.getResult( ).size( ) );

        repository = createRepository( id, "n-test-repo-10###" );
        result = repoHandler.validateRepository( repository );
        assertNotNull( result );
        assertEquals( 1, result.getResult( ).size( ) );
        assertNotNull( result.getResult().get( "name" ) );

    }

    @Test
    void validateRepositoryIfExisting( ) throws IOException, RepositoryException
    {
        final String id = "test-repo-11";
        try
        {
            RemoteRepositoryHandler repoHandler = createHandler( );
            EditableRemoteRepository repository = createRepository( id, "n-" + id );
            repoHandler.put( repository );
            CheckedResult<RemoteRepository, Map<String, List<ValidationError>>> result = repoHandler.validateRepository( repository );
            assertNotNull( result );
            assertEquals( 1, result.getResult( ).size( ) );
        } finally
        {
            removeRepositoryFromConfig( id );
        }

    }

    @Test
    void validateRepositoryForUpdate( ) throws IOException, RepositoryException
    {
        final String id = "test-repo-12";
        try
        {
            RemoteRepositoryHandler repoHandler = createHandler( );
            EditableRemoteRepository repository = createRepository( id, "n-" + id );
            repoHandler.put( repository );
            CheckedResult<RemoteRepository, Map<String, List<ValidationError>>> result = repoHandler.validateRepositoryForUpdate( repository );
            assertNotNull( result );
            assertEquals( 0, result.getResult( ).size( ) );
        } finally
        {
            removeRepositoryFromConfig( id );
        }
    }

    @Test
    void has( ) throws IOException, RepositoryException
    {
        final String id = "test-repo-13";
        try
        {
            RemoteRepositoryHandler repoHandler = createHandler( );
            EditableRemoteRepository repository = createRepository( id, "n-" + id );
            assertFalse( repoHandler.hasRepository( id ) );
            repoHandler.put( repository );
            assertTrue( repoHandler.hasRepository( id ) );
        } finally
        {
            removeRepositoryFromConfig( id );
        }
    }

}