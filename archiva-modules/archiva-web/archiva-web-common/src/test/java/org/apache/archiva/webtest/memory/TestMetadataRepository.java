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
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TestMetadataRepository
    implements MetadataRepository
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
    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getRootNamespaces( String repoId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getNamespaces( String repoId, String namespace )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getProjects( String repoId, String namespace )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        return versions;
    }

    @Override
    public void updateProject( String repoId, ProjectMetadata project )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectVersion( String repoId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNamespace( String repoId, String namespace )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getMetadataFacets( String repodId, String facetId )
    {
        return Collections.emptyList();
    }

    @Override
    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
 {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMetadataFacets( String repositoryId, String facetId )
    {
        throw new UnsupportedOperationException();
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
    public List<ArtifactMetadata> getArtifactsByChecksum( String repoId, String checksum )
    {
        return null;
    }

    @Override
    public void removeArtifact( String repositoryId, String namespace, String project, String version, String id )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeArtifact( String repositoryId, String namespace, String project, String version,
                                MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRepository( String repoId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
    {
        return artifacts;
    }

    @Override
    public void save()
    {
    }

    @Override
    public void close()
    {
    }

    @Override
    public void revert()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        return false;
    }

    @Override
    public <T>T obtainAccess( Class<T> aClass )
    {
        return null;
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( String repositoryId )
    {
        return artifacts;
    }

    @Override
    public void removeArtifact( ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException
    {

    }

    @Override
    public void removeProjectVersion( String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException
    {

    }

    @Override
    public void removeProject( String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean hasMetadataFacet( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        return false;
    }
}