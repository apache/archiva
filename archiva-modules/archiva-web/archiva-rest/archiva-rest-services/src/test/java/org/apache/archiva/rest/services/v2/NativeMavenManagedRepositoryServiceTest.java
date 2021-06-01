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

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.model.PropertyEntry;
import org.apache.archiva.rest.api.model.v2.BeanInformation;
import org.apache.archiva.rest.api.model.v2.CacheConfiguration;
import org.apache.archiva.rest.api.model.v2.LdapConfiguration;
import org.apache.archiva.rest.api.model.v2.MavenManagedRepository;
import org.apache.archiva.rest.api.services.v2.RestConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
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



}
