package org.apache.maven.archiva.configuration;

import junit.framework.TestCase;

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

/**
 * Test the generated LegacyArtifactPath class from Modello. This is primarily to test the hand coded methods.
 * @since 1.1
 */
public class LegacyArtifactPathTest
    extends TestCase
{

    private LegacyArtifactPath legacyArtifactPath = new LegacyArtifactPath();

    public void testLegacyArtifactPathWithClassifierResolution()
    {
        legacyArtifactPath.setArtifact( "groupId:artifactId:version:classifier:type" );

        assertEquals( "groupId", legacyArtifactPath.getGroupId() );
        assertEquals( "artifactId", legacyArtifactPath.getArtifactId() );
        assertEquals( "version", legacyArtifactPath.getVersion() );
        assertEquals( "classifier", legacyArtifactPath.getClassifier() );
        assertEquals( "type", legacyArtifactPath.getType() );
    }


    public void testLegacyArtifactPathWithoutClassifierResolution()
    {
        legacyArtifactPath.setArtifact( "groupId:artifactId:version::type" );

        assertEquals( "groupId", legacyArtifactPath.getGroupId() );
        assertEquals( "artifactId", legacyArtifactPath.getArtifactId() );
        assertEquals( "version", legacyArtifactPath.getVersion() );
        assertNull( legacyArtifactPath.getClassifier() );
        assertEquals( "type", legacyArtifactPath.getType() );
    }
}
