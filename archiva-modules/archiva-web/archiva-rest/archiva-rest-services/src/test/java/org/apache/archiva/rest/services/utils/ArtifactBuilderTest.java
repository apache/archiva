package org.apache.archiva.rest.services.utils;
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

import org.easymock.TestSubject;
import org.junit.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactBuilderTest
{
    @TestSubject
    private ArtifactBuilder builder = new ArtifactBuilder();

    @Test
    public void testBuildSnapshot()
    {
        assertThat( builder.getExtensionFromFile( Paths.get( "/tmp/foo-2.3-20141119.064321-40.jar" ) ) ).isEqualTo( "jar" );
    }

    @Test
    public void testBuildPom()
    {
        assertThat( builder.getExtensionFromFile( Paths.get( "/tmp/foo-1.0.pom" ) ) ).isEqualTo( "pom" );
    }

    @Test
    public void testBuildJar()
    {
        assertThat( builder.getExtensionFromFile( Paths.get( "/tmp/foo-1.0-sources.jar" ) ) ).isEqualTo( "jar" );
    }

    @Test
    public void testBuildTarGz()
    {
        assertThat( builder.getExtensionFromFile( Paths.get( "/tmp/foo-1.0.tar.gz" ) ) ).isEqualTo( "tar.gz" );
    }

    @Test
    public void testBuildPomZip()
    {
        assertThat( builder.getExtensionFromFile( Paths.get( "/tmp/foo-1.0.pom.zip" ) ) ).isEqualTo( "pom.zip" );
    }

    @Test
    public void testBuildR00()
    {
        assertThat( builder.getExtensionFromFile( Paths.get( "/tmp/foo-1.0.r00" ) ) ).isEqualTo( "r00" );
    }
}
