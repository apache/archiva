package org.apache.maven.archiva.web.action.admin.repositories;

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
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DeleteManagedRepositoryAction
 *
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="deleteManagedRepositoryAction" instantiation-strategy="per-lookup"
 */
public class DeleteManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable
{
    private ManagedRepositoryConfiguration repository;

    private ManagedRepositoryConfiguration stagingRepository;

    private String repoid;

    /**
     * @plexus.requirement
     */
    private RepositoryStatisticsManager repositoryStatisticsManager;

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
            this.stagingRepository = archivaConfiguration.getConfiguration().findManagedRepositoryById(
                repoid + "-stage" );
        }
    }

    public String confirmDelete()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Unable to delete managed repository: repository id was blank." );
            return ERROR;
        }

        return INPUT;
    }

    public String deleteEntry()
    {
        return deleteRepository( false );
    }

    public String deleteContents()
    {
        return deleteRepository( true );
    }

    private String deleteRepository( boolean deleteContents )
    {
        ManagedRepositoryConfiguration existingRepository = repository;
        ManagedRepositoryConfiguration attachedStagingRepo = stagingRepository;
        if ( existingRepository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        String result;

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();
            if ( attachedStagingRepo != null )
            {
                cleanupRepositoryData( attachedStagingRepo, repositorySession );
                removeRepository( repoid + "-stage", configuration );
                triggerAuditEvent( repoid + "-stage", null, AuditEvent.DELETE_MANAGED_REPO );

            }
            cleanupRepositoryData( existingRepository, repositorySession );
            removeRepository( repoid, configuration );
            triggerAuditEvent( repoid, null, AuditEvent.DELETE_MANAGED_REPO );
            result = saveConfiguration( configuration );

            if ( result.equals( SUCCESS ) )
            {
                if ( deleteContents )
                {
                    if ( attachedStagingRepo != null )
                    {
                        removeContents( attachedStagingRepo );
                    }
                    removeContents( existingRepository );
                }
            }
        }
        catch ( IOException e )
        {
            addActionError(
                "Unable to delete repository, content may already be partially removed: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RoleManagerException e )
        {
            addActionError(
                "Unable to delete repository, content may already be partially removed: " + e.getMessage() );
            result = ERROR;
        }
        catch ( MetadataRepositoryException e )
        {
            addActionError(
                "Unable to delete repository, content may already be partially removed: " + e.getMessage() );
            result = ERROR;
        }
        finally
        {
            repositorySession.close();
        }

        return result;
    }

    private void cleanupRepositoryData( ManagedRepositoryConfiguration cleanupRepository,
                                        RepositorySession repositorySession )
        throws RoleManagerException, MetadataRepositoryException
    {
        removeRepositoryRoles( cleanupRepository );
        MetadataRepository metadataRepository = repositorySession.getRepository();
        cleanupDatabase( metadataRepository, cleanupRepository.getId() );
        repositoryStatisticsManager.deleteStatistics( metadataRepository, cleanupRepository.getId() );
        // TODO: delete all content for a repository from the content API?
        repositorySession.save();

        List<ProxyConnectorConfiguration> proxyConnectors = getProxyConnectors();
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), cleanupRepository.getId() ) )
            {
                archivaConfiguration.getConfiguration().removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = archivaConfiguration.getConfiguration().getRepositoryToGroupMap();
        if ( repoToGroupMap != null )
        {
            if ( repoToGroupMap.containsKey( cleanupRepository.getId() ) )
            {
                List<String> repoGroups = repoToGroupMap.get( cleanupRepository.getId() );
                for ( String repoGroup : repoGroups )
                {
                    archivaConfiguration.getConfiguration().findRepositoryGroupById( repoGroup ).removeRepository(
                        cleanupRepository.getId() );
                }
            }
        }
    }

    private void cleanupDatabase( MetadataRepository metadataRepository, String repoId )
        throws MetadataRepositoryException
    {
        metadataRepository.removeRepository( repoId );
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }
}
