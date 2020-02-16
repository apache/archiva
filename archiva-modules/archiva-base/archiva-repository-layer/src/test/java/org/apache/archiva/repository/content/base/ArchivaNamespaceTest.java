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

import org.apache.archiva.repository.content.base.builder.NamespaceOptBuilder;
import org.apache.archiva.repository.content.base.builder.OptBuilder;
import org.apache.archiva.repository.content.base.builder.WithNamespaceBuilder;
import org.apache.archiva.repository.mock.ManagedRepositoryContentMock;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class ArchivaNamespaceTest extends ContentItemTest
{


    @Test
    void nullNamespaceThrowsIllegalArgumentException() {
        WithNamespaceBuilder builder = ArchivaNamespace.withRepository( repository ).withAsset( asset );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withNamespace( null ) );
    }

    @Test
    public void namespaceOnly() throws IOException
    {
        assertNotNull( this.repository );
        ArchivaNamespace result = ArchivaNamespace.withRepository( repository )
            .withAsset( this.asset ).withNamespace( "test.1d.d" ).build( );

        assertNotNull( result );
        assertEquals( this.repository, result.getRepository( ) );
        assertEquals( this.asset, result.getAsset( ) );
        assertEquals( "test.1d.d", result.getNamespace( ) );
        assertNotNull( result.getAttributes( ) );
        assertEquals( 0, result.getAttributes( ).size( ) );
        assertArrayEquals( new String[]{ "test", "1d", "d" }, result.getNamespacePath().toArray() );
    }

    @Test
    public void namespaceWithEmptyPart() throws IOException
    {
        assertNotNull( this.repository );
        ArchivaNamespace result = ArchivaNamespace.withRepository( repository )
            .withAsset( this.asset ).withNamespace( "test.1d..d." ).build( );

        assertNotNull( result );
        assertEquals( this.repository, result.getRepository( ) );
        assertEquals( this.asset, result.getAsset( ) );
        assertEquals( "test.1d..d.", result.getNamespace( ) );
        assertNotNull( result.getAttributes( ) );
        assertEquals( 0, result.getAttributes( ).size( ) );
        assertArrayEquals( new String[]{ "test", "1d", "","d" }, result.getNamespacePath().toArray() );
    }

    @Test
    public void withSeparatorExpression() throws IOException
    {
        assertNotNull( this.repository );
        ArchivaNamespace result = ArchivaNamespace.withRepository( repository )
            .withAsset( this.asset ).withNamespace( "test.1d.d/abc/def" )
            .withSeparatorExpression( "/" )
            .build( );

        assertNotNull( result );
        assertEquals( this.repository, result.getRepository( ) );
        assertEquals( this.asset, result.getAsset( ) );
        assertEquals( "test.1d.d/abc/def", result.getNamespace( ) );
        assertNotNull( result.getAttributes( ) );
        assertEquals( 0, result.getAttributes( ).size( ) );
        assertArrayEquals( new String[]{ "test.1d.d", "abc", "def" }, result.getNamespacePath().toArray() );
    }

    @Test
    void badSeparatorExpression() {
        NamespaceOptBuilder builder = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.de/abc" );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withSeparatorExpression( null ) );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withSeparatorExpression( "(" ) );
    }


    @Test
    void equalityTests() {
        ArchivaNamespace ns1 = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.de/abc" ).build();
        ArchivaNamespace ns2 = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.de/abc" ).build();
        ArchivaNamespace ns3 = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.de/abcd" ).build();
        ArchivaNamespace ns4 = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.de/abc" ).
            withSeparatorExpression( "/" ).build();
        ArchivaNamespace ns5 = ArchivaNamespace.withRepository( new ManagedRepositoryContentMock() ).withAsset( asset ).withNamespace( "abc.de/abc" ).build();

        assertFalse( ns1 == ns2 );
        assertEquals( ns1, ns2 );
        assertNotEquals( ns1, ns3 );
        assertEquals( ns1, ns4 );
        assertNotEquals( ns1, ns5 );

    }


    @Override
    public OptBuilder getBuilder( )
    {
        return ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.def" );
    }
}