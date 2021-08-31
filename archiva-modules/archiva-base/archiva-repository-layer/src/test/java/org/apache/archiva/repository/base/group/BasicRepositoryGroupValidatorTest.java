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

import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.base.managed.BasicManagedRepository;
import org.apache.archiva.repository.base.RepositoryHandlerDependencies;
import org.apache.archiva.repository.mock.ManagedRepositoryContentMock;
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
class BasicRepositoryGroupValidatorTest
{

    @Inject
    ConfigurationHandler configurationHandler;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @SuppressWarnings( "unused" )
    @Inject
    RepositoryGroupHandler repositoryGroupHandler;

    @SuppressWarnings( "unused" )
    @Inject
    RepositoryHandlerDependencies repositoryHandlerDependencies;

    Path repoBaseDir;

    @AfterEach
    void cleanup() throws RepositoryException
    {
        repositoryRegistry.removeRepository("test");
    }

    protected EditableManagedRepository createRepository( String id, String name, Path location ) throws IOException
    {
        BasicManagedRepository repo = BasicManagedRepository.newFilesystemInstance(id, name, location);
        repo.setLocation( location.toAbsolutePath().toUri());
        repo.setContent(new ManagedRepositoryContentMock());
        return repo;
    }

    protected EditableRepositoryGroup createGroup(String id, String name) throws IOException
    {
        return BasicRepositoryGroup.newFilesystemInstance( id, name, getRepoBaseDir( ).resolve( id ) );
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
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        EditableRepositoryGroup group = createGroup( "test", "test" );
        group.setMergedIndexTTL( 360 );
        ValidationResponse<RepositoryGroup> result = validator.apply( group );
        assertNotNull( result );
        assertTrue( result.isValid( ) );
    }

    @Test
    void applyWithExisting( ) throws IOException, RepositoryException
    {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        EditableRepositoryGroup group = createGroup( "test", "test" );
        group.setMergedIndexTTL( 360 );
        repositoryRegistry.putRepositoryGroup( group );
        EditableRepositoryGroup group2 = createGroup( "test", "test2" );
        group2.setMergedIndexTTL( 360 );
        ValidationResponse<RepositoryGroup> result = validator.apply( group2 );
        assertNotNull( result );
        assertFalse( result.isValid( ) );
        assertEquals( "group_exists", result.getResult( ).get( "id" ).get( 0 ).getType( ) );
    }

    @Test
    void applyWithBadTTL( ) throws IOException
    {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        EditableRepositoryGroup group = createGroup( "test", "test" );
        group.setMergedIndexTTL( 0 );
        ValidationResponse<RepositoryGroup> result = validator.apply( group );
        assertNotNull( result );
        assertFalse( result.isValid( ) );
        assertTrue( result.getResult( ).containsKey( "merged_index_ttl" ) );
        assertEquals( "repository_group", result.getResult( ).get( "merged_index_ttl" ).get( 0 ).getCategory( ) );
        assertEquals( "min", result.getResult( ).get( "merged_index_ttl" ).get( 0 ).getType( ) );
        assertEquals( "merged_index_ttl", result.getResult( ).get( "merged_index_ttl" ).get( 0 ).getAttribute() );
    }

    @Test
    void applyWithNullObject( ) throws IOException
    {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        EditableRepositoryGroup group = createGroup( "", "test" );
        group.setMergedIndexTTL( 0 );
        ValidationResponse<RepositoryGroup> result = validator.apply( null );
        assertNotNull( result );
        assertFalse( result.isValid( ) );
        assertTrue( result.getResult( ).containsKey( "object" ) );
        assertEquals( "repository_group", result.getResult( ).get( "object" ).get( 0 ).getCategory( ) );
        assertEquals( "isnull", result.getResult( ).get( "object" ).get( 0 ).getType( ) );
        assertEquals( "object", result.getResult( ).get( "object" ).get( 0 ).getAttribute() );
    }

    @Test
    void applyWithEmptyId( ) throws IOException
    {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        EditableRepositoryGroup group = createGroup( "", "test" );
        group.setMergedIndexTTL( 0 );
        ValidationResponse<RepositoryGroup> result = validator.apply( group );
        assertNotNull( result );
        assertFalse( result.isValid( ) );
        assertTrue( result.getResult( ).containsKey( "id" ) );
        assertEquals( "repository_group", result.getResult( ).get( "id" ).get( 0 ).getCategory( ) );
        assertEquals( "empty", result.getResult( ).get( "id" ).get( 0 ).getType( ) );
        assertEquals( "id", result.getResult( ).get( "id" ).get( 0 ).getAttribute() );
    }

    @Test
    void applyWithBadName( ) throws IOException
    {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        validator.setRepositoryRegistry( repositoryRegistry );
        EditableRepositoryGroup group = createGroup( "test", "badtest\\name" );
        group.setMergedIndexTTL( 360);
        ValidationResponse<RepositoryGroup> result = validator.apply( group );
        assertFalse( result.isValid( ) );
        assertEquals( 1, result.getResult( ).size( ) );
        assertEquals( "invalid_chars", result.getResult( ).get( "name" ).get( 0 ).getType( ) );
    }

    @Test
    void getFlavour( )
    {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        assertEquals( RepositoryGroup.class, validator.getFlavour( ) );
    }

    @Test
    void isFlavour() {
        BasicRepositoryGroupValidator validator = new BasicRepositoryGroupValidator( configurationHandler );
        assertTrue( validator.isFlavour( RepositoryGroup.class ) );
        assertFalse( validator.isFlavour( ManagedRepository.class ) );
        assertTrue( validator.isFlavour( BasicRepositoryGroup.class ) );
    }
}