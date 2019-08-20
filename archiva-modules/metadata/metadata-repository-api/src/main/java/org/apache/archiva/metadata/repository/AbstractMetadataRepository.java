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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public abstract class AbstractMetadataRepository
    implements MetadataRepository
{

    protected MetadataService metadataService;

    public AbstractMetadataRepository() {

    }

    public AbstractMetadataRepository( MetadataService metadataService )
    {
        this.metadataService = metadataService;
    }

    @Override
    public void updateProject( RepositorySession session, String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArtifact( RepositorySession session, String repositoryId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectVersion( RepositorySession session, String repositoryId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMetadataFacet( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        return false;
    }

    @Override
    public void addMetadataFacet( RepositorySession session, String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
    }

    @Override
    public void removeMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
    }

    @Override
    public void removeMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange(RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ArtifactMetadata> getArtifactsByChecksum( RepositorySession session, String repositoryId, String checksum )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionMetadata( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByMetadata( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProperty( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeArtifact( RepositorySession session, String repositoryId, String namespace, String project, String version, String id )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeArtifact( RepositorySession session, ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeArtifact( RepositorySession session, String repositoryId, String namespace, String project, String version,
                                MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRepository( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException
    {
    }

    @Override
    public void removeNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectMetadata getProject( RepositorySession session, String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectVersionMetadata getProjectVersion( RepositorySession session, String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getArtifactVersions( RepositorySession session, String repoId, String namespace, String projectId,
                                                   String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ProjectVersionReference> getProjectReferences( RepositorySession session, String repoId, String namespace, String projectId,
                                                                     String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getRootNamespaces( RepositorySession session, String repoId )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getNamespaces( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getProjects( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getProjectVersions( RepositorySession session, String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProjectVersion( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( RepositorySession session, String repoId, String namespace, String projectId,
                                                      String projectVersion )
        throws MetadataResolutionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProject( RepositorySession session, String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void close()
        throws MetadataRepositoryException
    {
    }

    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T obtainAccess( RepositorySession session, Class<T> aClass )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String text, boolean exact )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String key, String text, boolean exact )
        throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession session, String repositoryId, Class<T> facetClazz ) throws MetadataRepositoryException
    {
        return getMetadataFacetStream( session, repositoryId, facetClazz, 0, Long.MAX_VALUE );
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactsByDateRangeStream( RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime ) throws MetadataRepositoryException
    {
        return getArtifactsByDateRangeStream( session, repositoryId, startTime, endTime, 0, Long.MAX_VALUE );
    }

    @Override
    public MetadataFacet getMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        return getMetadataFacet( session, repositoryId, getFactoryClassForId( facetId ), name );
    }

    @Override
    public <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession session, String repositoryId, Class<T> facetClazz, long offset, long maxEntries ) throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends MetadataFacet> T getMetadataFacet( RepositorySession session, String repositoryId, Class<T> clazz, String name ) throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }



    @Override
    public Stream<ArtifactMetadata> getArtifactsByDateRangeStream( RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime, long offset, long maxEntries ) throws MetadataRepositoryException
    {
        throw new UnsupportedOperationException();
    }


    protected <T extends MetadataFacet> MetadataFacetFactory getFacetFactory(Class<T> facetClazz) {
        return metadataService.getFactory( facetClazz );
    }

    protected MetadataFacetFactory getFacetFactory(String facetId) {
        return metadataService.getFactory( facetId );
    }

    protected Set<String> getSupportedFacets() {
        return metadataService.getSupportedFacets( );
    }

    protected Class<? extends MetadataFacet> getFactoryClassForId( String facetId ) {
        return metadataService.getFactoryClassForId( facetId );
    }
}
