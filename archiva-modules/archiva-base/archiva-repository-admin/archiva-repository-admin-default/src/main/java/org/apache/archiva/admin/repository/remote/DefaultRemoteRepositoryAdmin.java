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
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryCheckPath;
import org.apache.archiva.indexer.UnsupportedBaseContextException;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.PasswordCredentials;
import org.apache.archiva.repository.RepositoryCredentials;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index_shaded.lucene.index.IndexFormatTooOldException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    RepositoryRegistry repositoryRegistry;

    @Inject
    private List<? extends IndexCreator> indexCreators;

    @Inject
    private NexusIndexer indexer;

    @PostConstruct
    private void initialize()
        throws RepositoryAdminException
    {
        for ( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository : getRemoteRepositories() )
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
            List<org.apache.archiva.admin.model.beans.RemoteRepository> remoteRepositories = getRemoteRepositories();
            // close index on shutdown
            for ( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository : remoteRepositories )
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


    /*
 * Conversion between the repository from the registry and the serialized DTO for the admin API
 */
    private org.apache.archiva.admin.model.beans.RemoteRepository convertRepo( RemoteRepository repo ) {
        if (repo==null) {
            return null;
        }
        org.apache.archiva.admin.model.beans.RemoteRepository adminRepo = new org.apache.archiva.admin.model.beans.RemoteRepository( getArchivaConfiguration().getDefaultLocale() );
        setBaseRepoAttributes( adminRepo, repo );
        adminRepo.setUrl( convertUriToString( repo.getLocation() ));
        adminRepo.setCronExpression( repo.getSchedulingDefinition() );
        adminRepo.setCheckPath( repo.getCheckPath() );
        adminRepo.setExtraHeaders( repo.getExtraHeaders() );
        adminRepo.setExtraParameters( repo.getExtraParameters() );
        adminRepo.setTimeout( (int) repo.getTimeout().getSeconds() );
        RepositoryCredentials creds = repo.getLoginCredentials();
        if (creds!=null && creds instanceof PasswordCredentials) {
            PasswordCredentials pCreds = (PasswordCredentials) creds;
            adminRepo.setUserName( pCreds.getUsername() );
            adminRepo.setPassword( new String(pCreds.getPassword()!=null ? pCreds.getPassword() : new char[0]) );
        }
        if (repo.supportsFeature( RemoteIndexFeature.class )) {
            RemoteIndexFeature rif = repo.getFeature( RemoteIndexFeature.class ).get();
            adminRepo.setRemoteIndexUrl( convertUriToString( rif.getIndexUri() ) );
            adminRepo.setDownloadRemoteIndex( rif.isDownloadRemoteIndex() );
            adminRepo.setRemoteDownloadNetworkProxyId( rif.getProxyId() );
            adminRepo.setDownloadRemoteIndexOnStartup( rif.isDownloadRemoteIndexOnStartup() );
            adminRepo.setRemoteDownloadTimeout( (int) rif.getDownloadTimeout().getSeconds() );
        }
        return adminRepo;
    }

    private RemoteRepositoryConfiguration getRepositoryConfiguration( org.apache.archiva.admin.model.beans.RemoteRepository repo) {
        RemoteRepositoryConfiguration repoConfig = new RemoteRepositoryConfiguration();
        setBaseRepoAttributes( repoConfig, repo );
        repoConfig.setUrl( getRepositoryCommonValidator().removeExpressions( repo.getUrl() ) );
        repoConfig.setRefreshCronExpression( repo.getCronExpression() );
        repoConfig.setCheckPath( repo.getCheckPath() );
        repoConfig.setExtraHeaders( repo.getExtraHeaders() );
        repoConfig.setExtraParameters( repo.getExtraParameters() );
        repoConfig.setUsername( repo.getUserName() );
        repoConfig.setPassword( repo.getPassword() );
        repoConfig.setTimeout( repo.getTimeout() );
        repoConfig.setRemoteIndexUrl( repo.getRemoteIndexUrl() );
        repoConfig.setDownloadRemoteIndex( repo.isDownloadRemoteIndex() );
        repoConfig.setRemoteDownloadNetworkProxyId( repo.getRemoteDownloadNetworkProxyId() );
        repoConfig.setDownloadRemoteIndexOnStartup( repo.isDownloadRemoteIndexOnStartup() );
        repoConfig.setRemoteDownloadTimeout( repo.getRemoteDownloadTimeout() );
        return repoConfig;
    }

    @Override
    public List<org.apache.archiva.admin.model.beans.RemoteRepository> getRemoteRepositories()
        throws RepositoryAdminException
    {

        return repositoryRegistry.getRemoteRepositories().stream().map( repo -> convertRepo( repo ) ).collect( Collectors.toList());
    }

    @Override
    public org.apache.archiva.admin.model.beans.RemoteRepository getRemoteRepository( String repositoryId )
        throws RepositoryAdminException
    {
        return convertRepo( repositoryRegistry.getRemoteRepository( repositoryId ));
    }

    @Override
    public Boolean addRemoteRepository( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository, AuditInformation auditInformation )
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
        //MRM-1940 - URL should not end with a slash
        remoteRepository.setUrl( StringUtils.stripEnd(StringUtils.trim( remoteRepository.getUrl() ), "/"));

        if (StringUtils.isEmpty(remoteRepository.getCheckPath())) {
            String checkUrl = remoteRepository.getUrl().toLowerCase();
            for (RepositoryCheckPath path : getArchivaConfiguration ().getConfiguration().getArchivaDefaultConfiguration().getDefaultCheckPaths()) {
                log.debug("Checking path for urls: {} <-> {}", checkUrl, path.getUrl());
                if (checkUrl.startsWith(path.getUrl())) {
                    remoteRepository.setCheckPath(path.getPath());
                    break;
                }
            }
        }

        Configuration configuration = getArchivaConfiguration().getConfiguration();
        RemoteRepositoryConfiguration remoteRepositoryConfiguration =
            getRepositoryConfiguration( remoteRepository );

        try
        {
            repositoryRegistry.putRepository( remoteRepositoryConfiguration, configuration );
        }
        catch ( RepositoryException e )
        {
            log.error("Could not add remote repository {}: {}", remoteRepositoryConfiguration.getId(), e.getMessage(), e);
            throw new RepositoryAdminException( "Adding of remote repository failed"+(e.getMessage()==null?"":": "+e.getMessage()) );

        }

        saveConfiguration( configuration );

        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteRemoteRepository( String repositoryId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        triggerAuditEvent( repositoryId, null, AuditEvent.DELETE_REMOTE_REPO, auditInformation );

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        RemoteRepository repo = repositoryRegistry.getRemoteRepository( repositoryId );
        if (repo==null) {
            throw new RepositoryAdminException( "Could not delete repository "+repositoryId+". The repository does not exist." );
        }
        try
        {
            repositoryRegistry.removeRepository( repo, configuration );
        }
        catch ( RepositoryException e )
        {
            log.error("Deletion of remote repository failed {}: {}", repo.getId(), e.getMessage(), e);
            throw new RepositoryAdminException( "Could not delete remote repository"+(e.getMessage()==null?"":": "+e.getMessage()) );
        }

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
    public Boolean updateRemoteRepository( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        String repositoryId = remoteRepository.getId();

        triggerAuditEvent( repositoryId, null, AuditEvent.MODIFY_REMOTE_REPO, auditInformation );

        // update means : remove and add

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        RemoteRepositoryConfiguration remoteRepositoryConfiguration = getRepositoryConfiguration( remoteRepository );
        try
        {
            repositoryRegistry.putRepository( remoteRepositoryConfiguration, configuration );
        }
        catch ( RepositoryException e )
        {
            log.error("Could not update remote repository {}: {}", remoteRepositoryConfiguration.getId(), e.getMessage(), e);
            throw new RepositoryAdminException( "Update of remote repository failed"+(e.getMessage()==null?"":": "+e.getMessage()) );
        }
        saveConfiguration( configuration );
        return Boolean.TRUE;
    }

    @Override
    public Map<String, org.apache.archiva.admin.model.beans.RemoteRepository> getRemoteRepositoriesAsMap()
        throws RepositoryAdminException
    {
        java.util.Map<String, org.apache.archiva.admin.model.beans.RemoteRepository> map = new HashMap<>();

        for ( org.apache.archiva.admin.model.beans.RemoteRepository repo : getRemoteRepositories() )
        {
            map.put( repo.getId(), repo );
        }

        return map;
    }

    @Override
    public IndexingContext createIndexContext( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository )
        throws RepositoryAdminException
    {
        try
        {
            RemoteRepository repo = repositoryRegistry.getRemoteRepository(remoteRepository.getId());
            return repo.getIndexingContext().getBaseContext(IndexingContext.class);
            /*String appServerBase = getRegistry().getString( "appserver.base" );

            String contextKey = "remote-" + remoteRepository.getId();
            IndexingContext indexingContext = indexer.getIndexingContexts().get( contextKey );
            if ( indexingContext != null )
            {
                return indexingContext;
            }
            // create remote repository path
            Path repoDir = Paths.get( appServerBase, "data/remotes/" + remoteRepository.getId() );
            if ( !Files.exists(repoDir) )
            {
                Files.createDirectories(repoDir);
            }

            Path indexDirectory = null;

            // is there configured indexDirectory ?
            String indexDirectoryPath = remoteRepository.getIndexDirectory();

            if ( StringUtils.isNotBlank( indexDirectoryPath ) )
            {
                repoDir.resolve( indexDirectoryPath );
            }
            // if not configured use a default value
            if ( indexDirectory == null )
            {
                indexDirectory = repoDir.resolve(".index" );
            }
            if ( !Files.exists(indexDirectory) )
            {
                Files.createDirectories(indexDirectory);
            }

            try
            {

                return indexer.addIndexingContext( contextKey, remoteRepository.getId(), repoDir.toFile(), indexDirectory.toFile(),
                                                   remoteRepository.getUrl(), calculateIndexRemoteUrl( remoteRepository ),
                                                   indexCreators );
            }
            catch ( IndexFormatTooOldException e )
            {
                // existing index with an old lucene format so we need to delete it!!!
                // delete it first then recreate it.
                log.warn( "the index of repository {} is too old we have to delete and recreate it", //
                          remoteRepository.getId() );
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDirectory );
                return indexer.addIndexingContext( contextKey, remoteRepository.getId(), repoDir.toFile(), indexDirectory.toFile(),
                                                   remoteRepository.getUrl(), calculateIndexRemoteUrl( remoteRepository ),
                                                   indexCreators );

            }*/
        } catch (UnsupportedBaseContextException e) {
            throw new RepositoryAdminException( e.getMessage(), e);
        }

    }

    protected String calculateIndexRemoteUrl( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository )
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



}
