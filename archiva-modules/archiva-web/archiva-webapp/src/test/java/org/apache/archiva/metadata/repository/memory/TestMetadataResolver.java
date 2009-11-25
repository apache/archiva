package org.apache.archiva.metadata.repository.memory;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;

public class TestMetadataResolver
    implements MetadataResolver
{
    private Map<String, ProjectBuildMetadata> projectBuilds = new HashMap<String, ProjectBuildMetadata>();

    private Map<String, List<String>> artifactVersions = new HashMap<String, List<String>>();

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setNamespace( namespace );
        metadata.setId( projectId );
        return metadata;
    }

    public ProjectBuildMetadata getProjectBuild( String repoId, String namespace, String projectId, String buildId )
    {
        return projectBuilds.get( createMapKey( repoId, namespace, projectId, buildId ) );
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId, String buildId )
    {
        List<String> versions = artifactVersions.get( createMapKey( repoId, namespace, projectId, buildId ) );
        return ( versions != null ? versions : Collections.<String>emptyList() );
    }

    public void setProjectBuild( String repoId, String namespace, String projectId, ProjectBuildMetadata build )
    {
        projectBuilds.put( createMapKey( repoId, namespace, projectId, build.getId() ), build );
    }

    public void setArtifactVersions( String repoId, String namespace, String projectId, String version,
                                     List<String> versions )
    {
        artifactVersions.put( createMapKey( repoId, namespace, projectId, version ), versions );
    }

    private String createMapKey( String repoId, String namespace, String projectId, String buildId )
    {
        return repoId + ":" + namespace + ":" + projectId + ":" + buildId;
    }
}
