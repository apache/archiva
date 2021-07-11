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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.indexer.merger.MergedRemoteIndexesScheduler;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Map;

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


    private RepositoryGroupHandler createHandler( )
    {
        RepositoryGroupHandler groupHandler = new RepositoryGroupHandler( repositoryRegistry, configurationHandler, mergedRemoteIndexesScheduler, repositoryValidatorList );
        groupHandler.init( );
        return groupHandler;
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
    void put( )
    {
    }

    @Test
    void testPut( )
    {
    }

    @Test
    void testPut1( )
    {
    }

    @Test
    void putWithCheck( )
    {
    }

    @Test
    void remove( )
    {
    }

    @Test
    void testRemove( )
    {
    }

    @Test
    void get( )
    {
    }

    @Test
    void testClone( )
    {
    }

    @Test
    void updateReferences( )
    {
    }

    @Test
    void getAll( )
    {
    }

    @Test
    void getValidator( )
    {
    }

    @Test
    void validateRepository( )
    {
    }

    @Test
    void validateRepositoryForUpdate( )
    {
    }

    @Test
    void has( )
    {
    }

    @Test
    void close( )
    {
    }
}