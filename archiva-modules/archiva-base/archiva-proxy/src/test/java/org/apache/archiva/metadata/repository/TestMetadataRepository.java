package org.apache.archiva.metadata.repository;

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
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// TODO: remove, it does nothing

@Service
public class TestMetadataRepository
    implements MetadataRepository
{
    @Override
    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        return null;
    }

    @Override
    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        return null;
    }

    @Override
    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getRootNamespaces( String repoId )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getNamespaces( String repoId, String namespace )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getProjects( String repoId, String namespace )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        return Collections.emptyList();
    }

    @Override
    public void updateProject( String repoId, ProjectMetadata project )
    {
        // no op
    }

    @Override
    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
    {
        // no op
    }

    @Override
    public void updateProjectVersion( String repoId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {
        // no op
    }

    @Override
    public void updateNamespace( String repoId, String namespace )
    {
        // no op
    }

    @Override
    public List<String> getMetadataFacets( String repodId, String facetId )
    {
        return Collections.emptyList();
    }

    @Override
    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
    {
        return null;
    }

    @Override
    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
    {
        // no op
    }

    @Override
    public void removeMetadataFacets( String repositoryId, String facetId )
    {
        // no op
    }

    @Override
    public void removeMetadataFacet( String repoId, String facetId, String name )
    {
        // no op
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getRepositories()
    {
        return Collections.emptyList();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum( String repoId, String checksum )
    {
        return Collections.emptyList();
    }

    @Override
    public void removeArtifact( String repositoryId, String namespace, String project, String version, String id )
    {
        // no op
    }

    @Override
    public void removeRepository( String repoId )
    {
        // no op
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
    {
        return Collections.emptyList();
    }

    @Override
    public void save()
    {
        // no op
    }

    @Override
    public void close()
    {
        // no op
    }

    @Override
    public void revert()
    {
        // no op
    }

    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        return false;
    }

    @Override
    public <T>T obtainAccess( Class<T> aClass )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( String repositoryId )
    {
        return Collections.emptyList();
    }

    @Override
    public void removeArtifact( String repositoryId, String namespace, String project, String version,
                                MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
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
