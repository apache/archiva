package org.apache.archiva.rest.services.v2;

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
import org.apache.archiva.rest.api.model.v2.Repository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag( "rest-native" )
@TestMethodOrder( MethodOrderer.Random.class )
@DisplayName( "Native REST tests for V2 RepositoryService" )
public class NativeRepositoryServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/repositories";
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
    void testGetRepositories() {
        String token = getAdminToken( );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "" )
                .prettyPeek()
                .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        PagedResult<Repository> repositoryPagedResult = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
        assertEquals( 3, repositoryPagedResult.getPagination( ).getTotalCount( ) );
        List<Repository> data = response.getBody( ).jsonPath( ).getList( "data", Repository.class );
        assertTrue( data.stream( ).anyMatch( p -> "central".equals( p.getId( ) ) ) );
        assertTrue( data.stream( ).anyMatch( p -> "internal".equals( p.getId( ) ) ) );
        assertTrue( data.stream( ).anyMatch( p -> "snapshots".equals( p.getId( ) ) ) );
        Repository snapshotRepo = data.stream( ).filter( p -> "snapshots".equals( p.getId( ) ) ).findFirst( ).get( );
        assertEquals( "Archiva Managed Snapshot Repository", snapshotRepo.getName( ) );
        assertEquals( "MAVEN", snapshotRepo.getType() );
        assertEquals( "managed", snapshotRepo.getCharacteristic() );
        assertEquals( "default", snapshotRepo.getLayout() );
        assertTrue( snapshotRepo.isScanned( ) );
        assertTrue( snapshotRepo.isIndex( ) );

        Repository centralRepo = data.stream( ).filter( p -> "central".equals( p.getId( ) ) ).findFirst( ).get( );
        assertEquals( "Central Repository", centralRepo.getName( ) );
        assertEquals( "MAVEN", centralRepo.getType() );
        assertEquals( "remote", centralRepo.getCharacteristic() );
        assertEquals( "default", centralRepo.getLayout() );


    }

    @Test
    void testGetFilteredRepositories() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .queryParam( "q", "central" )
            .get( "" )
            .prettyPeek()
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        PagedResult<Repository> repositoryPagedResult = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
        assertEquals( 1, repositoryPagedResult.getPagination( ).getTotalCount( ) );
        List<Repository> data = response.getBody( ).jsonPath( ).getList( "data", Repository.class );
        assertTrue( data.stream( ).anyMatch( p -> "central".equals( p.getId( ) ) ) );
    }


    @Test
    void getStatistics() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "managed/internal/statistics" )
            .prettyPeek()
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );

    }

    @Test
    void scheduleScan() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .post( "managed/internal/scan/schedule" )
            .prettyPeek()
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );

    }

    @Test
    void immediateScan() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .post( "managed/internal/scan/now" )
            .prettyPeek()
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );

    }

    @Test
    void scanStatus() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "managed/internal/scan/status" )
            .prettyPeek()
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );

    }
}
