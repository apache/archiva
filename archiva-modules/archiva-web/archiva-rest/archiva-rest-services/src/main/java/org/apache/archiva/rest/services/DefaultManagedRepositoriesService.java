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

import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepositoryAdmin;
import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "managedRepositoriesService#rest" )
public class DefaultManagedRepositoriesService
    extends AbstractRestService
    implements ManagedRepositoriesService
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    public List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {
        List<org.apache.archiva.admin.repository.managed.ManagedRepository> repos =
            managedRepositoryAdmin.getManagedRepositories();

        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>( repos.size() );

        for ( org.apache.archiva.admin.repository.managed.ManagedRepository repoConfig : repos )
        {
            // TODO staging repo too
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                       repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases(),
                                       repoConfig.isBlockRedeployments(), false, repoConfig.getCronExpression() );
            managedRepos.add( repo );
        }

        return managedRepos;
    }

    public ManagedRepository getManagedRepository( String repositoryId )
        throws RepositoryAdminException
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
        throws Exception
    {

        return managedRepositoryAdmin.deleteManagedRepository( repoId, getAuditInformation(), deleteContent );
    }

    public Boolean addManagedRepository( ManagedRepository managedRepository )
        throws Exception
    {
        org.apache.archiva.admin.repository.managed.ManagedRepository repo =
            new org.apache.archiva.admin.repository.managed.ManagedRepository();
        repo.setLocation( managedRepository.getLocation() );
        repo.setBlockRedeployments( managedRepository.isBlockRedeployments() );
        repo.setCronExpression( managedRepository.getCronExpression() );
        repo.setId( managedRepository.getId() );
        repo.setLayout( managedRepository.getLayout() );
        repo.setName( managedRepository.getName() );
        repo.setReleases( managedRepository.isReleases() );
        repo.setSnapshots( managedRepository.isSnapshots() );
        return managedRepositoryAdmin.addManagedRepository( repo, managedRepository.isStageRepoNeeded(),
                                                            getAuditInformation() );
    }


    public Boolean updateManagedRepository( ManagedRepository managedRepository )
        throws Exception
    {
        org.apache.archiva.admin.repository.managed.ManagedRepository repo =
            new org.apache.archiva.admin.repository.managed.ManagedRepository();
        repo.setLocation( managedRepository.getLocation() );
        repo.setBlockRedeployments( managedRepository.isBlockRedeployments() );
        repo.setCronExpression( managedRepository.getCronExpression() );
        repo.setId( managedRepository.getId() );
        repo.setLayout( managedRepository.getLayout() );
        repo.setName( managedRepository.getName() );
        repo.setReleases( managedRepository.isReleases() );
        repo.setSnapshots( managedRepository.isSnapshots() );
        return managedRepositoryAdmin.updateManagedRepository( repo, managedRepository.isStageRepoNeeded(),
                                                               getAuditInformation(),
                                                               managedRepository.isResetStats() );
    }

}
