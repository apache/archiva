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
import org.apache.archiva.repository.content.base.builder.WithVersionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class ArchivaVersionTest extends ContentItemTest
{

    ArchivaProject project;

    @BeforeEach
    void init() {
        ArchivaNamespace namespace = ArchivaNamespace.withRepository( repository ).withAsset( asset ).withNamespace( "abc.def" ).build();
        project = ArchivaProject.withAsset( asset ).withNamespace( namespace ).withId( "proj001" ).build( );
    }

    @Override
    public OptBuilder getBuilder( )
    {
        return ArchivaVersion.withAsset( asset ).withProject( project ).withVersion( "1.5.8" );
    }

    @Test
    void versionOnly() {
        ArchivaVersion item = ArchivaVersion.withAsset( asset ).withProject( project ).withVersion( "3.4.5" ).build();
        assertNotNull( item );
        assertNotNull( item.getRepository( ) );
        assertEquals( repository, item.getRepository( ) );
        assertNotNull( item.getAsset( ) );
        assertEquals( asset, item.getAsset( ) );
        assertNotNull( item.getProject( ) );
        assertEquals( project, item.getProject( ) );
        assertNotNull( item.getVersion( ) );
        assertEquals( "3.4.5", item.getVersion( ) );
        assertNotNull( item.getVersionSegments( ) );
        assertArrayEquals( new String[]{"3", "4", "5"}, item.getVersionSegments( ).toArray( ) );
    }

    @Test
    void versionSegments() {
        ArchivaVersion item = ArchivaVersion.withAsset( asset ).withProject( project ).withVersion( "3.455.5" ).build();
        assertNotNull( item );
        assertNotNull( item.getVersionSegments( ) );
        assertArrayEquals( new String[]{"3", "455", "5"}, item.getVersionSegments( ).toArray( ) );

        ArchivaVersion item2 = ArchivaVersion.withAsset( asset ).withProject( project ).withVersion( "xd43.455.5" ).build();
        assertNotNull( item2 );
        assertNotNull( item2.getVersionSegments( ) );
        assertArrayEquals( new String[]{"xd43", "455", "5"}, item2.getVersionSegments( ).toArray( ) );
    }

    @Test
    void illegalVersion() {
        WithVersionBuilder builder = ArchivaVersion.withAsset( asset ).withProject( project );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withVersion( null ) );
        assertThrows( IllegalArgumentException.class, ( ) -> builder.withVersion( "" ) );
    }

}