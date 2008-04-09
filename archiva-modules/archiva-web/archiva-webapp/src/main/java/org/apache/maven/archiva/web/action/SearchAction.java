package org.apache.maven.archiva.web.action;

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

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.search.CrossRepositorySearch;
import org.apache.maven.archiva.indexer.search.SearchResultLimits;
import org.apache.maven.archiva.indexer.search.SearchResults;
import org.apache.maven.archiva.security.AccessDeniedException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.web.util.ArchivaXworkUser;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

/**
 * Search all indexed fields by the given criteria.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="searchAction"
 */
public class SearchAction
    extends PlexusActionSupport
{
    /**
     * Query string.
     */
    private String q;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * The Search Results.
     */
    private SearchResults results;

    /**
     * @plexus.requirement role-hint="default"
     */
    private CrossRepositorySearch crossRepoSearch;
    
    /**
     * @plexus.requirement
     */
    private UserRepositories userRepositories;
    
    private static final String RESULTS = "results";

    private static final String ARTIFACT = "artifact";

    private List databaseResults;

    public String quickSearch()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        /* TODO: give action message if indexing is in progress.
         * This should be based off a count of 'unprocessed' artifacts.
         * This (yet to be written) routine could tell the user that X (unprocessed) artifacts are not yet 
         * present in the full text search.
         */

        assert q != null && q.length() != 0;

        SearchResultLimits limits = new SearchResultLimits( 0 );
        
        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        results = crossRepoSearch.searchForTerm( getPrincipal(), selectedRepos, q, limits );

        if ( results.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        // TODO: filter / combine the artifacts by version? (is that even possible with non-artifact hits?)

        /* I don't think that we should, as I expect us to utilize the 'score' system in lucene in 
         * the future to return relevant links better.
         * I expect the lucene scoring system to take multiple hits on different areas of a single document
         * to result in a higher score. 
         *   - Joakim
         */

        return SUCCESS;
    }

    public String findArtifact()
        throws Exception
    {
        // TODO: give action message if indexing is in progress

        if ( StringUtils.isBlank( q ) )
        {
            addActionError( "Unable to search for a blank checksum" );
            return INPUT;
        }

        Constraint constraint = new ArtifactsByChecksumConstraint( q );
        databaseResults = dao.getArtifactDAO().queryArtifacts( constraint );

        if ( databaseResults.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        if ( databaseResults.size() == 1 )
        {
            // 1 hit? return it's information directly!            
            return ARTIFACT;
        }
        
        return RESULTS;
    }

    @Override
    public String doInput()
    {
        return INPUT;
    }
    
    private String getPrincipal()
    {
        return ArchivaXworkUser.getActivePrincipal();
    }
    
    private List<String> getObservableRepos()
    {
        try
        {
            return userRepositories.getObservableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            getLogger().warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            getLogger().warn( e.getMessage(), e );
            // TODO: pass this onto the screen.
        }
        catch ( ArchivaSecurityException e )
        {
            getLogger().warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    public String getQ()
    {
        return q;
    }

    public void setQ( String q )
    {
        this.q = q;
    }

    public SearchResults getResults()
    {
        return results;
    }

    public List getDatabaseResults()
    {
        return databaseResults;
    }
}
