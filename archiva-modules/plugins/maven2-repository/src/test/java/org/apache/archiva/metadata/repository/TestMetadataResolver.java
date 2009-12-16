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

import java.util.Collection;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

public class TestMetadataResolver
    implements MetadataResolver
{
    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolverException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> getNamespaces( String repoId, String namespace )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
        throws MetadataResolverException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
