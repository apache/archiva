package org.apache.archiva.webtest.memory;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.AbstractMetadataRepository;

import java.util.*;

public class TestMetadataRepository
    extends AbstractMetadataRepository
{
    private static final String TEST_REPO = "test-repo";

    private static final String TEST_NAMESPACE = "org.apache.archiva";

    private List<ArtifactMetadata> artifacts = new ArrayList<>();

    private List<String> versions = new ArrayList<>();

    public TestMetadataRepository()
    {
        Date whenGathered = new Date( 123456789 );

        addArtifact( "artifact-one", "1.0", whenGathered );
        addArtifact( "artifact-one", "1.1", whenGathered );
        addArtifact( "artifact-one", "2.0", whenGathered );
        addArtifact( "artifact-two", "1.0.1", whenGathered );
        addArtifact( "artifact-two", "1.0.2", whenGathered );
        addArtifact( "artifact-two", "1.0.3-SNAPSHOT", whenGathered );
        addArtifact( "artifact-three", "2.0-SNAPSHOT", whenGathered );
        addArtifact( "artifact-four", "1.1-beta-2", whenGathered );
    }

    private void addArtifact( String projectId, String projectVersion, Date whenGathered )
    {
        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setFileLastModified( System.currentTimeMillis() );
        artifact.setNamespace( TEST_NAMESPACE );
        artifact.setProjectVersion( projectVersion );
        artifact.setVersion( projectVersion );
        artifact.setId( projectId + "-" + projectVersion + ".jar" );
        artifact.setProject( projectId );
        artifact.setRepositoryId( TEST_REPO );
        artifact.setWhenGathered( whenGathered );
        artifacts.add( artifact );

        versions.add( projectVersion );
    }

    @Override
    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        return versions;
    }

    @Override
    public List<String> getMetadataFacets( String repodId, String facetId )
    {
        return Collections.emptyList();
    }

    @Override
    public void removeMetadataFacet( String repoId, String facetId, String name )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
    {
        return artifacts;
    }

    @Override
    public Collection<String> getRepositories()
    {
        return Collections.singletonList( TEST_REPO );
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
    {
        return artifacts;
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( String repositoryId )
    {
        return artifacts;
    }

}