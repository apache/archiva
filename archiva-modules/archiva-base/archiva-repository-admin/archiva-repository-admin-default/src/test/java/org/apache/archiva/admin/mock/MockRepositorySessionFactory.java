package org.apache.archiva.admin.mock;
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
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "RepositorySessionFactory#mock" )
public class MockRepositorySessionFactory
    implements RepositorySessionFactory
{
    public RepositorySession createSession( )
    {
        return new RepositorySession( null, null )
        {
            @Override
            public void close( )
            {
                return;
            }

            @Override
            public void save( )
            {
                // no op
            }

            @Override
            public MetadataRepository getRepository( )
            {
                return new MetadataRepository( )
                {
                    public void updateProject( String repositoryId, ProjectMetadata project )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void updateArtifact( String repositoryId, String namespace, String projectId,
                                                String projectVersion, ArtifactMetadata artifactMeta )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void updateProjectVersion( String repositoryId, String namespace, String projectId,
                                                      ProjectVersionMetadata versionMetadata )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void updateNamespace( String repositoryId, String namespace )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public List<String> getMetadataFacets( String repositoryId, String facetId )
                        throws MetadataRepositoryException
                    {
                        return Collections.emptyList( );
                    }

                    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
                        throws MetadataRepositoryException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void removeMetadataFacets( String repositoryId, String facetId )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void removeMetadataFacet( String repositoryId, String facetId, String name )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public List<ArtifactMetadata> getArtifactsByDateRange( String repositoryId, Date startTime,
                                                                           Date endTime )
                        throws MetadataRepositoryException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getRepositories( )
                        throws MetadataRepositoryException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
                        throws MetadataRepositoryException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void removeArtifact( String repositoryId, String namespace, String project, String version,
                                                String id )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void removeRepository( String repositoryId )
                        throws MetadataRepositoryException
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public List<ArtifactMetadata> getArtifacts( String repositoryId )
                        throws MetadataRepositoryException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                                   String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace,
                                                                                     String projectId,
                                                                                     String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getRootNamespaces( String repoId )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getNamespaces( String repoId, String namespace )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getProjects( String repoId, String namespace )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                                      String projectVersion )
                        throws MetadataResolutionException
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void save( )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void close( )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void revert( )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean canObtainAccess( Class<?> aClass )
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Object obtainAccess( Class<?> aClass )
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                };
            }
        };
    }
}
