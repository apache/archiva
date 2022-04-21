package org.apache.archiva.repository.base.managed;

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
import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.base.managed.ManagedRepositoryHandler;
import org.apache.archiva.repository.base.remote.RemoteRepositoryHandler;
import org.apache.archiva.repository.base.group.RepositoryGroupHandler;
import org.apache.archiva.repository.mock.ManagedRepositoryContentMock;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( SpringExtension.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" })
class BasicManagedRepositoryValidatorTest
{

    @Inject
    ConfigurationHandler configurationHandler;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @SuppressWarnings( "unused" )
    @Inject
    RepositoryGroupHandler repositoryGroupHandler;

    @Inject
    ManagedRepositoryHandler managedRepositoryHandler;

    @Inject
    RemoteRepositoryHandler remoteRepositoryHandler;

    Path repoBaseDir;

    @AfterEach
    void cleanup() {
        try
        {
            repositoryRegistry.removeRepository( "test" );
        }
        catch ( RepositoryException e )
        {
            // Ignore this
        }

    }

    protected EditableManagedRepository createRepository( String id, String name, Path location ) throws IOException
    {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(location.toAbsolutePath(), lockManager);
        BasicManagedRepository repo = new BasicManagedRepository(id, name, storage);
        repo.setLocation( location.toAbsolutePath().toUri());
        repo.setContent(new ManagedRepositoryContentMock());
        return repo;
    }

    private Path getRepoBaseDir() {
        if (repoBaseDir==null) {
            try
            {
                repoBaseDir = Paths.get(Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories" ).toURI());
            }
            catch ( URISyntaxException e )
            {
                throw new RuntimeException( "Could not retrieve repository base directory" );
            }
        }
        return repoBaseDir;
    }


    @Test
    void apply( ) throws IOException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        Path repoDir = getRepoBaseDir().resolve("test" );
        EditableManagedRepository repo = createRepository( "test", "test", repoDir );
        ValidationResponse<ManagedRepository> result = validator.apply( repo );
        assertTrue( result.isValid( ) );
    }

    @Test
    void applyWithExistingRepo( ) throws IOException, RepositoryException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        Path repoDir = getRepoBaseDir().resolve("test" );
        EditableManagedRepository repo = createRepository( "test", "test", repoDir );
        Path repoDir2 = getRepoBaseDir().resolve("test2" );
        EditableManagedRepository repo2 = createRepository( "test", "test", repoDir2 );
        repositoryRegistry.putRepository( repo );
        ValidationResponse<ManagedRepository> result = validator.apply( repo );
        assertFalse( result.isValid( ) );
        assertEquals( 1, result.getResult( ).size( ) );
        assertTrue( result.getResult( ).containsKey( "id" ) );
        assertEquals( "managed_repository", result.getResult( ).get( "id" ).get( 0 ).getCategory( ) );
        assertEquals( "managed_repo_exists", result.getResult( ).get( "id" ).get( 0 ).getType( ) );
        assertEquals( "id", result.getResult( ).get( "id" ).get( 0 ).getAttribute() );
    }

    @Test
    void applyUpdateWithExistingRepo( ) throws IOException, RepositoryException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        Path repoDir = getRepoBaseDir().resolve("test" );
        EditableManagedRepository repo = createRepository( "test", "test", repoDir );
        Path repoDir2 = getRepoBaseDir().resolve("test2" );
        EditableManagedRepository repo2 = createRepository( "test", "test", repoDir2 );
        repositoryRegistry.putRepository( repo );
        ValidationResponse<ManagedRepository> result = validator.applyForUpdate( repo );
        assertTrue( result.isValid( ) );
        assertEquals( 0, result.getResult( ).size( ) );
    }

    @Test
    void applyWithNullObject( ) throws IOException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        ValidationResponse<ManagedRepository> result = validator.apply( null );
        assertFalse( result.isValid( ) );
        assertEquals( 1, result.getResult( ).size( ) );
        assertTrue( result.getResult( ).containsKey( "object" ) );
        assertEquals( "managed_repository", result.getResult( ).get( "object" ).get( 0 ).getCategory( ) );
        assertEquals( "isnull", result.getResult( ).get( "object" ).get( 0 ).getType( ) );
        assertEquals( "object", result.getResult( ).get( "object" ).get( 0 ).getAttribute() );
    }

    @Test
    void applyWithEmptyId( ) throws IOException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        Path repoDir = getRepoBaseDir().resolve("test" );
        EditableManagedRepository repo = createRepository( "", "test", repoDir );
        ValidationResponse<ManagedRepository> result = validator.apply( repo );
        assertFalse( result.isValid( ) );
        assertEquals( 1, result.getResult( ).size( ) );
        assertTrue( result.getResult( ).containsKey( "id" ) );
        assertEquals( "managed_repository", result.getResult( ).get( "id" ).get( 0 ).getCategory( ) );
        assertEquals( "empty", result.getResult( ).get( "id" ).get( 0 ).getType( ) );
        assertEquals( "id", result.getResult( ).get( "id" ).get( 0 ).getAttribute() );
    }

    @Test
    void applyWithBadName( ) throws IOException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        Path repoDir = getRepoBaseDir().resolve("test" );
        EditableManagedRepository repo = createRepository( "test", "badtest\\name", repoDir );
        ValidationResponse<ManagedRepository> result = validator.apply( repo );
        assertFalse( result.isValid( ) );
        assertEquals( 1, result.getResult( ).size( ) );
        assertEquals( "invalid_chars", result.getResult( ).get( "name" ).get( 0 ).getType( ) );
    }

    @Test
    void applyWithBadSchedulingExpression( ) throws IOException
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        Path repoDir = getRepoBaseDir().resolve("test" );
        EditableManagedRepository repo = createRepository( "test", "test", repoDir );
        repo.setSchedulingDefinition( "xxxxx" );
        ValidationResponse<ManagedRepository> result = validator.apply( repo );
        assertFalse( result.isValid( ) );
        assertEquals( 1, result.getResult( ).size( ) );
        assertEquals( "invalid_scheduling_exp", result.getResult( ).get( "scheduling_definition" ).get( 0 ).getType( ) );
    }

    @Test
    void applyForUpdate( )
    {
    }

    @Test
    void getFlavour( )
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        assertEquals( ManagedRepository.class, validator.getFlavour( ) );
    }

    @Test
    void isFlavour( )
    {
        BasicManagedRepositoryValidator validator = new BasicManagedRepositoryValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        assertTrue( validator.isFlavour( ManagedRepository.class ) );
        assertTrue( validator.isFlavour( BasicManagedRepository.class ) );
    }
}