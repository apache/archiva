package org.apache.maven.archiva.repository.layout;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * AbstractBidirectionalRepositoryLayoutTestCase
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractBidirectionalRepositoryLayoutTestCase
    extends PlexusTestCase
{
    protected ManagedRepositoryConfiguration repository;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        repository = createTestRepository();
    }

    protected ManagedRepositoryConfiguration createTestRepository()
    {
        File targetDir = new File( getBasedir(), "target" );
        File testRepo = new File( targetDir, "test-repo" );

        if ( !testRepo.exists() )
        {
            testRepo.mkdirs();
        }

        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( "testRepo" );
        repo.setName( "Test Repository" );
        repo.setLocation( testRepo.getAbsolutePath() );
        return repo;
    }

    protected ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String classifier,
                                              String type )
    {
        ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, classifier, type );
        assertNotNull( artifact );
        artifact.getModel().setRepositoryId( repository.getId() );
        return artifact;
    }

    protected void assertArtifact( ArchivaArtifact actualArtifact, String groupId, String artifactId, String version,
                                   String classifier, String type )
    {
        String expectedId = groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type;

        assertNotNull( expectedId + " - Should not be null.", actualArtifact );

        assertEquals( expectedId + " - Group ID", groupId, actualArtifact.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, actualArtifact.getArtifactId() );
        if ( StringUtils.isNotBlank( classifier ) )
        {
            assertEquals( expectedId + " - Classifier", classifier, actualArtifact.getClassifier() );
        }
        assertEquals( expectedId + " - Version ID", version, actualArtifact.getVersion() );
        assertEquals( expectedId + " - Type", type, actualArtifact.getType() );
    }

    protected void assertArtifactReference( ArtifactReference actualReference, String groupId, String artifactId,
                                            String version, String classifier, String type )
    {
        String expectedId = "ArtifactReference - " + groupId + ":" + artifactId + ":" + version + ":" + classifier
            + ":" + type;

        assertNotNull( expectedId + " - Should not be null.", actualReference );

        assertEquals( expectedId + " - Group ID", groupId, actualReference.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, actualReference.getArtifactId() );
        if ( StringUtils.isNotBlank( classifier ) )
        {
            assertEquals( expectedId + " - Classifier", classifier, actualReference.getClassifier() );
        }
        assertEquals( expectedId + " - Version ID", version, actualReference.getVersion() );
        assertEquals( expectedId + " - Type", type, actualReference.getType() );
    }

    protected void assertVersionedReference( VersionedReference actualReference, String groupId, String artifactId,
                                             String version )
    {
        String expectedId = "VersionedReference - " + groupId + ":" + artifactId + ":" + version;

        assertNotNull( expectedId + " - Should not be null.", actualReference );
        assertEquals( expectedId + " - Group ID", groupId, actualReference.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, actualReference.getArtifactId() );
        assertEquals( expectedId + " - Version ID", version, actualReference.getVersion() );
    }

    protected void assertProjectReference( ProjectReference actualReference, String groupId, String artifactId )
    {
        String expectedId = "ProjectReference - " + groupId + ":" + artifactId;

        assertNotNull( expectedId + " - Should not be null.", actualReference );
        assertEquals( expectedId + " - Group ID", groupId, actualReference.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, actualReference.getArtifactId() );
    }

    protected void assertSnapshotArtifact( ArchivaArtifact actualArtifact, String groupId, String artifactId,
                                           String version, String classifier, String type )
    {
        String expectedId = groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type;

        assertNotNull( expectedId + " - Should not be null.", actualArtifact );

        assertEquals( expectedId + " - Group ID", actualArtifact.getGroupId(), groupId );
        assertEquals( expectedId + " - Artifact ID", actualArtifact.getArtifactId(), artifactId );
        assertEquals( expectedId + " - Version ID", actualArtifact.getVersion(), version );
        assertEquals( expectedId + " - Classifier", actualArtifact.getClassifier(), classifier );
        assertEquals( expectedId + " - Type", actualArtifact.getType(), type );
        assertTrue( expectedId + " - Snapshot", actualArtifact.isSnapshot() );
    }

}
