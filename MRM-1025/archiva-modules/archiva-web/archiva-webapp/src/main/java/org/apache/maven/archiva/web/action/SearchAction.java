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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
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
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.Preparable;

/**
 * Search all indexed fields by the given criteria.
 *
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="searchAction"
 */
public class SearchAction
    extends PlexusActionSupport
    implements Preparable
{
    /**
     * Query string.
     */

    private ArchivaConfiguration archivaConfiguration;

    private Map<String, ManagedRepositoryConfiguration> managedRepositories;

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
    
    /**
     * @plexus.requirement
     */
    private ArchivaXworkUser archivaXworkUser;
    
    private static final String RESULTS = "results";

    private static final String ARTIFACT = "artifact";

    private List databaseResults;
    
    private int currentPage = 0;
    
    private int totalPages;
    
    private boolean searchResultsOnly;
    
    private String completeQueryString;
    
    private static final String COMPLETE_QUERY_STRING_SEPARATOR = ";";

    private List<String> managedRepositoryList;

    private String groupId;

    private String artifactId;

    private String version;

    private String className;

    private int rowCount = 30;

    private String repositoryId;

    private boolean fromFilterSearch;

    private boolean filterSearch = false;

    private boolean fromResultsPage;

    private int num;

    public boolean isFromResultsPage()
    {
        return fromResultsPage;
    }

    public void setFromResultsPage( boolean fromResultsPage )
    {
        this.fromResultsPage = fromResultsPage;
    }

    public boolean isFromFilterSearch()
    {
        return fromFilterSearch;
    }

    public void setFromFilterSearch( boolean fromFilterSearch )
    {
        this.fromFilterSearch = fromFilterSearch;
    }

    public void prepare()
    {
        managedRepositoryList = new ArrayList<String>();
        managedRepositoryList = getObservableRepos();

        if ( managedRepositoryList.size() > 0 )
        {
            managedRepositoryList.add( "all" );
        }
    }

    // advanced search MRM-90 -- filtered search
    public String filteredSearch()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        fromFilterSearch = true;

        if ( CollectionUtils.isEmpty( managedRepositoryList ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        SearchResultLimits limits = new SearchResultLimits( currentPage );

        limits.setPageSize( rowCount );
        List<String> selectedRepos = new ArrayList<String>();

        if ( repositoryId.equals( "all" ) )
        {
            selectedRepos = getObservableRepos();
        }
        else
        {
            selectedRepos.add( repositoryId );
        }

        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        results =
            crossRepoSearch.executeFilteredSearch( getPrincipal(), selectedRepos, groupId, artifactId, version,
                                                   className, limits );

        if ( results.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        totalPages = results.getTotalHits() / limits.getPageSize();

        if ( ( results.getTotalHits() % limits.getPageSize() ) != 0 )
        {
            totalPages = totalPages + 1;
        }

        return SUCCESS;
    }

    public String quickSearch()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        /* TODO: give action message if indexing is in progress.
         * This should be based off a count of 'unprocessed' artifacts.
         * This (yet to be written) routine could tell the user that X (unprocessed) artifacts are not yet
         * present in the full text search.
         */

        assert q != null && q.length() != 0;

        fromFilterSearch = false;

        SearchResultLimits limits = new SearchResultLimits( currentPage );

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        if( SearchUtil.isBytecodeSearch( q ) )
        {
            results = crossRepoSearch.searchForBytecode( getPrincipal(), selectedRepos, SearchUtil.removeBytecodeKeyword( q ), limits );
        }
        else
        {
            if( searchResultsOnly && !completeQueryString.equals( "" ) )
            {
                results = crossRepoSearch.searchForTerm( getPrincipal(), selectedRepos, q, limits, parseCompleteQueryString() );
            }
            else
            {
                completeQueryString = "";
                results = crossRepoSearch.searchForTerm( getPrincipal(), selectedRepos, q, limits );
            }
        }

        if ( results.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        totalPages = results.getTotalHits() / limits.getPageSize();

        if( (results.getTotalHits() % limits.getPageSize()) != 0 )
        {
            totalPages = totalPages + 1;
        }
        // TODO: filter / combine the artifacts by version? (is that even possible with non-artifact hits?)

        /* I don't think that we should, as I expect us to utilize the 'score' system in lucene in
         * the future to return relevant links better.
         * I expect the lucene scoring system to take multiple hits on different areas of a single document
         * to result in a higher score.
         *   - Joakim
         */

        if( !isEqualToPreviousSearchTerm( q ) )
        {
            buildCompleteQueryString( q );
        }

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
    
    public String doInput()
    {
        return INPUT;
    }

    private String getPrincipal()
    {
        return archivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() );
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

    private void buildCompleteQueryString( String searchTerm )
    {
        if ( searchTerm.indexOf( COMPLETE_QUERY_STRING_SEPARATOR ) != -1 )
        {
            searchTerm = StringUtils.remove( searchTerm, COMPLETE_QUERY_STRING_SEPARATOR );
        }

        if ( completeQueryString == null || "".equals( completeQueryString ) )
        {
            completeQueryString = searchTerm;
        }
        else
        {
            completeQueryString = completeQueryString + COMPLETE_QUERY_STRING_SEPARATOR + searchTerm;
        }
    }

    private List<String> parseCompleteQueryString()
    {
        List<String> parsedCompleteQueryString = new ArrayList<String>();
        String[] parsed = StringUtils.split( completeQueryString, COMPLETE_QUERY_STRING_SEPARATOR );
        CollectionUtils.addAll( parsedCompleteQueryString, parsed );

        return parsedCompleteQueryString;
    }

    private boolean isEqualToPreviousSearchTerm( String searchTerm )
    {
        if ( !"".equals( completeQueryString ) )
        {
            String[] parsed = StringUtils.split( completeQueryString, COMPLETE_QUERY_STRING_SEPARATOR );
            if ( StringUtils.equalsIgnoreCase( searchTerm, parsed[parsed.length - 1] ) )
            {
                return true;
            }
        }

        return false;
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

    public void setCurrentPage( int page )
    {
        this.currentPage = page;
    }

    public int getCurrentPage()
    {
        return currentPage;
    }

    public int getTotalPages()
    {
        return totalPages;
    }

    public void setTotalPages( int totalPages )
    {
        this.totalPages = totalPages;
    }

    public boolean isSearchResultsOnly()
    {
        return searchResultsOnly;
    }

    public void setSearchResultsOnly( boolean searchResultsOnly )
    {
        this.searchResultsOnly = searchResultsOnly;
    }

    public String getCompleteQueryString()
    {
        return completeQueryString;
    }

    public void setCompleteQueryString( String completeQueryString )
    {
        this.completeQueryString = completeQueryString;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Map<String, ManagedRepositoryConfiguration> getManagedRepositories()
    {
        return getArchivaConfiguration().getConfiguration().getManagedRepositoriesAsMap();
    }

    public void setManagedRepositories( Map<String, ManagedRepositoryConfiguration> managedRepositories )
    {
        this.managedRepositories = managedRepositories;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount( int rowCount )
    {
        this.rowCount = rowCount;
    }

    public boolean isFilterSearch()
    {
        return filterSearch;
    }

    public void setFilterSearch( boolean filterSearch )
    {
        this.filterSearch = filterSearch;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public List<String> getManagedRepositoryList()
    {
        return managedRepositoryList;
    }

    public void setManagedRepositoryList( List<String> managedRepositoryList )
    {
        this.managedRepositoryList = managedRepositoryList;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }
}
