package org.apache.maven.archiva.indexer.search;

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
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Searchable;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.ArtifactKeys;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.bytecode.BytecodeHandlers;
import org.apache.maven.archiva.indexer.bytecode.BytecodeKeys;
import org.apache.maven.archiva.indexer.filecontent.FileContentHandlers;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesHandlers;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesKeys;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultCrossRepositorySearch
 * 
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.indexer.search.CrossRepositorySearch" role-hint="default"
 */
public class DefaultCrossRepositorySearch
    implements CrossRepositorySearch, RegistryListener, Initializable
{
    private Logger log = LoggerFactory.getLogger( DefaultCrossRepositorySearch.class );

    /**
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    private List<ManagedRepositoryConfiguration> localIndexedRepositories = new ArrayList<ManagedRepositoryConfiguration>();
    
    public SearchResults executeFilteredSearch( String principal, List<String> selectedRepos, String groupId,
                                                String artifactId, String version, String className,
                                                SearchResultLimits limits )
    {
        List<RepositoryContentIndex> indexes = getBytecodeIndexes( principal, selectedRepos );
        SearchResults results = new SearchResults();        
        List<String> fieldsList = new ArrayList<String>();
        List<String> termsList = new ArrayList<String>();
        List<BooleanClause.Occur> flagsList = new ArrayList<BooleanClause.Occur>();
        
        if( groupId != null && !"".equals( groupId.trim() ) )
        {
            fieldsList.add( ArtifactKeys.GROUPID );
            termsList.add( groupId );
            flagsList.add( BooleanClause.Occur.MUST );            
        }
        
        if( artifactId != null && !"".equals( artifactId.trim() ) )
        {
            fieldsList.add( ArtifactKeys.ARTIFACTID );
            termsList.add( artifactId );
            flagsList.add( BooleanClause.Occur.MUST );
        }
        
        if( version != null && !"".equals( version.trim() ) )
        {
            fieldsList.add( ArtifactKeys.VERSION );
            termsList.add( version );
            flagsList.add( BooleanClause.Occur.MUST );
        }
        
        if( className != null && !"".equals( className.trim() ) )
        {   
            fieldsList.add( BytecodeKeys.CLASSES );
            fieldsList.add( BytecodeKeys.FILES );
            fieldsList.add( BytecodeKeys.METHODS );
            termsList.add( className.trim() );
            termsList.add( className.trim() );
            termsList.add( className.trim() );
            flagsList.add( BooleanClause.Occur.SHOULD );
            flagsList.add( BooleanClause.Occur.SHOULD );
            flagsList.add( BooleanClause.Occur.SHOULD );
        }        
        
        try
        {
            String[] fieldsArr = new String[ fieldsList.size() ];
            String[] queryArr = new String[ termsList.size() ];
            BooleanClause.Occur[] flagsArr = new BooleanClause.Occur[ flagsList.size() ];
            
            Query fieldsQuery =
                MultiFieldQueryParser.parse( termsList.toArray( queryArr ), fieldsList.toArray( fieldsArr ),
                                             flagsList.toArray( flagsArr ), new BytecodeHandlers().getAnalyzer() );
            
            LuceneQuery query = new LuceneQuery( fieldsQuery );
            results = searchAll( query, limits, indexes, null );
            results.getRepositories().add( this.localIndexedRepositories );
        }
        catch ( ParseException e )
        {
            log.warn( "Unable to parse advanced search fields and query terms." );
        }        

        return results;
    }

    public SearchResults searchForChecksum( String principal, List<String> selectedRepos, String checksum,
                                            SearchResultLimits limits )
    {
        List<RepositoryContentIndex> indexes = getHashcodeIndexes( principal, selectedRepos );

        try
        {
            QueryParser parser = new MultiFieldQueryParser( new String[]{HashcodesKeys.MD5, HashcodesKeys.SHA1},
                                           new HashcodesHandlers().getAnalyzer() );
            LuceneQuery query = new LuceneQuery( parser.parse( checksum ) );
            SearchResults results = searchAll( query, limits, indexes, null );
            results.getRepositories().addAll( this.localIndexedRepositories );

            return results;
        }
        catch ( ParseException e )
        {
            log.warn( "Unable to parse query [" + checksum + "]: " + e.getMessage(), e );
        }

        // empty results.
        return new SearchResults();
    }

    public SearchResults searchForBytecode( String principal, List<String> selectedRepos, String term, SearchResultLimits limits )
    {
        List<RepositoryContentIndex> indexes = getBytecodeIndexes( principal, selectedRepos );

        try
        {
            QueryParser parser = new BytecodeHandlers().getQueryParser();
            LuceneQuery query = new LuceneQuery( parser.parse( term ) );
            SearchResults results = searchAll( query, limits, indexes, null );
            results.getRepositories().addAll( this.localIndexedRepositories );

            return results;
        }
        catch ( ParseException e )
        {
            log.warn( "Unable to parse query [" + term + "]: " + e.getMessage(), e );
        }

        // empty results.
        return new SearchResults();
    }

    public SearchResults searchForTerm( String principal, List<String> selectedRepos, String term, SearchResultLimits limits )
    {
        return searchForTerm( principal, selectedRepos, term, limits, null );        
    }

    public SearchResults searchForTerm( String principal, List<String> selectedRepos, String term,
                                        SearchResultLimits limits, List<String> previousSearchTerms )
    {
        List<RepositoryContentIndex> indexes = getFileContentIndexes( principal, selectedRepos );

        try
        {
            QueryParser parser = new FileContentHandlers().getQueryParser();
            LuceneQuery query = null;
            SearchResults results = null;
            if ( previousSearchTerms == null || previousSearchTerms.isEmpty() )
            {
                query = new LuceneQuery( parser.parse( term ) );
                results = searchAll( query, limits, indexes, null );
            }
            else
            {
                // AND the previous search terms
                BooleanQuery booleanQuery = new BooleanQuery();
                for ( String previousSearchTerm : previousSearchTerms )
                {
                    booleanQuery.add( parser.parse( previousSearchTerm ), BooleanClause.Occur.MUST );
                }

                query = new LuceneQuery( booleanQuery );
                Filter filter = new QueryWrapperFilter( parser.parse( term ) );
                results = searchAll( query, limits, indexes, filter );
            }
            results.getRepositories().addAll( this.localIndexedRepositories );

            return results;
        }
        catch ( ParseException e )
        {
            log.warn( "Unable to parse query [" + term + "]: " + e.getMessage(), e );
        }

        // empty results.
        return new SearchResults();
    }

    private SearchResults searchAll( LuceneQuery luceneQuery, SearchResultLimits limits, List<RepositoryContentIndex> indexes, Filter filter )
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
            Hits hits = null;
            if ( filter != null )
            {
                hits = searcher.search( specificQuery, filter );
            }
            else
            {
                hits = searcher.search( specificQuery );
            }

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
                for ( int i = 0; i < fetchCount; i++ )
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
                        log.warn( "Unable to parse document into record: " + e.getMessage(), e );
                    }
                }
            }

        }
        catch ( IOException e )
        {
            log.error( "Unable to setup multi-search: " + e.getMessage(), e );
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
                log.error( "Unable to close index searcher: " + ie.getMessage(), ie );
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
                log.warn( "Unable to get searchable for index [" + contentIndex.getId() + "] :"
                                      + e.getMessage(), e );
            }
        }
        return searchableList;
    }

    public List<RepositoryContentIndex> getBytecodeIndexes( String principal, List<String> selectedRepos )
    {
        List<RepositoryContentIndex> ret = new ArrayList<RepositoryContentIndex>();

        for ( ManagedRepositoryConfiguration repoConfig : localIndexedRepositories )
        {
            // Only used selected repo
            if ( selectedRepos.contains( repoConfig.getId() ) )
            {
                RepositoryContentIndex index = indexFactory.createBytecodeIndex( repoConfig );
                // If they exist.
                if ( indexExists( index ) )
                {
                    ret.add( index );
                }
            }
        }

        return ret;
    }

    public List<RepositoryContentIndex> getFileContentIndexes( String principal, List<String> selectedRepos )
    {
        List<RepositoryContentIndex> ret = new ArrayList<RepositoryContentIndex>();

        for ( ManagedRepositoryConfiguration repoConfig : localIndexedRepositories )
        {
            // Only used selected repo
            if ( selectedRepos.contains( repoConfig.getId() ) )
            {
                RepositoryContentIndex index = indexFactory.createFileContentIndex( repoConfig );
                // If they exist.
                if ( indexExists( index ) )
                {
                    ret.add( index );
                }
            }
        }

        return ret;
    }

    public List<RepositoryContentIndex> getHashcodeIndexes( String principal, List<String> selectedRepos )
    {
        List<RepositoryContentIndex> ret = new ArrayList<RepositoryContentIndex>();

        for ( ManagedRepositoryConfiguration repoConfig : localIndexedRepositories )
        {
            // Only used selected repo
            if ( selectedRepos.contains( repoConfig.getId() ) )
            {
                RepositoryContentIndex index = indexFactory.createHashcodeIndex( repoConfig );
                // If they exist.
                if ( indexExists( index ) )
                {
                    ret.add( index );
                }
            }
        }

        return ret;
    }

    private boolean indexExists( RepositoryContentIndex index )
    {
        try
        {
            return index.exists();
        }
        catch ( RepositoryIndexException e )
        {
            log.info(
                              "Repository Content Index [" + index.getId() + "] for repository ["
                                  + index.getRepository().getId() + "] does not exist yet in ["
                                  + index.getIndexDirectory().getAbsolutePath() + "]." );
            return false;
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName ) )
        {
            initRepositories();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* Nothing to do here */
    }

    private void initRepositories()
    {
        synchronized ( this.localIndexedRepositories )
        {
            this.localIndexedRepositories.clear();

            List<ManagedRepositoryConfiguration> repos = configuration.getConfiguration().getManagedRepositories();
            for ( ManagedRepositoryConfiguration repo : repos )
            {
                if ( repo.isScanned() )
                {
                    localIndexedRepositories.add( repo );
                }
            }
        }
    }

    public void initialize()
        throws InitializationException
    {
        initRepositories();
        configuration.addChangeListener( this );
    }
}
