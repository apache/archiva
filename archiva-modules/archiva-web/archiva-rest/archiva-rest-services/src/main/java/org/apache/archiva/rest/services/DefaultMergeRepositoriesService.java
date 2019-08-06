package org.apache.archiva.rest.services;
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

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.filter.Filter;
import org.apache.archiva.filter.IncludesFilter;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.MergeRepositoriesService;
import org.apache.archiva.stagerepository.merge.Maven2RepositoryMerger;
import org.apache.archiva.stagerepository.merge.RepositoryMergerException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service ( "mergeRepositoriesService#rest" )
public class DefaultMergeRepositoriesService
    extends AbstractRestService
    implements MergeRepositoriesService
{

    // FIXME check archiva-merge-repository to sourceRepoId

    @Inject
    @Named ( value = "repositoryMerger#maven2" )
    private Maven2RepositoryMerger repositoryMerger;


    @Override
    public List<Artifact> getMergeConflictedArtifacts( String sourceRepositoryId, String targetRepositoryId )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> artifactMetadatas =
                repositoryMerger.getConflictingArtifacts( repositorySession.getRepository(), sourceRepositoryId,
                                                          targetRepositoryId );

            return buildArtifacts( artifactMetadatas, sourceRepositoryId );
        }
        catch ( RepositoryMergerException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public void mergeRepositories( String sourceRepositoryId, String targetRepositoryId, boolean skipConflicts )
        throws ArchivaRestServiceException
    {
        try
        {
            if ( skipConflicts )
            {
                mergeBySkippingConflicts( sourceRepositoryId, targetRepositoryId );
            }
            else
            {
                doMerge( sourceRepositoryId, targetRepositoryId );
            }

        }
        catch ( RepositoryMergerException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

    }


    protected void doMerge( String sourceRepositoryId, String targetRepositoryId )
        throws RepositoryMergerException, ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        try
        {
            org.apache.archiva.repository.ManagedRepository managedRepo = repositoryRegistry.getManagedRepository(targetRepositoryId);
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts(repositorySession , sourceRepositoryId );

            if ( managedRepo.getActiveReleaseSchemes().contains(ReleaseScheme.RELEASE) && !managedRepo.getActiveReleaseSchemes().contains(ReleaseScheme.SNAPSHOT) )
            {
                mergeWithOutSnapshots( metadataRepository, sourceArtifacts, sourceRepositoryId, targetRepositoryId );
            }
            else
            {
                repositoryMerger.merge( metadataRepository, sourceRepositoryId, targetRepositoryId );

                for ( ArtifactMetadata metadata : sourceArtifacts )
                {
                    triggerAuditEvent( targetRepositoryId, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
                }
            }

            doScanRepository( targetRepositoryId, false );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        } finally
        {
            repositorySession.close();
        }
    }

    public void mergeBySkippingConflicts( String sourceRepositoryId, String targetRepositoryId )
        throws RepositoryMergerException, ArchivaRestServiceException
    {

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<ArtifactMetadata> conflictSourceArtifacts =
                repositoryMerger.getConflictingArtifacts( repositorySession.getRepository(), sourceRepositoryId,
                                                          targetRepositoryId );
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts(repositorySession , sourceRepositoryId );
            sourceArtifacts.removeAll( conflictSourceArtifacts );

            org.apache.archiva.repository.ManagedRepository managedRepo = repositoryRegistry.getManagedRepository(targetRepositoryId);

            if ( managedRepo.getActiveReleaseSchemes().contains(ReleaseScheme.RELEASE) && !managedRepo.getActiveReleaseSchemes().contains(ReleaseScheme.SNAPSHOT))
            {
                mergeWithOutSnapshots( metadataRepository, sourceArtifacts, sourceRepositoryId, targetRepositoryId );
            }
            else
            {

                Filter<ArtifactMetadata> artifactsWithOutConflicts =
                    new IncludesFilter<ArtifactMetadata>( sourceArtifacts );
                repositoryMerger.merge( metadataRepository, sourceRepositoryId, targetRepositoryId,
                                        artifactsWithOutConflicts );
                for ( ArtifactMetadata metadata : sourceArtifacts )
                {
                    triggerAuditEvent( targetRepositoryId, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
                }
            }

            doScanRepository( targetRepositoryId, false );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        } finally
        {
            repositorySession.close();
        }
    }

    private void mergeWithOutSnapshots( MetadataRepository metadataRepository, List<ArtifactMetadata> sourceArtifacts,
                                        String sourceRepoId, String repoid )
        throws RepositoryMergerException
    {
        List<ArtifactMetadata> artifactsWithOutSnapshots = new ArrayList<>();
        for ( ArtifactMetadata metadata : sourceArtifacts )
        {
            if ( VersionUtil.isSnapshot( metadata.getProjectVersion() ) )
            //if ( metadata.getProjectVersion().contains( VersionUtil.SNAPSHOT ) )
            {
                artifactsWithOutSnapshots.add( metadata );
            }
            else
            {
                triggerAuditEvent( repoid, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
            }

        }
        sourceArtifacts.removeAll( artifactsWithOutSnapshots );

        Filter<ArtifactMetadata> artifactListWithOutSnapShots = new IncludesFilter<ArtifactMetadata>( sourceArtifacts );
        repositoryMerger.merge( metadataRepository, sourceRepoId, repoid, artifactListWithOutSnapShots );
    }
}
