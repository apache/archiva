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
import org.apache.archiva.admin.repository.remote.RemoteRepositoryAdmin;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "remoteRepositoriesService#rest" )
public class DefaultRemoteRepositoriesService
    extends AbstractRestService
    implements RemoteRepositoriesService
{

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    public List<RemoteRepository> getRemoteRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
            for ( org.apache.archiva.admin.repository.remote.RemoteRepository remoteRepository : remoteRepositoryAdmin.getRemoteRepositories() )
            {
                RemoteRepository repo = new RemoteRepository( remoteRepository.getId(), remoteRepository.getName(),
                                                              remoteRepository.getUrl(), remoteRepository.getLayout(),
                                                              remoteRepository.getUserName(),
                                                              remoteRepository.getPassword(),
                                                              remoteRepository.getTimeout() );
                remoteRepositories.add( repo );
            }
            return remoteRepositories;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public RemoteRepository getRemoteRepository( String repositoryId )
        throws ArchivaRestServiceException
    {
        List<RemoteRepository> remoteRepositories = getRemoteRepositories();
        for ( RemoteRepository repository : remoteRepositories )
        {
            if ( StringUtils.equals( repositoryId, repository.getId() ) )
            {
                return repository;
            }
        }
        return null;
    }

    public Boolean deleteRemoteRepository( String repositoryId )
        throws Exception
    {
        return remoteRepositoryAdmin.deleteRemoteRepository( repositoryId, getAuditInformation() );
    }

    public Boolean addRemoteRepository( RemoteRepository remoteRepository )
        throws Exception
    {
        return remoteRepositoryAdmin.addRemoteRepository( getModelRemoteRepository( remoteRepository ),
                                                          getAuditInformation() );
    }

    public Boolean updateRemoteRepository( RemoteRepository remoteRepository )
        throws Exception
    {
        return remoteRepositoryAdmin.updateRemoteRepository( getModelRemoteRepository( remoteRepository ),
                                                             getAuditInformation() );
    }

    private org.apache.archiva.admin.repository.remote.RemoteRepository getModelRemoteRepository(
        RemoteRepository remoteRepository )
    {
        return new org.apache.archiva.admin.repository.remote.RemoteRepository( remoteRepository.getId(),
                                                                                remoteRepository.getName(),
                                                                                remoteRepository.getUrl(),
                                                                                remoteRepository.getLayout(),
                                                                                remoteRepository.getUserName(),
                                                                                remoteRepository.getPassword(),
                                                                                remoteRepository.getTimeOut() );
    }
}
