package org.apache.archiva.maven2.model;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


public class ModelTest
{
    @Test
    void testTreeEntry() throws JAXBException, IOException
    {
        TreeEntry parent = new TreeEntry( );
        TreeEntry entry = new TreeEntry( );
        entry.setParent( parent );
        Artifact artifact1 = new Artifact( );
        artifact1.setGroupId( "test.group" );
        artifact1.setArtifactId( "artifact1" );
        artifact1.setVersion( "1.0" );
        entry.setArtifact( artifact1 );

        TreeEntry child1 = new TreeEntry( );
        TreeEntry child2 = new TreeEntry( );
        child1.setParent( entry );
        child2.setParent( entry );
        Artifact artifact2 = new Artifact( );
        artifact2.setGroupId( "test.group" );
        artifact2.setArtifactId( "artifact1" );
        artifact2.setVersion( "1.1" );
        child1.setArtifact( artifact2 );
        child2.setArtifact( artifact2 );
        entry.setChilds( Arrays.asList( child1, child2) );

        ObjectMapper objectMapper = new ObjectMapper( );
        objectMapper.registerModule( new JaxbAnnotationModule( ) );
        StringWriter sw = new StringWriter( );
        objectMapper.writeValue( sw, entry );

        JSONObject js = new JSONObject( sw.toString() );
        assertFalse( js.has( "parent" ) );
        assertTrue( js.has( "childs" ) );
        assertEquals(2, js.getJSONArray( "childs" ).length());
        assertTrue( js.has( "artifact" ) );

    }
}
