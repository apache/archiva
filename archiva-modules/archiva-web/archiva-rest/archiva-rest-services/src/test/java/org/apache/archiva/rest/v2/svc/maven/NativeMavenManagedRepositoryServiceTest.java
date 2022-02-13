package org.apache.archiva.rest.v2.svc.maven;

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

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepository;
import org.apache.archiva.rest.api.v2.svc.RestConfiguration;
import org.apache.archiva.rest.v2.svc.AbstractNativeRestServices;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag( "rest-native" )
@TestMethodOrder( MethodOrderer.OrderAnnotation.class )
@DisplayName( "Native REST tests for V2 ManagedRepositoryService" )
public class NativeMavenManagedRepositoryServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/repositories/maven/managed";
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
    @Order( 1 )
    void testGetRepositories( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "" )
            .then( ).statusCode( 200 ).extract( ).response( );
        JsonPath json = response.getBody( ).jsonPath( );
        assertEquals( 2, json.getInt( "pagination.total_count" ) );
        assertEquals( 0, json.getInt( "pagination.offset" ) );
        assertEquals( Integer.valueOf( RestConfiguration.DEFAULT_PAGE_LIMIT ), json.getInt( "pagination.limit" ) );
        List<MavenManagedRepository> repositories = json.getList( "data", MavenManagedRepository.class );
        assertEquals( "internal", repositories.get( 0 ).getId( ) );
        assertEquals( "snapshots", repositories.get( 1 ).getId( ) );
    }

    private Response createRepository(String id, String name, String description, String token) {
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "id", id );
        jsonAsMap.put( "name", name );
        jsonAsMap.put( "description", description );
        return given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .body( jsonAsMap )
            .post( "" )
            .then( ).statusCode( 201 ).extract( ).response( );
    }


    @Test
    @Order( 2 )
    void testCreateRepository() {
        String token = getAdminToken( );
        Response response = createRepository( "repo001", "Repository 001", "This is repository 001", token );
        assertNotNull( response );
        JsonPath json = response.getBody( ).jsonPath( );
        assertNotNull( json );
        assertEquals( "repo001", json.get( "id" ) );
        assertEquals( "Repository 001", json.get( "name" ) );
        assertEquals( "MAVEN", json.get( "type" ) );
        assertEquals( "This is repository 001", json.get( "description" ) );
    }



}
