package org.apache.maven.archiva.repository.content;

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
import org.apache.maven.archiva.repository.ArchivaArtifact;
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * AbstractBidirectionalRepositoryLayoutTestCase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AbstractBidirectionalRepositoryLayoutTestCase extends PlexusTestCase
{
    protected ArchivaRepository repository;

    protected void setUp() throws Exception
    {
        super.setUp();
        
        repository = createTestRepository();
    }
    
    protected ArchivaRepository createTestRepository()
    {
        File targetDir = new File( getBasedir(), "target" );
        File testRepo = new File( targetDir, "test-repo" );
    
        if ( !testRepo.exists() )
        {
            testRepo.mkdirs();
        }
    
        String repoUri = "file://" + StringUtils.replace( testRepo.getAbsolutePath(), "\\", "/" ) ;
    
        ArchivaRepository repo = new ArchivaRepository( "testRepo", "Test Repository", repoUri );
    
        return repo;
    }

    protected ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String classifier, String type )
    {
        ArchivaArtifact artifact = new ArchivaArtifact( repository, groupId, artifactId, version, classifier, type );
        assertNotNull( artifact );
        return artifact;
    }

    protected void assertArtifact( ArchivaArtifact actualArtifact, String groupId, String artifactId, String version, String classifier, String type )
    {
        String expectedId = groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type;
    
        assertEquals( expectedId + " - Group ID", actualArtifact.getGroupId(), groupId );
        assertEquals( expectedId + " - Artifact ID", actualArtifact.getArtifactId(), artifactId );
        assertEquals( expectedId + " - Version ID", actualArtifact.getVersion(), version );
        assertEquals( expectedId + " - Classifier", actualArtifact.getClassifier(), classifier );
        assertEquals( expectedId + " - Type", actualArtifact.getType(), type );
    }

    protected void assertSnapshotArtifact( ArchivaArtifact actualArtifact, String groupId, String artifactId, String version, String classifier, String type )
    {
        String expectedId = groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type;
    
        assertEquals( expectedId + " - Group ID", actualArtifact.getGroupId(), groupId );
        assertEquals( expectedId + " - Artifact ID", actualArtifact.getArtifactId(), artifactId );
        assertEquals( expectedId + " - Version ID", actualArtifact.getVersion(), version );
        assertEquals( expectedId + " - Classifier", actualArtifact.getClassifier(), classifier );
        assertEquals( expectedId + " - Type", actualArtifact.getType(), type );
        assertTrue( expectedId + " - Snapshot", actualArtifact.isSnapshot() );
    }

}
