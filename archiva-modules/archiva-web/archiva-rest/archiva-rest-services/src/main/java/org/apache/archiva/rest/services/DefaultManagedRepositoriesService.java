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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.RepositoryCommonValidator;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.rest.api.model.ArchivaRepositoryStatistics;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "managedRepositoriesService#rest" )
public class DefaultManagedRepositoriesService
    extends AbstractRestService
    implements ManagedRepositoriesService
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private RepositoryCommonValidator repositoryCommonValidator;

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    @Named( value = "repositorySessionFactory" )
    protected RepositorySessionFactory repositorySessionFactory;


    public List<ManagedRepository> getManagedRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            List<org.apache.archiva.admin.model.beans.ManagedRepository> repos =
                managedRepositoryAdmin.getManagedRepositories();
            return repos == null ? Collections.<ManagedRepository>emptyList() : repos;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public ManagedRepository getManagedRepository( String repositoryId )
        throws ArchivaRestServiceException
    {
        List<ManagedRepository> repos = getManagedRepositories();
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( repo.getId(), repositoryId ) )
            {
                return repo;
            }
        }
        return null;
    }


    public Boolean deleteManagedRepository( String repoId, boolean deleteContent )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.deleteManagedRepository( repoId, getAuditInformation(), deleteContent );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName() );
        }
    }

    public ManagedRepository addManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException
    {

        try
        {
            boolean res =
                managedRepositoryAdmin.addManagedRepository( managedRepository, managedRepository.isStageRepoNeeded(),
                                                             getAuditInformation() );
            if ( res )
            {
                return getManagedRepository( managedRepository.getId() );
            }
            throw new ArchivaRestServiceException( "fail to created managed Repository" );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName() );
        }
    }


    public Boolean updateManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.updateManagedRepository( managedRepository,
                                                                   managedRepository.isStageRepoNeeded(),
                                                                   getAuditInformation(),
                                                                   managedRepository.isResetStats() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName() );
        }
    }

    public Boolean fileLocationExists( String fileLocation )
        throws ArchivaRestServiceException
    {
        String location = repositoryCommonValidator.removeExpressions( fileLocation );
        return new File( location ).exists();
    }

    public ArchivaRepositoryStatistics getManagedRepositoryStatistics( String repositoryId )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();

            RepositoryStatistics stats = null;
            try
            {
                stats = repositoryStatisticsManager.getLastStatistics( metadataRepository, repositoryId );
            }
            catch ( MetadataRepositoryException e )
            {
                log.warn( "Error retrieving repository statistics: " + e.getMessage(), e );
            }
            if ( stats != null )
            {
                ArchivaRepositoryStatistics archivaRepositoryStatistics =
                    new BeanReplicator().replicateBean( stats, ArchivaRepositoryStatistics.class );
                archivaRepositoryStatistics.setDuration( archivaRepositoryStatistics.getScanEndTime().getTime()
                                                             - archivaRepositoryStatistics.getScanStartTime().getTime() );
                return archivaRepositoryStatistics;
            }

        }
        finally
        {
            repositorySession.close();
        }
        return null;
    }
}
