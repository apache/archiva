package org.apache.archiva.metadata.repository.stats.model;

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
import org.apache.archiva.metadata.maven.model.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.RepositorySession;

import java.util.Collection;

/**
 *
 * This is a default implementation of a statistics provider that walks the tree and
 * counts the artifacts found during the walk.
 * The implementation is not very fast. If metadata store provider can improve the
 * process by using store specific techniques (like query language) they should provide
 * their own implementation.
 *
 * @author Martin Stockhammer
 */
public class RepositoryWalkingStatisticsProvider implements RepositoryStatisticsProvider
{

    /**
     * Walks each namespace of the given repository id and counts the artifacts.
     *
     *
     * @param repositorySession
     * @param metadataRepository The repository implementation
     * @param repositoryId The repository Id
     * @param repositoryStatistics The statistics object that must be populated
     * @throws MetadataRepositoryException Throws the repository exception, if an error occurs while accessing the repository.
     */
    @Override
    public void populateStatistics( RepositorySession repositorySession, MetadataRepository metadataRepository, String repositoryId,
                                    RepositoryStatistics repositoryStatistics )
        throws MetadataRepositoryException
    {
        try
        {
            for ( String ns : metadataRepository.getRootNamespaces( repositorySession, repositoryId ) )
            {
                walkRepository( repositorySession, metadataRepository, repositoryStatistics, repositoryId, ns );
            }
        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private void walkRepository( RepositorySession repositorySession, MetadataRepository metadataRepository, RepositoryStatistics stats, String repositoryId,
                                 String ns )
        throws MetadataResolutionException
    {
        for ( String namespace : metadataRepository.getChildNamespaces( repositorySession , repositoryId, ns ) )
        {
            walkRepository( repositorySession, metadataRepository, stats, repositoryId, ns + "." + namespace );
        }

        Collection<String> projects = metadataRepository.getProjects( repositorySession , repositoryId, ns );
        if ( !projects.isEmpty() )
        {
            stats.setTotalGroupCount( stats.getTotalGroupCount() + 1 );
            stats.setTotalProjectCount( stats.getTotalProjectCount() + projects.size() );

            for ( String project : projects )
            {
                for ( String version : metadataRepository.getProjectVersions( repositorySession , repositoryId, ns, project ) )
                {
                    for ( ArtifactMetadata artifact : metadataRepository.getArtifacts( repositorySession , repositoryId, ns,
                        project, version ) )
                    {
                        stats.setTotalArtifactCount( stats.getTotalArtifactCount() + 1 );
                        stats.setTotalArtifactFileSize( stats.getTotalArtifactFileSize() + artifact.getSize() );

                        MavenArtifactFacet facet =
                            (MavenArtifactFacet) artifact.getFacet( MavenArtifactFacet.FACET_ID );
                        if ( facet != null )
                        {
                            String type = facet.getType();
                            stats.setTotalCountForType( type, stats.getTotalCountForType( type ) + 1 );
                        }
                    }
                }
            }
        }
    }
}
