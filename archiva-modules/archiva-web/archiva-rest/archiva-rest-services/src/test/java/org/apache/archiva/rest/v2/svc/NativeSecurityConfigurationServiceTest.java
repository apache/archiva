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
import org.apache.archiva.components.rest.model.PropertyEntry;
import org.apache.archiva.rest.api.v2.model.BeanInformation;
import org.apache.archiva.rest.api.v2.model.CacheConfiguration;
import org.apache.archiva.rest.api.v2.model.LdapConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
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
@TestMethodOrder( MethodOrderer.Random.class )
@DisplayName( "Native REST tests for V2 SecurityConfigurationService" )
public class NativeSecurityConfigurationServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/security";
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
    void testGetConfiguration() {
        String token = getAdminToken( );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "config" )
                .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( "jpa", response.getBody( ).jsonPath( ).getString( "active_user_managers[0]" ) );
        assertEquals( "jpa", response.getBody( ).jsonPath( ).getString( "active_rbac_managers[0]" ) );
        assertEquals( "memory", response.getBody( ).jsonPath( ).getString( "properties.\"authentication.jwt.keystoreType\"" ) );
        assertEquals("10",response.getBody( ).jsonPath( ).getString( "properties.\"security.policy.allowed.login.attempt\""));
        assertTrue( response.getBody( ).jsonPath( ).getBoolean( "user_cache_enabled" ) );
        assertFalse( response.getBody( ).jsonPath( ).getBoolean( "ldap_active" ) );
    }

    @Test
    void testGetConfigurationProperties() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "config/properties" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        PagedResult<PropertyEntry> result = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
        List<PropertyEntry> propList = response.getBody( ).jsonPath( ).getList( "data", PropertyEntry.class );
        assertEquals( 10, result.getPagination( ).getLimit( ) );
        assertEquals( 0, result.getPagination( ).getOffset( ) );
        assertEquals( 47, result.getPagination( ).getTotalCount( ) );
        assertEquals( "authentication.jwt.keystoreType", propList.get( 0 ).getKey() );

        response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .queryParam( "offset", "3" )
            .queryParam( "limit", "5" )
            .get( "config/properties" )
            .then( ).statusCode( 200 ).extract( ).response( );

        assertNotNull( response );
        result = response.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
        assertEquals( 5, result.getPagination( ).getLimit( ) );
        assertEquals( 47, result.getPagination( ).getTotalCount( ) );
        propList = response.getBody( ).jsonPath( ).getList( "data", PropertyEntry.class );
        assertEquals( "authentication.jwt.refreshLifetimeMs", propList.get( 0 ).getKey() );
    }

    @Test
    void testGetLdapConfiguration() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "config/ldap" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( "", response.getBody( ).jsonPath( ).get( "host_name" ) );
        assertEquals( 0, response.getBody( ).jsonPath( ).getMap( "properties" ).size( ) );
    }


    @Test
    void testUpdateLdapConfiguration() {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonMap = new HashMap<>( );
            jsonMap.put( "host_name", "localhost" );
            jsonMap.put( "port", 389 );
            jsonMap.put( "ssl_enabled", false );
            jsonMap.put( "writable", true );
            jsonMap.put( "base_dn", "dc=apache,dc=org" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonMap )
                .put( "config/ldap" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "config/ldap" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            LdapConfiguration config = response.getBody( ).jsonPath( ).getObject( "", LdapConfiguration.class );
            assertEquals( "localhost", config.getHostName( ) );
            assertEquals( 389, config.getPort( ) );
            assertFalse( config.isSslEnabled( ) );
            assertTrue( config.isWritable( ) );
            assertEquals( "dc=apache,dc=org", config.getBaseDn( ) );
        } finally
        {
            Map<String, Object> jsonMap = new HashMap<>( );
            jsonMap.put( "host_name", "" );
            jsonMap.put( "port", -1 );
            jsonMap.put( "ssl_enabled", false );
            jsonMap.put( "base_dn", "" );
            jsonMap.put( "writable", false );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonMap )
                .put( "config/ldap" )
                .then( ).statusCode( 200 );

        }
    }


    @Test
    void testGetCacheConfiguration() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "config/cache" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
    }

    @Test
    void testUpdateCacheConfiguration() {
        String token = getAdminToken( );

        try
        {
            Map<String, Object> jsonMap = new HashMap<>( );
            jsonMap.put( "time_to_idle_seconds", 1600 );
            jsonMap.put( "time_to_live_seconds", 12000 );
            jsonMap.put( "max_entries_in_memory", 500 );
            jsonMap.put( "max_entries_on_disk", 400 );

            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonMap )
                .put( "config/cache" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "config/cache" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            CacheConfiguration config = response.getBody( ).jsonPath( ).getObject( "", CacheConfiguration.class );
            assertEquals( 1600, config.getTimeToIdleSeconds( ) );
            assertEquals( 12000, config.getTimeToLiveSeconds( ) );
            assertEquals( 500, config.getMaxEntriesInMemory( ) );
            assertEquals( 400, config.getMaxEntriesOnDisk( ) );
        } finally
        {
            Map<String, Object> jsonMap = new HashMap<>( );
            jsonMap.put( "time_to_idle_seconds", 1800 );
            jsonMap.put( "time_to_live_seconds", 14400 );
            jsonMap.put( "max_entries_in_memory", 1000 );
            jsonMap.put( "max_entries_on_disk", 0 );

            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonMap )
                .put( "config/cache" )
                .then( ).statusCode( 200 );

        }
    }


    @Test
    void testGetUserManagers() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "user_managers" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<BeanInformation> usrList = response.getBody( ).jsonPath( ).getList( "", BeanInformation.class );
        assertEquals( 2, usrList.size( ) );
        assertTrue( usrList.stream( ).anyMatch( bi -> "LDAP User Manager".equals( bi.getDisplayName( ) ) ) );
        assertTrue( usrList.stream( ).anyMatch( bi -> "Database User Manager".equals( bi.getDisplayName( ) ) ) );
    }

    @Test
    void testGetRbacManagers() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "rbac_managers" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<BeanInformation> rbacList = response.getBody( ).jsonPath( ).getList( "", BeanInformation.class );
        assertEquals( 2, rbacList.size( ) );
        assertTrue( rbacList.stream( ).anyMatch( bi -> "Database RBAC Manager".equals( bi.getDisplayName( ) ) ) );
        assertTrue( rbacList.stream( ).anyMatch( bi -> "LDAP RBAC Manager".equals( bi.getDisplayName( ) ) ) );
    }

    @Test
    void testUpdateConfiguration() {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "active_user_managers", Arrays.asList( "jpa", "ldap" ) );
            jsonAsMap.put( "active_rbac_managers", Arrays.asList( "jpa" ) );
            jsonAsMap.put( "user_cache_enabled", false );
            jsonAsMap.put( "ldap_active", false );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .put( "config" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "config" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            assertEquals( 2, response.getBody( ).jsonPath( ).getList( "active_user_managers" ).size( ) );
        } finally
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "active_user_managers", Arrays.asList( "jpa" ) );
            jsonAsMap.put( "active_rbac_managers", Arrays.asList( "jpa" ) );
            jsonAsMap.put( "user_cache_enabled", true );
            jsonAsMap.put( "ldap_active", false );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .put( "config" )
                .then( ).statusCode( 200 );

        }

    }

    @Test
    void testGetConfigProperty() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "config/properties/rest.csrffilter.absentorigin.deny" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( "true", response.getBody( ).jsonPath( ).getString( "value" ) );
    }

    @Test
    void testUpdateConfigProperty() {
        String token = getAdminToken( );

        try
        {
            Map<String, String> jsonMap = new HashMap<>( );
            jsonMap.put( "value", "false" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonMap )
                .put( "config/properties/rest.csrffilter.absentorigin.deny" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );


            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "config/properties/rest.csrffilter.absentorigin.deny" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            assertEquals( "false", response.getBody( ).jsonPath( ).getString( "value" ) );
        } finally
        {
            Map<String, String> jsonMap = new HashMap<>( );
            jsonMap.put( "value", "true" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonMap )
                .put( "config/properties/rest.csrffilter.absentorigin.deny" )
                .then( ).statusCode( 200 ).extract( ).response( );
        }
    }


}
