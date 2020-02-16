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

package org.apache.archiva.repository.content.base;

import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.base.builder.OptBuilder;
import org.apache.archiva.repository.content.base.builder.ProjectWithIdBuilder;
import org.apache.archiva.repository.content.base.builder.WithNamespaceObjectBuilder;
import org.apache.archiva.repository.mock.ManagedRepositoryContentMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class ArchivaProjectTest extends ContentItemTest
{

    Namespace namespace;

    @BeforeEach
    void init() {
        namespace = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "test.namespace.123" ).build();
    }

    @Override
    public OptBuilder getBuilder( )
    {
        return ArchivaProject.withAsset( asset ).withNamespace( namespace ).withId( "abcde" );
    }

    @Test
    void withNamespaceAndId() {
        ArchivaProject item = ArchivaProject.withAsset( asset ).withNamespace( namespace )
            .withId( "abcdefg" ).build( );

        assertNotNull( item );
        assertEquals( "abcdefg", item.getId( ) );
        assertNotNull( item.getNamespace( ) );
        assertEquals( namespace, item.getNamespace( ) );
        assertNotNull( item.getRepository( ) );
        assertEquals( repository, item.getRepository( ) );
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );

    }

    @Test
    void illegalNamespace() {
        WithNamespaceObjectBuilder builder = ArchivaProject.withAsset( asset );

        assertThrows( IllegalArgumentException.class, ( ) -> builder.withNamespace( null ) );
    }

    @Test
    void illegalId() {
        ProjectWithIdBuilder builder = ArchivaProject.withAsset( asset ).withNamespace( namespace );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withId( null ) );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withId( "" ) );
    }

    @Test
    void equalityTests() {
        ArchivaProject item1 = ArchivaProject.withAsset( asset ).withNamespace( namespace )
            .withId( "abcdefgtest1" ).build( );
        ArchivaProject item2 = ArchivaProject.withAsset( asset ).withNamespace( namespace )
            .withId( "abcdefgtest2" ).build( );
        ArchivaProject item3 = ArchivaProject.withAsset( asset ).withNamespace( namespace )
            .withId( "abcdefgtest1" ).build( );
        ArchivaNamespace ns2 = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "test.namespace.123" ).build( );
        ArchivaProject item4 = ArchivaProject.withAsset( asset ).withNamespace( ns2 )
            .withId( "abcdefgtest1" ).build( );
        ArchivaNamespace ns3 = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "test.namespace.1234" ).build( );
        ArchivaProject item5 = ArchivaProject.withAsset( asset ).withNamespace( ns3 )
            .withId( "abcdefgtest1" ).build( );

        assertNotEquals( item1, item2 );
        assertEquals( item1, item3 );
        assertEquals( item1, item4 );
        assertNotEquals( item1, item5 );

    }



}