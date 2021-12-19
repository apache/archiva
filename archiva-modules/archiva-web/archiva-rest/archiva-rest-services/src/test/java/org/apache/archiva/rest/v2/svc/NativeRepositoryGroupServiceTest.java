package org.apache.archiva.rest.v2.svc;

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

import io.restassured.response.Response;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.rest.api.v2.model.RepositoryGroup;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestError;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag( "rest-native" )
@TestMethodOrder( MethodOrderer.Random.class )
@DisplayName( "Native REST tests for V2 RepositoryGroupService" )
public class NativeRepositoryGroupServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/repository_groups";
    }

    @BeforeAll
    void setup( ) throws Exception
    {
        super.setupNative( );
    }

    @AfterAll
    void destroy( ) throws Exception
    {
        super.shutdownNative( );
    }

    @Test
    void testGetEmptyList( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        PagedResult result = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
        assertEquals( 0, result.getPagination( ).getTotalCount( ) );

    }

    @Test
    void testAddGroup( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "group_001" );
            jsonAsMap.put( "name", "group_001" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .post( "" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 1, resultList.getPagination( ).getTotalCount( ) );
        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void testAddExistingGroup( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "group_001" );
            jsonAsMap.put( "name", "group_001" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .post( "" )
                .prettyPeek()
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .redirects().follow( false )
                .body( jsonAsMap )
                .post( "" )
                .prettyPeek()
                .then( ).statusCode( 303 )
                .assertThat()
                .header( "Location", endsWith("group_001") ).extract( ).response( );
        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" );
        }
    }


    @Test
    void testAddMultipleGroups( )
    {
        String token = getAdminToken( );
        List<String> groups = new ArrayList<>( );
        try
        {
            for ( int i=0; i<10; i++)
            {
                String groupName = String.format( "group_%03d", i );
                groups.add( groupName );
                Map<String, Object> jsonAsMap = new HashMap<>( );
                jsonAsMap.put( "id", groupName );
                jsonAsMap.put( "name", groupName );
                Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( )
                    .body( jsonAsMap )
                    .post( "" )
                    .then( ).statusCode( 201 ).extract( ).response( );
                assertNotNull( response );
                RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
                assertNotNull( result );
            }
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 10, resultList.getPagination( ).getTotalCount( ) );
        } finally
        {
            for (String groupName : groups)
            {
                given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( )
                    .delete( groupName );
            }
        }
    }

    @Test
    void testRemoveRepositoryGroup( )
    {
        String token = getAdminToken( );
        List<String> groups = new ArrayList<>( );
        try
        {
            for ( int i=0; i<10; i++)
            {
                String groupName = String.format( "group_%03d", i );
                groups.add( groupName );
                Map<String, Object> jsonAsMap = new HashMap<>( );
                jsonAsMap.put( "id", groupName );
                jsonAsMap.put( "name", groupName );
                Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( )
                    .body( jsonAsMap )
                    .post( "" )
                    .then( ).statusCode( 201 ).extract( ).response( );
                assertNotNull( response );
                RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
                assertNotNull( result );
            }
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 9, resultList.getPagination( ).getTotalCount( ) );


            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_005" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 8, resultList.getPagination( ).getTotalCount( ) );

        } finally
        {
            for (String groupName : groups)
            {
                if (!("group_001".equals(groupName) || "group_005".equals(groupName) ) )
                {
                    given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                        .when( )
                        .delete( groupName );
                }
            }
        }
    }


    @Test
    void testAddRepositoryToGroup( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "group_001" );
            jsonAsMap.put( "name", "group_001" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .post( "" )
                .prettyPeek()
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 1, resultList.getPagination( ).getTotalCount( ) );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .put( "group_001/repositories/internal" )
                .prettyPeek()
                .then( ).statusCode( 200 ).extract( ).response( );

            assertNotNull( response );
            result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );
            assertEquals( 1, result.getRepositories( ).size( ) );
            assertTrue( result.getRepositories( ).contains( "internal" ) );

        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" );
        }
    }

    @Test
    void testAddRepositoryToGroupIdempotency( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "group_001" );
            jsonAsMap.put( "name", "group_001" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .post( "" )
                .prettyPeek()
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 1, resultList.getPagination( ).getTotalCount( ) );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .put( "group_001/repositories/internal" )
                .prettyPeek()
                .then( ).statusCode( 200 ).extract( ).response( );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .put( "group_001/repositories/internal" )
                .prettyPeek()
                .then( ).statusCode( 200 ).extract( ).response( );

            assertNotNull( response );
            result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );
            assertEquals( 1, result.getRepositories( ).size( ) );
            assertTrue( result.getRepositories( ).contains( "internal" ) );

        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" )
                .then( ).statusCode( 200 );
        }
    }


    @Test
    void testRemoveRepositoryFromGroup( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "group_001" );
            jsonAsMap.put( "name", "group_001" );
            jsonAsMap.put( "repositories", Arrays.asList( "internal" ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .post( "" )
                .prettyPeek()
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 1, resultList.getPagination( ).getTotalCount( ) );

            assertNotNull( result.getRepositories( ) );
            assertEquals( 1, result.getRepositories( ).size( ) );
            assertTrue( result.getRepositories( ).contains( "internal" ) );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .delete( "group_001/repositories/internal" )
                .prettyPeek()
                .then( ).statusCode( 200 ).extract( ).response( );

            assertNotNull( response );
            result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );
            assertEquals( 0, result.getRepositories( ).size( ) );

        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void testRemoveRepositoryFromGroup404( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "group_001" );
            jsonAsMap.put( "name", "group_001" );
            jsonAsMap.put( "repositories", Arrays.asList( "internal" ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .post( "" )
                .prettyPeek()
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RepositoryGroup result = response.getBody( ).jsonPath( ).getObject( "", RepositoryGroup.class );
            assertNotNull( result );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            PagedResult resultList = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertEquals( 1, resultList.getPagination( ).getTotalCount( ) );

            assertNotNull( result.getRepositories( ) );
            assertEquals( 1, result.getRepositories( ).size( ) );
            assertTrue( result.getRepositories( ).contains( "internal" ) );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .delete( "group_001/repositories/internalxx" )
                .prettyPeek()
                .then( ).statusCode( 404 ).extract( ).response( );

            assertNotNull( response );
            ArchivaRestError error = response.getBody( ).jsonPath( ).getObject( "", ArchivaRestError.class );
            assertNotNull( error );
        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "group_001" );
        }
    }

}
