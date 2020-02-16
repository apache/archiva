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

import org.apache.archiva.repository.content.base.builder.OptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class ArchivaArtifactTest extends ContentItemTest
{

    ArchivaProject project;
    ArchivaVersion version;

    @BeforeEach
    void init() {
        ArchivaNamespace namespace = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.def" ).build();
        project = ArchivaProject.withAsset( asset ).withNamespace( namespace ).withId( "proj001" ).build( );
        version = ArchivaVersion.withAsset( asset ).withProject( project ).withVersion( "2.33.1" ).build();
    }


    @Override
    public OptBuilder getBuilder( )
    {
        return ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" );
    }


    @Test
    void idOnly() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" ).build( );

        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "", item.getArtifactVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "", item.getContentType( ) );
        assertNotNull( item.getId( ) );
        assertEquals( "testartifact", item.getId( ) );

    }


    @Test
    void withArtifactVersion() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "1.0.10494949" )
            .build( );
        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "", item.getContentType( ) );
        assertEquals( "testartifact", item.getId( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "1.0.10494949", item.getArtifactVersion( ) );
    }

    @Test
    void withType() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withType( "javadoc" )
            .build( );
        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "javadoc", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "", item.getContentType( ) );
        assertEquals( "testartifact", item.getId( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "", item.getArtifactVersion( ) );
    }

    @Test
    void withClassifier() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withClassifier( "source" )
            .build( );
        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "source", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "", item.getContentType( ) );
        assertEquals( "testartifact", item.getId( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "", item.getArtifactVersion( ) );
    }

    @Test
    void withRemainder() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withRemainder( "jar.md5" )
            .build( );
        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "jar.md5", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "", item.getContentType( ) );
        assertEquals( "testartifact", item.getId( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "", item.getArtifactVersion( ) );
    }

    @Test
    void withContentType() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withContentType( "text/xml" )
            .build( );
        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "text/xml", item.getContentType( ) );
        assertEquals( "testartifact", item.getId( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "", item.getArtifactVersion( ) );
    }

    @Test
    void withAllAttributes() {
        ArchivaArtifact item = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals(repository, item.getRepository());
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( version, item.getVersion( ) );
        assertNotNull( item.getType( ) );
        assertEquals( "jar", item.getType( ) );
        assertNotNull( item.getClassifier( ) );
        assertEquals( "private", item.getClassifier( ) );
        assertNotNull( item.getRemainder( ) );
        assertEquals( "jar.sha1", item.getRemainder( ) );
        assertNotNull( item.getContentType( ) );
        assertEquals( "text/html", item.getContentType( ) );
        assertEquals( "testartifact", item.getId( ) );
        assertNotNull( item.getArtifactVersion( ) );
        assertEquals( "3.0.484848", item.getArtifactVersion( ) );
    }


    @Test
    void equality() {
        ArchivaArtifact item1 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item2 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484849" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item3 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item4 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "sourcejar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item5 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "public" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item6 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.md5" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item7 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/xml" )
            .build( );

        ArchivaArtifact item8 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact2" )
            .withArtifactVersion( "3.0.484848" )
            .withType( "jar" )
            .withClassifier( "private" )
            .withRemainder( "jar.sha1" )
            .withContentType( "text/html" )
            .build( );

        ArchivaArtifact item9 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact2" )
            .build( );

        ArchivaArtifact item10 = ArchivaArtifact.withAsset( asset ).withVersion( version ).withId( "testartifact2" )
            .build( );


        assertNotEquals( item1, item2 );
        assertEquals( item1, item3 );
        assertNotEquals( item1, item4 );
        assertNotEquals( item1, item5 );
        // remainder and content type are ignored
        assertEquals( item1, item6 );
        assertEquals( item1, item7 );

        assertNotEquals( item1, item8 );
        assertEquals( item9, item10 );

    }

}