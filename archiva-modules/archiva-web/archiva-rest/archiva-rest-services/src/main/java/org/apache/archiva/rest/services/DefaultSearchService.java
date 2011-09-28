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
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.RepositorySearchException;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.Dependency;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.rest.services.interceptors.HttpContext;
import org.apache.archiva.rest.services.interceptors.HttpContextThreadLocal;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.codehaus.redback.rest.services.RedbackRequestInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "searchService#rest" )
public class DefaultSearchService
    implements SearchService
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositorySearch repositorySearch;

    @Inject
    private UserRepositories userRepositories;

    public List<Artifact> quickSearch( String queryString )
        throws ArchivaRestServiceException
    {
        if ( StringUtils.isBlank( queryString ) )
        {
            return Collections.emptyList();
        }

        SearchResultLimits limits = new SearchResultLimits( 0 );
        List<String> observableRepoIds = getObservableRepos();
        try
        {
            SearchResults searchResults =
                repositorySearch.search( getPrincipal(), getObservableRepos(), queryString, limits,
                                         Collections.<String>emptyList() );
            return getArtifacts( searchResults );

        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public List<Artifact> getArtifactVersions( String groupId, String artifactId, String packaging )
        throws ArchivaRestServiceException
    {
        if ( StringUtils.isBlank( groupId ) || StringUtils.isBlank( artifactId ) )
        {
            return Collections.emptyList();
        }
        SearchFields searchField = new SearchFields();
        searchField.setGroupId( groupId );
        searchField.setArtifactId( artifactId );
        searchField.setPackaging( StringUtils.isBlank( packaging ) ? "jar" : packaging );

        try
        {
            SearchResults searchResults = repositorySearch.search( getPrincipal(), searchField, null );
            return getArtifacts( searchResults );
        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public List<Artifact> searchArtifacts( SearchRequest searchRequest )
        throws ArchivaRestServiceException
    {
        if ( searchRequest == null )
        {
            return Collections.emptyList();
        }
        SearchFields searchField = new BeanReplicator().replicateBean( searchRequest, SearchFields.class );
        SearchResultLimits limits = new SearchResultLimits( 0 );

        try
        {
            SearchResults searchResults = repositorySearch.search( getPrincipal(), searchField, limits );
            return getArtifacts( searchResults );
        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public List<Dependency> getDependencies( String groupId, String artifactId, String version )
        throws ArchivaRestServiceException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Artifact> getArtifactByChecksum( String checksum )
        throws ArchivaRestServiceException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
            ? UserManager.GUEST_USERNAME
            : ( redbackRequestInformation.getUser() == null
                ? UserManager.GUEST_USERNAME
                : redbackRequestInformation.getUser().getUsername() );
    }

    protected List<Artifact> getArtifacts( SearchResults searchResults )
    {

        HttpContext httpContext = HttpContextThreadLocal.get();
        if ( searchResults == null || searchResults.isEmpty() )
        {
            return Collections.emptyList();
        }
        List<Artifact> artifacts = new ArrayList<Artifact>( searchResults.getReturnedHitsCount() );
        for ( SearchResultHit hit : searchResults.getHits() )
        {
            // duplicate Artifact one per available version
            if ( hit.getVersions().size() > 0 )
            {
                for ( String version : hit.getVersions() )
                {
                    /*
                    Artifact versionned = new Artifact(  );
                    versionned.setArtifactId( hit.getArtifactId());
                    versionned.setGroupId( hit.getGroupId() );
                    versionned.setRepositoryId(hit.getRepositoryId() );


                    versionned.setBundleExportPackage( hit.getBundleExportPackage() );
                    versionned.setBundleExportService( hit.getBundleExportService());
                    versionned.setBundleSymbolicName(hit.getBundleSymbolicName() );
                    versionned.setBundleVersion( artifactInfo.bundleVersion );
                    versionned.setBundleDescription( artifactInfo.bundleDescription );
                    versionned.setBundleDocUrl( artifactInfo.bundleDocUrl );

                    versionned.setBundleRequireBundle( artifactInfo.bundleRequireBundle );
                    versionned.setBundleImportPackage( artifactInfo.bundleImportPackage );
                    versionned.setBundleLicense( artifactInfo.bundleLicense );
                    versionned.setBundleName( artifactInfo.bundleName );
                    versionned.setContext( artifactInfo.context );
                    versionned.setGoals( artifactInfo.goals );
                    versionned.setPrefix( artifactInfo.prefix );
                    // sure ??
                    versionned.setUrl( artifactInfo.remoteUrl );
                    */
                    // FIXME archiva url ??

                    Artifact versionned = new BeanReplicator().replicateBean( hit, Artifact.class );

                    if ( StringUtils.isNotBlank( version ) )
                    {
                        versionned.setVersion( version );
                        versionned.setUrl( getArtifactUrl( httpContext, versionned ) );

                        artifacts.add( versionned );

                    }
                }
            }
        }
        return artifacts;
    }

    private String getArtifactUrl( HttpContext httpContext, Artifact artifact )
    {
        if ( httpContext == null )
        {
            return null;
        }
        if ( httpContext.getHttpServletRequest() == null )
        {
            return null;
        }
        if ( StringUtils.isEmpty( artifact.getUrl() ) )
        {
            return null;
        }
        StringBuilder sb = new StringBuilder( getBaseUrl( httpContext.getHttpServletRequest() ) );

        sb.append( "/repository" );
        if ( !StringUtils.startsWith( artifact.getUrl(), "/" ) )
        {
            sb.append( '/' );
        }
        sb.append( artifact.getUrl() );
        return sb.toString();
    }

    protected String getBaseUrl( HttpServletRequest req )
    {
        return req.getScheme() + "://" + req.getServerName()
            + ( req.getServerPort() == 80 ? "" : ":" + req.getServerPort() ) + req.getContextPath();
    }
}
