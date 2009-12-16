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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

public class TestMetadataRepository
    extends TestMetadataResolver
    implements MetadataRepository
{
    private Map<String, MetadataFacet> facets = new HashMap<String, MetadataFacet>();

    public void updateProject( String repoId, ProjectMetadata project )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateProjectVersion( String repoId, String namespace, String projectId, ProjectVersionMetadata versionMetadata )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateProjectReference( String repoId, String namespace, String projectId, String projectVersion,
                                        ProjectVersionReference reference )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNamespace( String repoId, String namespace )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<String> getMetadataFacets( String repoId, String facetId )
    {
        return new ArrayList<String>( facets.keySet() );
    }

    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
    {
        return facets.get( name );
    }

    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
    {
        facets.put( metadataFacet.getName(), metadataFacet );
    }

    public void removeMetadataFacets( String repositoryId, String facetId )
    {
        facets.clear();
    }

    public void removeMetadataFacet( String repoId, String facetId, String name )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> getRepositories()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ArtifactMetadata> getArtifactsByChecksum( String repoId, String checksum )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteArtifact( String repositoryId, String namespace, String project, String version, String id )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteRepository( String repoId )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}