package org.apache.archiva.web.action;

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
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
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
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Controller( "mergeAction" )
@Scope( "prototype" )
public class MergeAction
    extends AbstractActionSupport
    implements Validateable, Preparable, Auditable
{

    @Inject
    @Named( value = "repositoryMerger#maven2" )
    private Maven2RepositoryMerger repositoryMerger;

    @Inject
    protected ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    private ManagedRepository repository;

    private String repoid;

    private Collection<ArtifactMetadata> conflictSourceArtifactsToBeDisplayed;

    private static String SESSION_KEY = "default";

    public String requestMerge()
        throws Exception
    {
        if ( !repository.isStagingRequired() )
        {
            addActionError( "Repository [" + repository.getId() + "] is not configured for staging" );
            return ERROR;
        }

        // check for conflicts to display
        HashMap<String, ArtifactMetadata> map = new LinkedHashMap<String, ArtifactMetadata>();
        for ( ArtifactMetadata metadata : getConflictSourceArtifacts() )
        {
            String metadataId = metadata.getNamespace() + ":" + metadata.getProject() + ":" + metadata.getVersion();
            map.put( metadataId, metadata );
        }
        conflictSourceArtifactsToBeDisplayed = map.values();

        return "confirm";
    }

    public String doMerge()
    {
        return merge( true );
    }

    public String mergeBySkippingConflicts()
    {
        return merge( false );
    }

    private String merge( boolean overwriteConflicts )
    {
        // FIXME: stage repo should only need the repoid
        String sourceRepoId = null;

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts( sourceRepoId );
            if ( !overwriteConflicts )
            {
                sourceArtifacts.removeAll( getConflictSourceArtifacts() );

                Filter<ArtifactMetadata> artifactsWithOutConflicts = new IncludesFilter<ArtifactMetadata>(
                    sourceArtifacts );
                repositoryMerger.merge( metadataRepository, sourceRepoId, repoid, artifactsWithOutConflicts );
            }
            else
            {
                repositoryMerger.merge( metadataRepository, sourceRepoId, repoid );
            }

            // FIXME: this should happen in the merge itself
            for ( ArtifactMetadata metadata : sourceArtifacts )
            {
                triggerAuditEvent( repoid, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
            }

            // FIXME: this should happen in the merge itself, don't re-scan the whole thing. Make sure we test the
            //   results
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

    public ManagedRepository getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepository repository )
    {
        this.repository = repository;
    }

    public void prepare()
        throws Exception
    {
        this.repository = managedRepositoryAdmin.getManagedRepository( repoid );
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
        throws Exception
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            return repositoryMerger.getConflictingArtifacts( repositorySession.getRepository(), repoid );
        }
        finally
        {
            repositorySession.close();
        }
    }

    public Collection<ArtifactMetadata> getConflictSourceArtifactsToBeDisplayed()
    {
        return conflictSourceArtifactsToBeDisplayed;
    }

    private Filter<ArtifactMetadata> filterOutSnapshots( List<ArtifactMetadata> sourceArtifacts, String repoid )
    {
        for ( Iterator<ArtifactMetadata> i = sourceArtifacts.iterator(); i.hasNext(); )
        {
            ArtifactMetadata metadata = i.next();
            if ( metadata.getProjectVersion().contains( "SNAPSHOT" ) )
            {
                i.remove();
            }
            else
            {
                triggerAuditEvent( repoid, metadata.getId(), AuditEvent.MERGING_REPOSITORIES );
            }
        }
        return new IncludesFilter<ArtifactMetadata>( sourceArtifacts );
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

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }
}
