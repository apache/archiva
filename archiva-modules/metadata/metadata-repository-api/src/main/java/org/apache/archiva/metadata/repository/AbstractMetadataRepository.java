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
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

public abstract class AbstractMetadataRepository
    implements MetadataRepository
{

    @Override
    public void updateProject( String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectVersion( String repositoryId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getMetadataFacets( String repositoryId, String facetId )
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

    @Override
    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
    }

    @Override
    public void removeMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
    }

    @Override
    public void removeMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( String repositoryId, Date startTime, Date endTime )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getRepositories()
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionMetadata( String key , String value , String repositoryId  )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByMetadata( String key , String value , String repositoryId  )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProperty( String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeArtifact( String repositoryId, String namespace, String project, String version, String id )
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
    public void removeArtifact( String repositoryId, String namespace, String project, String version,
                                MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRepository( String repositoryId )
        throws MetadataRepositoryException
    {
    }

    @Override
    public void removeNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<ArtifactMetadata> getArtifacts( String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getRootNamespaces( String repoId )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getNamespaces( String repoId, String namespace )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getProjects( String repoId, String namespace )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProjectVersion( String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProject( String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save()
    {
    }

    @Override
    public void close()
        throws MetadataRepositoryException
    {
    }

    @Override
    public void revert()
    {
    }

    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T obtainAccess( Class<T> aClass )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( String text, String repositoryId, boolean exact )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( String key, String text, String repositoryId, boolean exact )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

}
