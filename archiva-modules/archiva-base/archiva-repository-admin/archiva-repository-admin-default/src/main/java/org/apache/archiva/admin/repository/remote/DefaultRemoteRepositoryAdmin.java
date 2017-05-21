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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service("remoteRepositoryAdmin#default")
public class DefaultRemoteRepositoryAdmin
    extends AbstractRepositoryAdmin
    implements RemoteRepositoryAdmin
{

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private MavenIndexerUtils mavenIndexerUtils;

    // fields
    private List<? extends IndexCreator> indexCreators;

    private NexusIndexer indexer;

    @PostConstruct
    private void initialize()
        throws RepositoryAdminException
    {
        try
        {
            indexCreators = mavenIndexerUtils.getAllIndexCreators();
            indexer = plexusSisuBridge.lookup( NexusIndexer.class );
        }
        catch ( PlexusSisuBridgeException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        for ( RemoteRepository remoteRepository : getRemoteRepositories() )
        {
            createIndexContext( remoteRepository );
        }
    }

    @PreDestroy
    private void shutdown()
        throws RepositoryAdminException
    {
        try
        {
            List<RemoteRepository> remoteRepositories = getRemoteRepositories();
            // close index on shutdown
            for ( RemoteRepository remoteRepository : remoteRepositories )
            {
                IndexingContext context = indexer.getIndexingContexts().get( remoteRepository.getId() );
                if ( context != null )
                {
                    indexer.removeIndexingContext( context, false );
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }


    @Override
    public List<RemoteRepository> getRemoteRepositories()
        throws RepositoryAdminException
    {
        List<RemoteRepository> remoteRepositories =
            new ArrayList<>( getArchivaConfiguration().getConfiguration().getRemoteRepositories().size() );
        for ( RemoteRepositoryConfiguration repositoryConfiguration : getArchivaConfiguration().getConfiguration().getRemoteRepositories() )
        {
            RemoteRepository remoteRepository =
                new RemoteRepository( repositoryConfiguration.getId(), repositoryConfiguration.getName(),
                                      repositoryConfiguration.getUrl(), repositoryConfiguration.getLayout(),
                                      repositoryConfiguration.getUsername(), repositoryConfiguration.getPassword(),
                                      repositoryConfiguration.getTimeout() );
            remoteRepository.setDownloadRemoteIndex( repositoryConfiguration.isDownloadRemoteIndex() );
            remoteRepository.setRemoteIndexUrl( repositoryConfiguration.getRemoteIndexUrl() );
            remoteRepository.setCronExpression( repositoryConfiguration.getRefreshCronExpression() );
            remoteRepository.setIndexDirectory( repositoryConfiguration.getIndexDir() );
            remoteRepository.setRemoteDownloadNetworkProxyId(
                repositoryConfiguration.getRemoteDownloadNetworkProxyId() );
            remoteRepository.setRemoteDownloadTimeout( repositoryConfiguration.getRemoteDownloadTimeout() );
            remoteRepository.setDownloadRemoteIndexOnStartup(
                repositoryConfiguration.isDownloadRemoteIndexOnStartup() );
            remoteRepository.setDescription( repositoryConfiguration.getDescription() );
            remoteRepository.setExtraHeaders( repositoryConfiguration.getExtraHeaders() );
            remoteRepository.setExtraParameters( repositoryConfiguration.getExtraParameters() );
            remoteRepositories.add( remoteRepository );
        }
        return remoteRepositories;
    }

    @Override
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

    @Override
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

    @Override
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
        List<ProxyConnectorConfiguration> proxyConnectors = new ArrayList<>( configuration.getProxyConnectors() );
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

    @Override
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

    @Override
    public Map<String, RemoteRepository> getRemoteRepositoriesAsMap()
        throws RepositoryAdminException
    {
        java.util.Map<String, RemoteRepository> map = new HashMap<>();

        for ( RemoteRepository repo : getRemoteRepositories() )
        {
            map.put( repo.getId(), repo );
        }

        return map;
    }

    @Override
    public IndexingContext createIndexContext( RemoteRepository remoteRepository )
        throws RepositoryAdminException
    {
        try
        {
            String appServerBase = getRegistry().getString( "appserver.base" );

            String contextKey = "remote-" + remoteRepository.getId();
            IndexingContext indexingContext = indexer.getIndexingContexts().get( contextKey );
            if ( indexingContext != null )
            {
                return indexingContext;
            }
            // create remote repository path
            File repoDir = new File( appServerBase, "data/remotes/" + remoteRepository.getId() );
            if ( !repoDir.exists() )
            {
                repoDir.mkdirs();
            }

            File indexDirectory = null;

            // is there configured indexDirectory ?
            String indexDirectoryPath = remoteRepository.getIndexDirectory();

            if ( StringUtils.isNotBlank( indexDirectoryPath ) )
            {
                if ( new File( indexDirectoryPath ).isAbsolute() )
                {
                    indexDirectory = new File( indexDirectoryPath );
                }
                else
                {
                    indexDirectory = new File( repoDir, indexDirectoryPath );
                }
            }
            // if not configured use a default value
            if ( indexDirectory == null )
            {
                indexDirectory = new File( repoDir, ".index" );
            }
            if ( !indexDirectory.exists() )
            {
                indexDirectory.mkdirs();
            }
            return indexer.addIndexingContext( contextKey, remoteRepository.getId(), repoDir, indexDirectory,
                                               remoteRepository.getUrl(), calculateIndexRemoteUrl( remoteRepository ),
                                               mavenIndexerUtils.getAllIndexCreators() );
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }

    }

    protected String calculateIndexRemoteUrl( RemoteRepository remoteRepository )
    {
        if ( StringUtils.startsWith( remoteRepository.getRemoteIndexUrl(), "http" ) )
        {
            String baseUrl = remoteRepository.getRemoteIndexUrl();
            return baseUrl.endsWith( "/" ) ? StringUtils.substringBeforeLast( baseUrl, "/" ) : baseUrl;
        }
        String baseUrl = StringUtils.endsWith( remoteRepository.getUrl(), "/" ) ? StringUtils.substringBeforeLast(
            remoteRepository.getUrl(), "/" ) : remoteRepository.getUrl();

        baseUrl = StringUtils.isEmpty( remoteRepository.getRemoteIndexUrl() )
            ? baseUrl + "/.index"
            : baseUrl + "/" + remoteRepository.getRemoteIndexUrl();
        return baseUrl;

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
        remoteRepositoryConfiguration.setDownloadRemoteIndex( remoteRepository.isDownloadRemoteIndex() );
        remoteRepositoryConfiguration.setRemoteIndexUrl( remoteRepository.getRemoteIndexUrl() );
        remoteRepositoryConfiguration.setRefreshCronExpression( remoteRepository.getCronExpression() );
        remoteRepositoryConfiguration.setIndexDir( remoteRepository.getIndexDirectory() );
        remoteRepositoryConfiguration.setRemoteDownloadNetworkProxyId(
            remoteRepository.getRemoteDownloadNetworkProxyId() );
        remoteRepositoryConfiguration.setRemoteDownloadTimeout( remoteRepository.getRemoteDownloadTimeout() );
        remoteRepositoryConfiguration.setDownloadRemoteIndexOnStartup(
            remoteRepository.isDownloadRemoteIndexOnStartup() );
        remoteRepositoryConfiguration.setDescription( remoteRepository.getDescription() );
        remoteRepositoryConfiguration.setExtraHeaders( remoteRepository.getExtraHeaders() );
        remoteRepositoryConfiguration.setExtraParameters( remoteRepository.getExtraParameters() );
        return remoteRepositoryConfiguration;
    }

}
