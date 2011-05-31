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
    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        return null;
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        return null;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        return Collections.emptyList();
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        return Collections.emptyList();
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        return Collections.emptyList();
    }

    public Collection<String> getNamespaces( String repoId, String namespace )
    {
        return Collections.emptyList();
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        return Collections.emptyList();
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        return Collections.emptyList();
    }

    public void updateProject( String repoId, ProjectMetadata project )
    {
        // no op
    }

    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
    {
        // no op
    }

    public void updateProjectVersion( String repoId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {
        // no op
    }

    public void updateNamespace( String repoId, String namespace )
    {
        // no op
    }

    public List<String> getMetadataFacets( String repodId, String facetId )
    {
        return Collections.emptyList();
    }

    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
    {
        return null;
    }

    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
    {
        // no op
    }

    public void removeMetadataFacets( String repositoryId, String facetId )
    {
        // no op
    }

    public void removeMetadataFacet( String repoId, String facetId, String name )
    {
        // no op
    }

    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
    {
        return Collections.emptyList();
    }

    public Collection<String> getRepositories()
    {
        return Collections.emptyList();
    }

    public List<ArtifactMetadata> getArtifactsByChecksum( String repoId, String checksum )
    {
        return Collections.emptyList();
    }

    public void removeArtifact( String repositoryId, String namespace, String project, String version, String id )
    {
        // no op
    }

    public void removeRepository( String repoId )
    {
        // no op
    }

    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
    {
        return Collections.emptyList();
    }

    public void save()
    {
        // no op
    }

    public void close()
    {
        // no op
    }

    public void revert()
    {
        // no op
    }

    public boolean canObtainAccess( Class<?> aClass )
    {
        return false;
    }

    public Object obtainAccess( Class<?> aClass )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ArtifactMetadata> getArtifacts( String repositoryId )
    {
        return Collections.emptyList();
    }
}
