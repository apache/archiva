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
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.springframework.stereotype.Service;

import java.util.Collection;

// FIXME: remove - this is useless, better to mock it or avoid needing it
@Service( "metadataResolver#test" )
public class TestMetadataResolver
    implements MetadataResolver
{
    public ProjectVersionMetadata resolveProjectVersion( RepositorySession session, String repoId, String namespace,
                                                         String projectId, String projectVersion )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<ProjectVersionReference> resolveProjectReferences( RepositorySession session, String repoId,
                                                                         String namespace, String projectId,
                                                                         String projectVersion )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> resolveRootNamespaces( RepositorySession session, String repoId )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> resolveNamespaces( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> resolveProjects( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<String> resolveProjectVersions( RepositorySession session, String repoId, String namespace,
                                                      String projectId )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<ArtifactMetadata> resolveArtifacts( RepositorySession session, String repoId, String namespace,
                                                          String projectId, String projectVersion )
        throws MetadataResolutionException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
