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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataResolver;

public class TestMetadataResolver
    implements MetadataResolver
{
    private Map<String, ProjectVersionMetadata> projectVersions = new HashMap<String, ProjectVersionMetadata>();

    private Map<String, List<String>> artifactVersions = new HashMap<String, List<String>>();

    private Map<String, List<ProjectVersionReference>> references =
        new HashMap<String, List<ProjectVersionReference>>();

    private List<String> namespaces;

    private Map<String, Collection<String>> projectsInNamespace = new HashMap<String, Collection<String>>();

    private Map<String, Collection<String>> versionsInProject = new HashMap<String, Collection<String>>();

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setNamespace( namespace );
        metadata.setId( projectId );
        return metadata;
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        return projectVersions.get( createMapKey( repoId, namespace, projectId, projectVersion ) );
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        List<String> versions = artifactVersions.get( createMapKey( repoId, namespace, projectId, projectVersion ) );
        return ( versions != null ? versions : Collections.<String>emptyList() );
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        return references.get( createMapKey( repoId, namespace, projectId, projectVersion ) );
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        return getNamespaces( null );
    }

    private Collection<String> getNamespaces( String baseNamespace )
    {
        Set<String> namespaces = new LinkedHashSet<String>();
        int fromIndex = baseNamespace != null ? baseNamespace.length() + 1 : 0;
        for ( String namespace : this.namespaces )
        {
            if ( baseNamespace == null || namespace.startsWith( baseNamespace + "." ) )
            {
                int i = namespace.indexOf( '.', fromIndex );
                if ( i >= 0 )
                {
                    namespaces.add( namespace.substring( fromIndex, i ) );
                }
                else
                {
                    namespaces.add( namespace.substring( fromIndex ) );
                }
            }
        }
        return namespaces;
    }

    public Collection<String> getNamespaces( String repoId, String namespace )
    {
        return getNamespaces( namespace );
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        Collection<String> list = projectsInNamespace.get( namespace );
        return list != null ? list : Collections.<String>emptyList();
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        Collection<String> list = versionsInProject.get( namespace + ":" + projectId );
        return list != null ? list : Collections.<String>emptyList();
    }

    public void setProjectVersion( String repoId, String namespace, String projectId,
                                   ProjectVersionMetadata versionMetadata )
    {
        projectVersions.put( createMapKey( repoId, namespace, projectId, versionMetadata.getId() ), versionMetadata );

        Collection<String> projects = projectsInNamespace.get( namespace );
        if ( projects == null )
        {
            projects = new LinkedHashSet<String>();
            projectsInNamespace.put( namespace, projects );
        }
        projects.add( projectId );

        String key = namespace + ":" + projectId;
        Collection<String> versions = versionsInProject.get( key );
        if ( versions == null )
        {
            versions = new LinkedHashSet<String>();
            versionsInProject.put( key, versions );
        }
        versions.add( versionMetadata.getId() );
    }

    public void setArtifactVersions( String repoId, String namespace, String projectId, String projectVersion,
                                     List<String> versions )
    {
        artifactVersions.put( createMapKey( repoId, namespace, projectId, projectVersion ), versions );
    }

    private String createMapKey( String repoId, String namespace, String projectId, String projectVersion )
    {
        return repoId + ":" + namespace + ":" + projectId + ":" + projectVersion;
    }

    public void setProjectReferences( String repoId, String namespace, String projectId, String projectVersion,
                                      List<ProjectVersionReference> references )
    {
        this.references.put( createMapKey( repoId, namespace, projectId, projectVersion ), references );
    }

    public void setNamespaces( List<String> namespaces )
    {
        this.namespaces = namespaces;
    }
}
