package org.apache.maven.archiva.configuration;

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

import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class FileTypesTest
    extends PlexusInSpringTestCase
{
    private FileTypes filetypes;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        filetypes = (FileTypes) lookup( FileTypes.class );
    }

    public void testIsArtifact()
    {
        assertTrue( filetypes.matchesArtifactPattern( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertTrue( filetypes.matchesArtifactPattern(
            "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertTrue( filetypes.matchesArtifactPattern( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );

        assertFalse(
            filetypes.matchesArtifactPattern( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.sha1" ) );
        assertFalse(
            filetypes.matchesArtifactPattern( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.md5" ) );
        assertFalse(
            filetypes.matchesArtifactPattern( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.asc" ) );
        assertFalse(
            filetypes.matchesArtifactPattern( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertFalse( filetypes.matchesArtifactPattern( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ) );
        assertFalse( filetypes.matchesArtifactPattern( "org/apache/derby/derby/maven-metadata.xml" ) );
    }
}
