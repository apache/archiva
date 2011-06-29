package org.apache.maven.archiva.web.action;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.filter.IncludesFilter;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.stagerepository.merge.Maven2RepositoryMerger;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * plexus.component role="com.opensymphony.xwork2.Action" role-hint="mergeAction" instantiation-strategy="per-lookup"
 */
@Controller( "mergeAction" )
@Scope( "prototype" )
public class MergeAction
    extends AbstractActionSupport
    implements Validateable, Preparable, Auditable

{
    /**
     * plexus.requirement role="org.apache.archiva.stagerepository.merge.RepositoryMerger" role-hint="maven2"
     */
    @Inject
    @Named( value = "repositoryMerger#maven2" )
    private Maven2RepositoryMerger repositoryMerger;

    /**
     * plexus.requirement
     */
    @Inject
    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    private ManagedRepositoryConfiguration repository;

    private String repoid;

    private String sourceRepoId;

    private final String action = "merge";

    private final String hasConflicts = "CONFLICTS";

    private List<ArtifactMetadata> conflictSourceArtifacts;

    private List<ArtifactMetadata> conflictSourceArtifactsToBeDisplayed;

    public String getConflicts()
    {
        sourceRepoId = repoid + "-stage";
        Configuration config = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration targetRepoConfig = config.findManagedRepositoryById( sourceRepoId );

        if ( targetRepoConfig != null )
        {
            return hasConflicts;

        }
        else
        {
            return ERROR;
        }
    }

    public String doMerge()
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts( sourceRepoId );

            if ( repository.isReleases() && !repository.isSnapshots() )
            {
                mergeWithOutSnapshots( metadataRepository, sourceArtifacts, sourceRepoId, repoid );
            }
            else
            {
                repositoryMerger.merge( metadataRepository, sourceRepoId, repoid );

                for ( ArtifactMetadata metadata : sourceArtifacts )
                {
                    triggerAuditEvent( repoid, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
                }
            }

            scanRepository();

            addActionMessage( "Repository '" + sourceRepoId + "' successfully merged to '" + repoid + "'." );

            return SUCCESS;
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Error occurred while merging the repositories: " + e.getMessage() );
            return ERROR;
        }
        finally
        {
            repositorySession.close();
        }
    }

    public String mergeBySkippingConflicts()
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts( sourceRepoId );
            sourceArtifacts.removeAll( conflictSourceArtifacts );

            if ( repository.isReleases() && !repository.isSnapshots() )
            {
                mergeWithOutSnapshots( metadataRepository, sourceArtifacts, sourceRepoId, repoid );
            }
            else
            {

                Filter<ArtifactMetadata> artifactsWithOutConflicts =
                    new IncludesFilter<ArtifactMetadata>( sourceArtifacts );
                repositoryMerger.merge( metadataRepository, sourceRepoId, repoid, artifactsWithOutConflicts );
                for ( ArtifactMetadata metadata : sourceArtifacts )
                {
                    triggerAuditEvent( repoid, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
                }
            }

            scanRepository();

            addActionMessage( "Repository '" + sourceRepoId + "' successfully merged to '" + repoid + "'." );

            return SUCCESS;
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Error occurred while merging the repositories: " + e.getMessage() );
            return ERROR;
        }
        finally
        {
            repositorySession.close();
        }
    }

    public String mergeWithOutConlficts()
    {
        sourceRepoId = repoid + "-stage";

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            conflictSourceArtifacts =
                repositoryMerger.getConflictingArtifacts( repositorySession.getRepository(), sourceRepoId, repoid );
        }
        catch ( Exception e )
        {
            addActionError( "Error occurred while merging the repositories." );
            return ERROR;
        }
        finally
        {
            repositorySession.close();
        }

        addActionMessage( "Repository '" + sourceRepoId + "' successfully merged to '" + repoid + "'." );

        return SUCCESS;
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public void prepare()
        throws Exception
    {
        sourceRepoId = repoid + "-stage";
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            conflictSourceArtifacts =
                repositoryMerger.getConflictingArtifacts( repositorySession.getRepository(), sourceRepoId, repoid );
        }
        finally
        {
            repositorySession.close();
        }

        Configuration config = archivaConfiguration.getConfiguration();
        this.repository = config.findManagedRepositoryById( repoid );
        setConflictSourceArtifactsToBeDisplayed( conflictSourceArtifacts );
    }

    public String getSourceRepoId()
    {
        return sourceRepoId;
    }

    public void setSourceRepoId( String sourceRepoId )
    {
        this.sourceRepoId = sourceRepoId;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    public List<ArtifactMetadata> getConflictSourceArtifacts()
    {
        return conflictSourceArtifacts;
    }

    public void setConflictSourceArtifacts( List<ArtifactMetadata> conflictSourceArtifacts )
    {
        this.conflictSourceArtifacts = conflictSourceArtifacts;
    }

    public List<ArtifactMetadata> getConflictSourceArtifactsToBeDisplayed()
    {
        return conflictSourceArtifactsToBeDisplayed;
    }

    public void setConflictSourceArtifactsToBeDisplayed( List<ArtifactMetadata> conflictSourceArtifacts )
        throws Exception
    {
        this.conflictSourceArtifactsToBeDisplayed = new ArrayList<ArtifactMetadata>();
        HashMap<String, ArtifactMetadata> map = new HashMap<String, ArtifactMetadata>();
        for ( ArtifactMetadata metadata : conflictSourceArtifacts )
        {
            String metadataId =
                metadata.getNamespace() + metadata.getProject() + metadata.getProjectVersion() + metadata.getVersion();
            map.put( metadataId, metadata );
        }
        conflictSourceArtifactsToBeDisplayed.addAll( map.values() );
    }

    private void mergeWithOutSnapshots( MetadataRepository metadataRepository, List<ArtifactMetadata> sourceArtifacts,
                                        String sourceRepoId, String repoid )
        throws Exception
    {
        List<ArtifactMetadata> artifactsWithOutSnapshots = new ArrayList<ArtifactMetadata>();
        for ( ArtifactMetadata metadata : sourceArtifacts )
        {

            if ( metadata.getProjectVersion().contains( "SNAPSHOT" ) )
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

    private void scanRepository()
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoid );
        task.setScanAll( true );

        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repoid ) )
        {
            log.info( "Repository [" + repoid + "] task was already queued." );
        }
        else
        {
            try
            {
                log.info( "Your request to have repository [" + repoid + "] be indexed has been queued." );
                repositoryTaskScheduler.queueTask( task );
            }
            catch ( TaskQueueException e )
            {
                log.warn(
                    "Unable to queue your request to have repository [" + repoid + "] be indexed: " + e.getMessage() );
            }
        }
    }
}