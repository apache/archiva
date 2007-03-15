package org.apache.maven.archiva.database.artifact;

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

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;

/**
 * ArtifactPersistenceTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactPersistenceTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactFactory artifactFactory;

    private ArtifactPersistence db;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        db = (ArtifactPersistence) lookup( ArtifactPersistence.class.getName() );
    }

    public void testLookup()
    {
        assertNotNull( db );
    }

    public void testAddArtifact() throws ArchivaDatabaseException
    {
        String groupId = "org.apache.maven.archiva";
        String artifactId = "archiva-test-artifact";
        String version = "1.0";

        Artifact artifact = artifactFactory
            .createArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar" );

        db.create( artifact );

        Artifact fetched = db.read( groupId, artifactId, version );

        assertNotNull( "Should have fetched an Artifact.", fetched );
        assertEquals( "Should have fetched the expected Artifact.", artifact, fetched );
    }
}
