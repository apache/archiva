package org.apache.archiva.consumers.lucene.test;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.easymock.MockControl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

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
@Service( "repositorySessionFactory#test" )
public class TestRepositorySessionFactory
    implements RepositorySessionFactory
{
    private MetadataRepository repository;

    private MetadataResolver resolver;

    public RepositorySession createSession()
    {
        return new RepositorySession( null, null )
        {
            @Override
            public MetadataResolver getResolver()
            {
                return (MetadataResolver) MockControl.createControl( MetadataResolver.class );
            }

            @Override
            public void close()
            {

            }

            @Override
            public MetadataRepository getRepository()
            {
                return new MetadataRepository()
                {
                    public void updateProject( String repositoryId, ProjectMetadata project )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void updateArtifact( String repositoryId, String namespace, String projectId,
                                                String projectVersion, ArtifactMetadata artifactMeta )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void updateProjectVersion( String repositoryId, String namespace, String projectId,
                                                      ProjectVersionMetadata versionMetadata )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void updateNamespace( String repositoryId, String namespace )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public List<String> getMetadataFacets( String repositoryId, String facetId )
                        throws MetadataRepositoryException
                    {
                        return null;  //
                    }

                    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
                        throws MetadataRepositoryException
                    {
                        return null;  //
                    }

                    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void removeMetadataFacets( String repositoryId, String facetId )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void removeMetadataFacet( String repositoryId, String facetId, String name )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public List<ArtifactMetadata> getArtifactsByDateRange( String repositoryId, Date startTime,
                                                                           Date endTime )
                        throws MetadataRepositoryException
                    {
                        return null;  //
                    }

                    public Collection<String> getRepositories()
                        throws MetadataRepositoryException
                    {
                        return null;  //
                    }

                    public List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
                        throws MetadataRepositoryException
                    {
                        return null;  //
                    }

                    public void removeArtifact( String repositoryId, String namespace, String project, String version,
                                                String id )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void removeRepository( String repositoryId )
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public List<ArtifactMetadata> getArtifacts( String repositoryId )
                        throws MetadataRepositoryException
                    {
                        return null;  //
                    }

                    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                                   String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace,
                                                                                     String projectId,
                                                                                     String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<String> getRootNamespaces( String repoId )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<String> getNamespaces( String repoId, String namespace )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<String> getProjects( String repoId, String namespace )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                                      String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //
                    }

                    public void save()
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public void close()
                    {
                        //
                    }

                    public void revert()
                        throws MetadataRepositoryException
                    {
                        //
                    }

                    public boolean canObtainAccess( Class<?> aClass )
                    {
                        return false;  //
                    }

                    public Object obtainAccess( Class<?> aClass )
                    {
                        return null;  //
                    }
                };
            }
        };
    }

    public void setRepository( MetadataRepository repository )
    {
        this.repository = repository;
    }

    public void setResolver( MetadataResolver resolver )
    {
        this.resolver = resolver;
    }
}
