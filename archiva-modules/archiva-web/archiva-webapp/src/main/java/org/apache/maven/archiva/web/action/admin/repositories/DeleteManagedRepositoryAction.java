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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsByRepositoryConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryContentStatisticsByRepositoryConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.audit.AuditEvent;

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

    private String repoid;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO archivaDAO;

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
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
        if ( existingRepository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        String result = SUCCESS;

        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();
            cleanupRepositoryData( existingRepository );
            removeRepository( repoid, configuration );
            triggerAuditEvent( repoid, null, AuditEvent.DELETE_MANAGED_REPO );
            result = saveConfiguration( configuration );

            if ( result.equals( SUCCESS ) )
            {
                if ( deleteContents )
                {
                    removeContents( existingRepository );
                }
            }
        }
        catch ( IOException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }
        catch ( ArchivaDatabaseException e )
        {
            addActionError( "Unable to delete repositoy: " + e.getMessage() );
            result = ERROR;
        }

        return result;
    }

    private void cleanupRepositoryData( ManagedRepositoryConfiguration cleanupRepository )
        throws RoleManagerException, ArchivaDatabaseException
    {
        removeRepositoryRoles( cleanupRepository );
        cleanupDatabase( cleanupRepository.getId() );
        cleanupScanStats( cleanupRepository.getId() );
        
        List<ProxyConnectorConfiguration> proxyConnectors = getProxyConnectors();
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), cleanupRepository.getId() ) )
            {
                archivaConfiguration.getConfiguration().removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = archivaConfiguration.getConfiguration().getRepositoryToGroupMap();
        if( repoToGroupMap != null )
        {
            if( repoToGroupMap.containsKey( cleanupRepository.getId() ) )
            {
                List<String> repoGroups = repoToGroupMap.get( cleanupRepository.getId() );
                for( String repoGroup : repoGroups )
                {
                    archivaConfiguration.getConfiguration().findRepositoryGroupById( repoGroup ).removeRepository( cleanupRepository.getId() );
                }
            }            
        }
    }

    private void cleanupDatabase( String repoId )
        throws ArchivaDatabaseException
    {
        Constraint constraint = new ArtifactsByRepositoryConstraint( repoId );

        List<ArchivaArtifact> artifacts = archivaDAO.getArtifactDAO().queryArtifacts( constraint );

        for ( ArchivaArtifact artifact : artifacts )
        {
            log.info( "Removing artifact " + artifact + " from the database." );
            try
            {
                archivaDAO.getArtifactDAO().deleteArtifact( artifact );

                ArchivaProjectModel projectModel =
                    archivaDAO.getProjectModelDAO().getProjectModel( artifact.getGroupId(), artifact.getArtifactId(),
                                                                     artifact.getVersion() );

                archivaDAO.getProjectModelDAO().deleteProjectModel( projectModel );
            }
            catch ( ObjectNotFoundException oe )
            {
                log.info( "Project model of artifact " + artifact + " does not exist in the database. " +
                                      "Moving on to the next artifact." );
            }
            catch ( ArchivaDatabaseException ae )
            {
                log.info( "Unable to delete artifact " + artifact + " from the database. " +
                                      "Moving on to the next artifact." );
            }
        }
    }

    private void cleanupScanStats( String repoId )
        throws ArchivaDatabaseException
    {
        List<RepositoryContentStatistics> results =
            archivaDAO.getRepositoryContentStatisticsDAO().queryRepositoryContentStatistics(
            new RepositoryContentStatisticsByRepositoryConstraint( repoId ) );

        for ( RepositoryContentStatistics stats : results )
        {
            archivaDAO.getRepositoryContentStatisticsDAO().deleteRepositoryContentStatistics( stats );
        }
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

    public void setArchivaDAO( ArchivaDAO archivaDAO )
    {
        this.archivaDAO = archivaDAO;
    }

    public ArchivaDAO getArchivaDAO()
    {
        return archivaDAO;
    }
}
