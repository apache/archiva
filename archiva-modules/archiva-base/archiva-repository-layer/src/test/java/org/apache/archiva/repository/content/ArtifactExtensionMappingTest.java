package org.apache.archiva.repository.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.Maven2RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.MavenArtifactFacet;
import org.apache.archiva.repository.AbstractRepositoryLayerTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * ArtifactExtensionMappingTest
 *
 * @version $Id$
 */
public class ArtifactExtensionMappingTest
    extends AbstractRepositoryLayerTestCase
{
    private RepositoryPathTranslator pathTranslator = new Maven2RepositoryPathTranslator(
        Collections.<ArtifactMappingProvider>emptyList() );

    @Test
    public void testIsMavenPlugin()
    {
        assertMavenPlugin( "maven-test-plugin" );
        assertMavenPlugin( "maven-clean-plugin" );
        assertMavenPlugin( "cobertura-maven-plugin" );
        assertMavenPlugin( "maven-project-info-reports-plugin" );
        assertMavenPlugin( "silly-name-for-a-maven-plugin" );

        assertNotMavenPlugin( "maven-plugin-api" );
        assertNotMavenPlugin( "foo-lib" );
        assertNotMavenPlugin( "another-maven-plugin-api" );
        assertNotMavenPlugin( "secret-maven-plugin-2" );
    }

    private void assertMavenPlugin( String artifactId )
    {
        assertEquals( "Should be detected as maven plugin: <" + artifactId + ">", "maven-plugin", getTypeFromArtifactId(
            artifactId ) );
    }

    private String getTypeFromArtifactId( String artifactId )
    {
        ArtifactMetadata artifact = pathTranslator.getArtifactFromId( null, "groupId", artifactId, "1.0",
                                                                      artifactId + "-1.0.jar" );
        MavenArtifactFacet facet = (MavenArtifactFacet) artifact.getFacet( MavenArtifactFacet.FACET_ID );
        return facet.getType();
    }

    private void assertNotMavenPlugin( String artifactId )
    {
        assertFalse( "Should NOT be detected as maven plugin: <" + artifactId + ">", "maven-plugin".equals(
            getTypeFromArtifactId( artifactId ) ) );
    }
}
