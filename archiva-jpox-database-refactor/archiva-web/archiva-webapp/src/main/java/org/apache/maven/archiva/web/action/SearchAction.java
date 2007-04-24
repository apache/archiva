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

import org.apache.lucene.queryParser.ParseException;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.search.CrossRepositorySearch;
import org.apache.maven.archiva.indexer.search.SearchResults;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.net.MalformedURLException;

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
     * The MD5 to search by.
     */
    private String md5;
    
    /**
     * The Search Results.
     */
    private SearchResults results;

    /**
     * @plexus.requirement role-hint="default"
     */
    private CrossRepositorySearch crossRepoSearch;

    private static final String RESULTS = "results";

    private static final String ARTIFACT = "artifact";

    public String quickSearch()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException, ParseException
    {
        /* TODO: give action message if indexing is in progress.
         * This should be based off a count of 'unprocessed' artifacts.
         * This (yet to be written) routine could tell the user that X artifacts are not yet 
         * present in the full text search.
         */

        assert q != null && q.length() != 0;

        results = crossRepoSearch.searchForTerm( q );

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

        assert md5 != null && md5.length() != 0;

        results = crossRepoSearch.searchForMd5( q );
        
        if ( results.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }
        
        if ( results.getHashcodeHits().size() == 1 )
        {
            return ARTIFACT;
        }
        else
        {
            return RESULTS;
        }
    }

    public String doInput()
    {
        return INPUT;
    }

    public String getQ()
    {
        return q;
    }

    public void setQ( String q )
    {
        this.q = q;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }
}
