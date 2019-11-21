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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.metadata.audit.AuditListener;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.services.utils.ArtifactBuilder;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * abstract class with common utilities methods
 *
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public abstract class AbstractRestService
{

    protected final Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<>();

    @Inject
    protected UserRepositories userRepositories;


    /**
     * FIXME: this could be multiple implementations and needs to be configured.
     */
    @Inject
    @Named(value = "repositorySessionFactory")
    protected RepositorySessionFactory repositorySessionFactory;

    @Inject
    protected ArchivaAdministration archivaAdministration;

    @Inject
    protected ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    protected RepositoryRegistry repositoryRegistry;

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    protected RepositoryArchivaTaskScheduler repositoryTaskScheduler;


    @Inject
    @Named(value = "userConfiguration#default")
    protected UserConfiguration config;

    @Context
    protected HttpServletRequest httpServletRequest;

    @Context
    protected HttpServletResponse httpServletResponse;

    protected AuditInformation getAuditInformation()
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        User user = redbackRequestInformation == null ? null : redbackRequestInformation.getUser();
        String remoteAddr = redbackRequestInformation == null ? null : redbackRequestInformation.getRemoteAddr();
        return new AuditInformation( user, remoteAddr );
    }

    public List<AuditListener> getAuditListeners()
    {
        return auditListeners;
    }

    public void setAuditListeners( List<AuditListener> auditListeners )
    {
        this.auditListeners = auditListeners;
    }

    protected List<String> getObservableRepos()
    {
        try
        {
            List<String> ids = userRepositories.getObservableRepositoryIds( getPrincipal() );
            return ids == null ? Collections.<String>emptyList() : ids;
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    protected String getPrincipal()
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();

        return redbackRequestInformation == null
            ? config.getString( UserConfigurationKeys.DEFAULT_GUEST )
            : ( redbackRequestInformation.getUser() == null
                ? config.getString( UserConfigurationKeys.DEFAULT_GUEST )
                : redbackRequestInformation.getUser().getUsername() );
    }

    protected String getBaseUrl()
        throws RepositoryAdminException
    {
        String applicationUrl = archivaAdministration.getUiConfiguration().getApplicationUrl();
        if ( StringUtils.isNotBlank( applicationUrl ) )
        {
            return applicationUrl;
        }
        return httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + (
            httpServletRequest.getServerPort() == 80 ? "" : ":" + httpServletRequest.getServerPort() )
            + httpServletRequest.getContextPath();
    }

    protected <T> Map<String, T> getBeansOfType( ApplicationContext applicationContext, Class<T> clazz )
    {
        //TODO do some caching here !!!
        // olamy : with plexus we get only roleHint
        // as per convention we named spring bean role#hint remove role# if exists
        Map<String, T> springBeans = applicationContext.getBeansOfType( clazz );

        Map<String, T> beans = new HashMap<>( springBeans.size() );

        for ( Map.Entry<String, T> entry : springBeans.entrySet() )
        {
            String key = StringUtils.contains( entry.getKey(), '#' )
                ? StringUtils.substringAfterLast( entry.getKey(), "#" )
                : entry.getKey();
            beans.put( key, entry.getValue() );
        }
        return beans;
    }

    protected void triggerAuditEvent( String repositoryId, String filePath, String action )
    {
        AuditEvent auditEvent = new AuditEvent( repositoryId, getPrincipal(), filePath, action );
        AuditInformation auditInformation = getAuditInformation();
        auditEvent.setUserId( auditInformation.getUser() == null ? "" : auditInformation.getUser().getUsername() );
        auditEvent.setRemoteIP( auditInformation.getRemoteAddr() );
        for ( AuditListener auditListener : getAuditListeners() )
        {
            auditListener.auditEvent( auditEvent );
        }
    }

    /**
     * @param artifact
     * @return
     */
    protected String getArtifactUrl( Artifact artifact )
        throws ArchivaRestServiceException
    {
        return getArtifactUrl( artifact, null );
    }


    protected String getArtifactUrl( Artifact artifact, String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {

            if ( httpServletRequest == null )
            {
                return null;
            }

            StringBuilder sb = new StringBuilder( getBaseUrl() );

            sb.append( "/repository" );

            // when artifact come from a remote repository when have here the remote repo id
            // we must replace it with a valid managed one available for the user.
            if ( StringUtils.isEmpty( repositoryId ) )
            {
                List<String> userRepos = userRepositories.getObservableRepositoryIds( getPrincipal() );
                // is it a good one? if yes nothing to
                // if not search the repo who is proxy for this remote
                if ( !userRepos.contains( artifact.getContext() ) )
                {
                    for ( Map.Entry<String, List<ProxyConnector>> entry : proxyConnectorAdmin.getProxyConnectorAsMap().entrySet() )
                    {
                        for ( ProxyConnector proxyConnector : entry.getValue() )
                        {
                            if ( StringUtils.equals( "remote-" + proxyConnector.getTargetRepoId(),
                                                     artifact.getContext() ) //
                                && userRepos.contains( entry.getKey() ) )
                            {
                                sb.append( '/' ).append( entry.getKey() );
                            }
                        }
                    }

                }
                else
                {
                    sb.append( '/' ).append( artifact.getContext() );
                }


            }
            else
            {
                sb.append( '/' ).append( repositoryId );
            }

            sb.append( '/' ).append( StringUtils.replaceChars( artifact.getGroupId(), '.', '/' ) );
            sb.append( '/' ).append( artifact.getArtifactId() );
            if ( VersionUtil.isSnapshot( artifact.getVersion() ) )
            {
                sb.append( '/' ).append( VersionUtil.getBaseVersion( artifact.getVersion() ) );
            }
            else
            {
                sb.append( '/' ).append( artifact.getVersion() );
            }
            sb.append( '/' ).append( artifact.getArtifactId() );
            sb.append( '-' ).append( artifact.getVersion() );
            if ( StringUtils.isNotBlank( artifact.getClassifier() ) )
            {
                sb.append( '-' ).append( artifact.getClassifier() );
            }
            // maven-plugin packaging is a jar
            if ( StringUtils.equals( "maven-plugin", artifact.getPackaging() ) )
            {
                sb.append( "jar" );
            }
            else
            {
                sb.append( '.' ).append( artifact.getFileExtension() );
            }

            return sb.toString();
        }
        catch ( Exception e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }

    protected List<Artifact> buildArtifacts( Collection<ArtifactMetadata> artifactMetadatas, String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {
            if ( artifactMetadatas != null && !artifactMetadatas.isEmpty() )
            {
                List<Artifact> artifacts = new ArrayList<>( artifactMetadatas.size() );
                for ( ArtifactMetadata artifact : artifactMetadatas )
                {

                    String repoId = repositoryId != null ? repositoryId : artifact.getRepositoryId();
                    if ( repoId == null ) {
                        throw new IllegalStateException( "Repository Id is null" );
                    }
                    ManagedRepository repo = repositoryRegistry.getManagedRepository( repoId );
                    if (repo==null) {
                        throw new RepositoryException( "Repository not found "+repoId );
                    }
                    ManagedRepositoryContent content = repo.getContent( );
                    ArtifactBuilder builder =
                        new ArtifactBuilder().forArtifactMetadata( artifact ).withManagedRepositoryContent(
                            content );
                    Artifact art = builder.build();
                    art.setUrl( getArtifactUrl( art, repositoryId ) );
                    artifacts.add( art );
                }
                return artifacts;
            }
            return Collections.emptyList();
        }
        catch ( RepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }

    protected Boolean doScanRepository( String repositoryId, boolean fullScan )
    {
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled", repositoryId );
            return Boolean.FALSE;
        }
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setScanAll( fullScan );
        try
        {
            repositoryTaskScheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to schedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
        return true;
    }

    private static class ModelMapperHolder
    {
        private static ModelMapper MODEL_MAPPER = new ModelMapper();

        static
        {
            MODEL_MAPPER.addMappings( new SearchResultHitMap() );
            MODEL_MAPPER.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
        }
    }


    private static class SearchResultHitMap
        extends PropertyMap<SearchResultHit, Artifact>
    {
        @Override
        protected void configure()
        {
            skip().setId( null );
        }
    }

    ;

    protected ModelMapper getModelMapper()
    {
        return ModelMapperHolder.MODEL_MAPPER;
    }
}
