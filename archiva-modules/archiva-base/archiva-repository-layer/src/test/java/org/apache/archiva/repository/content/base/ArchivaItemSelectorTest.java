package org.apache.archiva.repository.content.base;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchivaItemSelectorTest
{

    @Test
    void getProjectId( )
    {

        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withProjectId( "test-project-123" ).build( );
        assertEquals( "test-project-123", selector.getProjectId( ) );
        assertTrue( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );

        assertEquals( "", selector.getNamespace( ) );
        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getType( ) );
        assertEquals( "", selector.getClassifier( ) );
        assertNotNull( selector.getAttributes( ) );
    }

    @Test
    void getNamespace( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withNamespace( "abc.de.fg" ).build();
        assertEquals( "abc.de.fg", selector.getNamespace( ) );
        assertFalse( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );

        assertEquals( "", selector.getProjectId( ) );
        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getType( ) );
        assertEquals( "", selector.getClassifier( ) );
        assertNotNull( selector.getAttributes( ) );
    }

    @Test
    void getVersion( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withVersion( "1.15.20.3" ).build();
        assertEquals( "1.15.20.3", selector.getVersion( ) );
        assertFalse( selector.hasProjectId( ) );
        assertTrue( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );


        assertEquals( "", selector.getNamespace( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getType( ) );
        assertEquals( "", selector.getClassifier( ) );
        assertNotNull( selector.getAttributes( ) );
    }

    @Test
    void getArtifactVersion( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withArtifactVersion( "5.13.2.4" ).build();
        assertEquals( "5.13.2.4", selector.getArtifactVersion() );
        assertFalse( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertTrue( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );

        assertEquals( "", selector.getNamespace( ) );
        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getType( ) );
        assertEquals( "", selector.getClassifier( ) );
        assertNotNull( selector.getAttributes( ) );

    }

    @Test
    void getArtifactId( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withArtifactId( "xml-tools" ).build();
        assertEquals( "xml-tools", selector.getArtifactId() );
        assertFalse( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertTrue( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );

        assertEquals( "", selector.getNamespace( ) );
        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getType( ) );
        assertEquals( "", selector.getClassifier( ) );
        assertNotNull( selector.getAttributes( ) );
    }

    @Test
    void getType( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withType( "javadoc" ).build();
        assertEquals( "javadoc", selector.getType() );
        assertFalse( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertTrue( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );

        assertEquals( "", selector.getNamespace( ) );
        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getClassifier( ) );
        assertNotNull( selector.getAttributes( ) );
    }

    @Test
    void getClassifier( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withClassifier( "source" ).build();
        assertEquals( "source", selector.getClassifier() );
        assertFalse( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertTrue( selector.hasClassifier( ) );
        assertFalse( selector.hasAttributes( ) );

        assertEquals( "", selector.getNamespace( ) );
        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getType( ) );
        assertNotNull( selector.getAttributes( ) );

    }

    @Test
    void getAttribute( )
    {
        ArchivaItemSelector selector = ArchivaItemSelector.builder( ).withAttribute( "test1","value1" ).
            withAttribute( "test2", "value2" ).build();
        assertEquals( "value1", selector.getAttribute("test1") );
        assertEquals( "value2", selector.getAttribute("test2") );
        assertFalse( selector.hasProjectId( ) );
        assertFalse( selector.hasVersion( ) );
        assertFalse( selector.hasArtifactId( ) );
        assertFalse( selector.hasArtifactVersion( ) );
        assertFalse( selector.hasType( ) );
        assertFalse( selector.hasClassifier( ) );
        assertTrue( selector.hasAttributes( ) );

        assertEquals( "", selector.getVersion( ) );
        assertEquals( "", selector.getArtifactId( ) );
        assertEquals( "", selector.getArtifactVersion( ) );
        assertEquals( "", selector.getType( ) );
        assertEquals( "", selector.getClassifier( ) );

    }

}