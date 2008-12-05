package org.apache.maven.archiva.consumers.lucene;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.bytecode.BytecodeHandlers;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.indexer.search.SearchResultLimits;
import org.apache.maven.archiva.indexer.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searcher used for testing purposes only.
 * 
 * @version
 */
public class IndexJavaPublicMethodsCrossRepositorySearch    
{
    private Logger log = LoggerFactory.getLogger( IndexJavaPublicMethodsCrossRepositorySearch.class );
    
    private ManagedRepositoryConfiguration localIndexedRepo;
    
    private RepositoryContentIndexFactory indexFactory;
            
    public IndexJavaPublicMethodsCrossRepositorySearch( ManagedRepositoryConfiguration localIndexedRepo, RepositoryContentIndexFactory indexFactory )
    {
        this.localIndexedRepo = localIndexedRepo;
        this.indexFactory = indexFactory;
    }
    
    public SearchResults searchForBytecode( String principal, List<String> selectedRepos, String term,
                                            SearchResultLimits limits ) throws ParseException
    {   
        List<RepositoryContentIndex> indexes = new ArrayList<RepositoryContentIndex>();
        indexes.add( indexFactory.createBytecodeIndex( localIndexedRepo ) );
        
        QueryParser parser = new BytecodeHandlers().getQueryParser();
        LuceneQuery query = new LuceneQuery( parser.parse( term ) );
        SearchResults results = searchAll( query, limits, indexes );
        results.getRepositories().add( localIndexedRepo );
        
        return results;       
    }
    
    private SearchResults searchAll( LuceneQuery luceneQuery, SearchResultLimits limits, List<RepositoryContentIndex> indexes )
    {
        org.apache.lucene.search.Query specificQuery = luceneQuery.getLuceneQuery();

        SearchResults results = new SearchResults();

        if ( indexes.isEmpty() )
        {
            // No point going any further.
            return results;
        }

        // Setup the converter
        LuceneEntryConverter converter = null;
        RepositoryContentIndex index = indexes.get( 0 );
        converter = index.getEntryConverter();

        // Process indexes into an array of Searchables.
        List<Searchable> searchableList = toSearchables( indexes );

        Searchable searchables[] = new Searchable[searchableList.size()];
        searchableList.toArray( searchables );

        MultiSearcher searcher = null;

        try
        {
            // Create a multi-searcher for looking up the information.
            searcher = new MultiSearcher( searchables );

            // Perform the search.
            Hits hits = searcher.search( specificQuery );

            int hitCount = hits.length();

            // Now process the limits.
            results.setLimits( limits );
            results.setTotalHits( hitCount );

            int fetchCount = limits.getPageSize();
            int offset = ( limits.getSelectedPage() * limits.getPageSize() );

            if ( limits.getSelectedPage() == SearchResultLimits.ALL_PAGES )
            {
                fetchCount = hitCount;
                offset = 0;
            }

            // Goto offset.
            if ( offset < hitCount )
            {
                // only process if the offset is within the hit count.
                for ( int i = 0; i <= fetchCount; i++ )
                {
                    // Stop fetching if we are past the total # of available hits.
                    if ( offset + i >= hitCount )
                    {
                        break;
                    }

                    try
                    {
                        Document doc = hits.doc( offset + i );
                        LuceneRepositoryContentRecord record = converter.convert( doc );
                        results.addHit( record );
                    }
                    catch ( java.text.ParseException e )
                    {
                        log.error( e.getMessage() );
                    }
                }
            }

        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
        }
        finally
        {
            try
            {
                if ( searcher != null )
                {
                    searcher.close();
                }
            }
            catch ( IOException ie )
            {
                log.error( ie.getMessage() );
            }
        }

        return results;
    }
    
    private List<Searchable> toSearchables( List<RepositoryContentIndex> indexes )
    {
        List<Searchable> searchableList = new ArrayList<Searchable>();
        for ( RepositoryContentIndex contentIndex : indexes )
        {   
            try
            {         
                searchableList.add( contentIndex.getSearchable() );
            }
            catch ( RepositoryIndexSearchException e )
            {
                log.error( e.getMessage() );
            }
        }
        return searchableList;
    }
}
