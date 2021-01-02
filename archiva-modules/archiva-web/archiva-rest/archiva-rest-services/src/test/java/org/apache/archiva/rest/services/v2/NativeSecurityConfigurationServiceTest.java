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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
                .prettyPeek()
                .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( "jpa", response.getBody( ).jsonPath( ).getString( "active_user_managers[0]" ) );
        assertEquals( "jpa", response.getBody( ).jsonPath( ).getString( "active_rbac_managers[0]" ) );
        assertEquals( "memory", response.getBody( ).jsonPath( ).getString( "properties.\"authentication.jwt.keystoreType\"" ) );
        assertEquals("10",response.getBody( ).jsonPath( ).getString( "properties.\"security.policy.allowed.login.attempt\""));
        assertTrue( response.getBody( ).jsonPath( ).getBoolean( "user_cache_enabled" ) );
        assertFalse( response.getBody( ).jsonPath( ).getBoolean( "ldap_active" ) );
    }

}
