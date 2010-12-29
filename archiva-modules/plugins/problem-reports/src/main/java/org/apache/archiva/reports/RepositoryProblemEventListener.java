package org.apache.archiva.reports;

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

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataException;
import org.apache.archiva.repository.events.RepositoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process repository management events and respond appropriately.
 *
 * @plexus.component role="org.apache.archiva.repository.events.RepositoryListener" role-hint="problem-reports"
 */
public class RepositoryProblemEventListener
    implements RepositoryListener
{
    private Logger log = LoggerFactory.getLogger( RepositoryProblemEventListener.class );

    // FIXME: move to session
    public void deleteArtifact( MetadataRepository metadataRepository, String repositoryId, String namespace,
                                String project, String version, String id )
    {
        String name = RepositoryProblemFacet.createName( namespace, project, version, id );

        try
        {
            metadataRepository.removeMetadataFacet( repositoryId, RepositoryProblemFacet.FACET_ID, name );
        }
        catch ( MetadataRepositoryException e )
        {
            log.warn( "Unable to remove metadata facet as part of delete event: " + e.getMessage(), e );
        }
    }

    public void addArtifact( RepositorySession session, String repoId, String namespace, String projectId,
                             ProjectVersionMetadata metadata )
    {
        // Remove problems associated with this version on successful addition
        // TODO: this removes all problems - do we need something that just remove the problems we know are corrected?
        String name = RepositoryProblemFacet.createName( namespace, projectId, metadata.getId(), null );
        try
        {
            MetadataRepository metadataRepository = session.getRepository();
            metadataRepository.removeMetadataFacet( repoId, RepositoryProblemFacet.FACET_ID, name );
            session.markDirty();
        }
        catch ( MetadataRepositoryException e )
        {
            log.warn( "Unable to remove repository problem facets for the version being corrected in the repository: " +
                          e.getMessage(), e );
        }
    }

    public void addArtifactProblem( RepositorySession session, String repoId, String namespace, String projectId,
                                    String projectVersion, RepositoryStorageMetadataException exception )
    {
        RepositoryProblemFacet problem = new RepositoryProblemFacet();
        problem.setMessage( exception.getMessage() );
        problem.setProject( projectId );
        problem.setNamespace( namespace );
        problem.setRepositoryId( repoId );
        problem.setVersion( projectVersion );
        problem.setProblem( exception.getId() );

        try
        {
            session.getRepository().addMetadataFacet( repoId, problem );
            session.markDirty();
        }
        catch ( MetadataRepositoryException e )
        {
            log.warn( "Unable to add repository problem facets for the version being removed: " + e.getMessage(), e );
        }
    }

}