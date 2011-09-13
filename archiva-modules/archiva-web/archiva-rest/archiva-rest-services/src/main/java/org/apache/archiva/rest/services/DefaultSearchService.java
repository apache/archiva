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
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.Dependency;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.SearchService;
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

    public List<Artifact> getArtifactByChecksum( String checksum )
        throws ArchivaRestServiceException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Artifact> getArtifactVersions( String groupId, String artifactId )
        throws ArchivaRestServiceException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Dependency> getDependencies( String groupId, String artifactId, String version )
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
        if ( searchResults == null || searchResults.isEmpty() )
        {
            return Collections.emptyList();
        }
        List<Artifact> artifacts = new ArrayList<Artifact>( searchResults.getReturnedHitsCount() );
        for ( SearchResultHit searchResultHit : searchResults.getHits() )
        {
            Artifact artifact = new BeanReplicator().replicateBean( searchResultHit, Artifact.class );
            artifacts.add( artifact );
            // duplicate Artifact one per available version
            if ( searchResultHit.getVersions().size() > 1 )
            {
                for ( String version : searchResultHit.getVersions() )
                {
                    Artifact versionned = new BeanReplicator().replicateBean( searchResultHit, Artifact.class );
                    versionned.setVersion( version );
                    artifacts.add( versionned );
                }
            }
        }
        return artifacts;
    }
}
