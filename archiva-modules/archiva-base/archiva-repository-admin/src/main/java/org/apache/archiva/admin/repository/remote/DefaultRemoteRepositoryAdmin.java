package org.apache.archiva.admin.repository.remote;
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

import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.audit.AuditEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "remoteRepositoryAdmin#default" )
public class DefaultRemoteRepositoryAdmin
    extends AbstractRepositoryAdmin
    implements RemoteRepositoryAdmin
{


    public List<RemoteRepository> getRemoteRepositories()
        throws RepositoryAdminException
    {
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        for ( RemoteRepositoryConfiguration repositoryConfiguration : getArchivaConfiguration().getConfiguration().getRemoteRepositories() )
        {
            remoteRepositories.add(
                new RemoteRepository( repositoryConfiguration.getId(), repositoryConfiguration.getName(),
                                      repositoryConfiguration.getUrl(), repositoryConfiguration.getLayout(),
                                      repositoryConfiguration.getUsername(), repositoryConfiguration.getPassword(),
                                      repositoryConfiguration.getTimeout() ) );
        }
        return remoteRepositories;
    }

    public RemoteRepository getRemoteRepository( String repositoryId )
        throws RepositoryAdminException
    {
        for ( RemoteRepository remoteRepository : getRemoteRepositories() )
        {
            if ( StringUtils.equals( repositoryId, remoteRepository.getId() ) )
            {
                return remoteRepository;
            }
        }
        return null;
    }

    public Boolean addRemoteRepository( RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        triggerAuditEvent( remoteRepository.getId(), null, AuditEvent.ADD_REMOTE_REPO, auditInformation );
        getRepositoryCommonValidator().basicValidation( remoteRepository, false );

        //TODO we can validate it's a good uri/url
        if ( StringUtils.isEmpty( remoteRepository.getUrl() ) )
        {
            throw new RepositoryAdminException( "url cannot be null" );
        }

        //MRM-752 - url needs trimming
        remoteRepository.setUrl( StringUtils.trim( remoteRepository.getUrl() ) );

        RemoteRepositoryConfiguration remoteRepositoryConfiguration =
            getRemoteRepositoryConfiguration( remoteRepository );

        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.addRemoteRepository( remoteRepositoryConfiguration );
        saveConfiguration( configuration );

        return Boolean.TRUE;
    }

    public Boolean deleteRemoteRepository( String repositoryId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        triggerAuditEvent( repositoryId, null, AuditEvent.DELETE_REMOTE_REPO, auditInformation );

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        RemoteRepositoryConfiguration remoteRepositoryConfiguration =
            configuration.getRemoteRepositoriesAsMap().get( repositoryId );
        if ( remoteRepositoryConfiguration == null )
        {
            throw new RepositoryAdminException(
                "remoteRepository with id " + repositoryId + " not exist cannot remove it" );
        }

        configuration.removeRemoteRepository( remoteRepositoryConfiguration );

        // TODO use ProxyConnectorAdmin interface ?
        // [MRM-520] Proxy Connectors are not deleted with the deletion of a Repository.
        List<ProxyConnectorConfiguration> proxyConnectors =
            new ArrayList<ProxyConnectorConfiguration>( configuration.getProxyConnectors() );
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getTargetRepoId(), repositoryId ) )
            {
                configuration.removeProxyConnector( proxyConnector );
            }
        }

        saveConfiguration( configuration );

        return Boolean.TRUE;
    }

    public Boolean updateRemoteRepository( RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        String repositoryId = remoteRepository.getId();

        triggerAuditEvent( repositoryId, null, AuditEvent.MODIFY_REMOTE_REPO, auditInformation );

        // update means : remove and add

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        RemoteRepositoryConfiguration remoteRepositoryConfiguration =
            configuration.getRemoteRepositoriesAsMap().get( repositoryId );
        if ( remoteRepositoryConfiguration == null )
        {
            throw new RepositoryAdminException(
                "remoteRepository with id " + repositoryId + " not exist cannot remove it" );
        }

        configuration.removeRemoteRepository( remoteRepositoryConfiguration );

        remoteRepositoryConfiguration = getRemoteRepositoryConfiguration( remoteRepository );
        configuration.addRemoteRepository( remoteRepositoryConfiguration );
        saveConfiguration( configuration );

        return Boolean.TRUE;
    }

    public Map<String, RemoteRepository> getRemoteRepositoriesAsMap()
        throws RepositoryAdminException
    {
        java.util.Map<String, RemoteRepository> map = new HashMap<String, RemoteRepository>();

        for ( RemoteRepository repo : getRemoteRepositories() )
        {
            map.put( repo.getId(), repo );
        }

        return map;
    }

    private RemoteRepositoryConfiguration getRemoteRepositoryConfiguration( RemoteRepository remoteRepository )
    {
        RemoteRepositoryConfiguration remoteRepositoryConfiguration = new RemoteRepositoryConfiguration();
        remoteRepositoryConfiguration.setId( remoteRepository.getId() );
        remoteRepositoryConfiguration.setPassword( remoteRepository.getPassword() );
        remoteRepositoryConfiguration.setTimeout( remoteRepository.getTimeout() );
        remoteRepositoryConfiguration.setUrl( remoteRepository.getUrl() );
        remoteRepositoryConfiguration.setUsername( remoteRepository.getUserName() );
        remoteRepositoryConfiguration.setLayout( remoteRepository.getLayout() );
        remoteRepositoryConfiguration.setName( remoteRepository.getName() );
        return remoteRepositoryConfiguration;
    }

}
