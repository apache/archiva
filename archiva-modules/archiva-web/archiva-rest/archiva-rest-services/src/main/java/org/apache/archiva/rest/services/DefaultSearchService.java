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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.RepositorySearchException;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.rest.api.model.ChecksumSearch;
import org.apache.archiva.rest.api.model.GroupIdList;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.model.StringList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Olivier Lamy
 */
@Service( "searchService#rest" )
public class DefaultSearchService
    extends AbstractRestService
    implements SearchService
{

    private static final String LATEST_KEYWORD = "LATEST";

    @Inject
    private RepositorySearch repositorySearch;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Override
    public List<Artifact> quickSearch( String queryString )
        throws ArchivaRestServiceException
    {
        if ( StringUtils.isBlank( queryString ) )
        {
            return Collections.emptyList();
        }

        SearchResultLimits limits = new SearchResultLimits( 0 );
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
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<Artifact> quickSearchWithRepositories( SearchRequest searchRequest )
        throws ArchivaRestServiceException
    {
        String queryString = searchRequest.getQueryTerms();
        if ( StringUtils.isBlank( queryString ) )
        {
            return Collections.emptyList();
        }
        List<String> repositories = searchRequest.getRepositories();
        if ( repositories == null || repositories.isEmpty() )
        {
            repositories = getObservableRepos();
        }
        SearchResultLimits limits =
            new SearchResultLimits( searchRequest.getPageSize(), searchRequest.getSelectedPage() );
        try
        {
            SearchResults searchResults = repositorySearch.search( getPrincipal(), repositories, queryString, limits,
                                                                   Collections.<String>emptyList() );
            return getArtifacts( searchResults );

        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
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
        searchField.setRepositories( getObservableRepos() );

        try
        {
            SearchResults searchResults = repositorySearch.search( getPrincipal(), searchField, null );
            return getArtifacts( searchResults );
        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<Artifact> searchArtifacts( SearchRequest searchRequest )
        throws ArchivaRestServiceException
    {
        if ( searchRequest == null )
        {
            return Collections.emptyList();
        }
        SearchFields searchField = getModelMapper().map( searchRequest, SearchFields.class );
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( searchRequest.getPageSize() );

        // if no repos set we use ones available for the user
        if ( searchField.getRepositories() == null || searchField.getRepositories().isEmpty() )
        {
            searchField.setRepositories( getObservableRepos() );
        }

        try
        {
            SearchResults searchResults = repositorySearch.search( getPrincipal(), searchField, limits );
            return getArtifacts( searchResults );
        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public GroupIdList getAllGroupIds( List<String> selectedRepos )
        throws ArchivaRestServiceException
    {
        List<String> observableRepos = getObservableRepos();
        List<String> repos = ListUtils.intersection( observableRepos, selectedRepos );
        if ( repos == null || repos.isEmpty() )
        {
            return new GroupIdList( Collections.<String>emptyList() );
        }
        try
        {
            return new GroupIdList( new ArrayList<>( repositorySearch.getAllGroupIds( getPrincipal(), repos ) ) );
        }
        catch ( RepositorySearchException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

    }


    public List<Artifact> getArtifactByChecksum( ChecksumSearch checksumSearch )
        throws ArchivaRestServiceException
    {

        // if no repos set we use ones available for the user
        if ( checksumSearch.getRepositories() == null || checksumSearch.getRepositories().isEmpty() )
        {
            checksumSearch.setRepositories( getObservableRepos() );
        }

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }

        MetadataRepository metadataRepository = repositorySession.getRepository();

        Set<Artifact> artifactSet = new HashSet<>();

        try
        {
            for ( String repoId : checksumSearch.getRepositories() )
            {
                Collection<ArtifactMetadata> artifactMetadatas =
                    metadataRepository.getArtifactsByChecksum( repositorySession, repoId, checksumSearch.getChecksum() );
                artifactSet.addAll( buildArtifacts( artifactMetadatas, repoId ) );
            }

            return new ArrayList<>( artifactSet );

        }
        catch ( MetadataRepositoryException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.closeQuietly();
        }


    }

    @Override
    public StringList getObservablesRepoIds()
        throws ArchivaRestServiceException
    {
        return new StringList( getObservableRepos() );
    }

    @Override
    public Response redirectToArtifactFile( String repositoryId, String groupId, String artifactId, String version,
                                            String packaging, String classifier )
        throws ArchivaRestServiceException
    {
        try
        {
            // validate query

            if ( StringUtils.isEmpty( groupId ) )
            {
                return Response.status( new Response.StatusType()
                {
                    @Override
                    public int getStatusCode()
                    {
                        return Response.Status.BAD_REQUEST.getStatusCode();
                    }

                    @Override
                    public Response.Status.Family getFamily()
                    {
                        return Response.Status.BAD_REQUEST.getFamily();
                    }

                    @Override
                    public String getReasonPhrase()
                    {
                        return "groupId mandatory";
                    }
                } ).build();
            }

            if ( StringUtils.isEmpty( version ) )
            {
                return Response.status( new Response.StatusType()
                {
                    @Override
                    public int getStatusCode()
                    {
                        return Response.Status.BAD_REQUEST.getStatusCode();
                    }

                    @Override
                    public Response.Status.Family getFamily()
                    {
                        return Response.Status.BAD_REQUEST.getFamily();
                    }

                    @Override
                    public String getReasonPhrase()
                    {
                        return "version mandatory";
                    }
                } ).build();
            }

            if ( StringUtils.isEmpty( artifactId ) )
            {
                return Response.status( new Response.StatusType()
                {
                    @Override
                    public int getStatusCode()
                    {
                        return Response.Status.BAD_REQUEST.getStatusCode();
                    }

                    @Override
                    public Response.Status.Family getFamily()
                    {
                        return Response.Status.BAD_REQUEST.getFamily();
                    }

                    @Override
                    public String getReasonPhrase()
                    {
                        return "artifactId mandatory";
                    }
                } ).build();
            }

            SearchFields searchField = new SearchFields();
            searchField.setGroupId( groupId );
            searchField.setArtifactId( artifactId );
            searchField.setPackaging( StringUtils.isBlank( packaging ) ? "jar" : packaging );
            if ( !StringUtils.equals( version, LATEST_KEYWORD ) )
            {
                searchField.setVersion( version );
            }
            searchField.setClassifier( classifier );
            List<String> userRepos = getObservablesRepoIds().getStrings();
            searchField.setRepositories(
                StringUtils.isEmpty( repositoryId ) ? userRepos : Arrays.asList( repositoryId ) );
            searchField.setExactSearch( true );
            SearchResults searchResults = repositorySearch.search( getPrincipal(), searchField, null );
            List<Artifact> artifacts = getArtifacts( searchResults );

            if ( artifacts.isEmpty() )
            {
                return Response.status( new Response.StatusType()
                {
                    @Override
                    public int getStatusCode()
                    {
                        return Response.Status.NO_CONTENT.getStatusCode();
                    }

                    @Override
                    public Response.Status.Family getFamily()
                    {
                        return Response.Status.NO_CONTENT.getFamily();
                    }

                    @Override
                    public String getReasonPhrase()
                    {
                        return "your query doesn't return any artifact";
                    }
                } ).build();
            }

            // TODO improve that with querying lucene with null value for classifier
            // so simple loop and retain only artifact with null classifier
            if ( classifier == null )
            {
                List<Artifact> filteredArtifacts = new ArrayList<>( artifacts.size() );
                for ( Artifact artifact : artifacts )
                {
                    if ( artifact.getClassifier() == null )
                    {
                        filteredArtifacts.add( artifact );
                    }
                }

                artifacts = filteredArtifacts;
            }

            // TODO return json result of the query ?
            if ( artifacts.size() > 1 && !StringUtils.equals( version, LATEST_KEYWORD ) )
            {
                return Response.status( new Response.StatusType()
                {
                    @Override
                    public int getStatusCode()
                    {
                        return Response.Status.BAD_REQUEST.getStatusCode();
                    }

                    @Override
                    public Response.Status.Family getFamily()
                    {
                        return Response.Status.BAD_REQUEST.getFamily();
                    }

                    @Override
                    public String getReasonPhrase()
                    {
                        return "your query return more than one artifact";
                    }
                } ).build();
            }

            // version is LATEST so we have to find the latest one from the result
            if ( artifacts.size() > 1 && StringUtils.equals( version, LATEST_KEYWORD ) )
            {
                TreeMap<String, Artifact> artifactPerVersion = new TreeMap<>( VersionComparator.getInstance() );

                for ( Artifact artifact : artifacts )
                {
                    artifactPerVersion.put( artifact.getVersion(), artifact );
                }

                return Response.temporaryRedirect(
                    new URI( artifactPerVersion.lastEntry().getValue().getUrl() ) ).build();

            }

            Artifact artifact = artifacts.get( 0 );

            return Response.temporaryRedirect( new URI( artifact.getUrl() ) ).build();
        }
        catch ( Exception e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }


    //-------------------------------------
    // internal
    //-------------------------------------
    protected List<Artifact> getArtifacts( SearchResults searchResults )
        throws ArchivaRestServiceException
    {

        if ( searchResults == null || searchResults.isEmpty() )
        {
            return Collections.emptyList();
        }
        List<Artifact> artifacts = new ArrayList<>( searchResults.getReturnedHitsCount() );
        for ( SearchResultHit hit : searchResults.getHits() )
        {
            // duplicate Artifact one per available version
            if ( hit.getVersions().size() > 0 )
            {
                for ( String version : hit.getVersions() )
                {

                    Artifact versionned = getModelMapper().map( hit, Artifact.class );

                    if ( StringUtils.isNotBlank( version ) )
                    {
                        versionned.setVersion( version );
                        versionned.setUrl( getArtifactUrl( versionned ) );

                        artifacts.add( versionned );

                    }
                }
            }
        }
        return artifacts;
    }


}
