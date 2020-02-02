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
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryCheckPath;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryCredentials;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.base.PasswordCredentials;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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


    @PostConstruct
    private void initialize()
        throws RepositoryAdminException
    {
        for ( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository : getRemoteRepositories() )
        {
            createIndexContext( remoteRepository );
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
        if (repo.supportsFeature(IndexCreationFeature.class)) {
            IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
            adminRepo.setIndexDirectory(PathUtil.getPathFromUri(icf.getIndexPath()).toString());
        }
        adminRepo.setDescription(repo.getDescription());
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
        repoConfig.setDescription(repo.getDescription());
        repoConfig.setIndexDir(repo.getIndexDirectory());
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
        log.debug("Adding remote repo {}", remoteRepositoryConfiguration);

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
    public ArchivaIndexingContext createIndexContext( org.apache.archiva.admin.model.beans.RemoteRepository remoteRepository )
        throws RepositoryAdminException
    {
        RemoteRepository repo = repositoryRegistry.getRemoteRepository(remoteRepository.getId());
        return repo.getIndexingContext();

    }




}
