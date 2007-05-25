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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.AndPredicate;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.configuration.functors.IndexedRepositoryPredicate;
import org.apache.maven.archiva.configuration.functors.LocalRepositoryPredicate;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.bytecode.BytecodeHandlers;
import org.apache.maven.archiva.indexer.filecontent.FileContentHandlers;
import org.apache.maven.archiva.indexer.functors.UserAllowedToSearchRepositoryPredicate;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesHandlers;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesKeys;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DefaultCrossRepositorySearch 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.indexer.search.CrossRepositorySearch" role-hint="default"
 */
public class DefaultCrossRepositorySearch
    extends AbstractLogEnabled
    implements CrossRepositorySearch, RegistryListener, Initializable
{
    /**
     * @plexus.requirement role-hint="bytecode"
     */
    private Transformer bytecodeIndexTransformer;

    /**
     * @plexus.requirement role-hint="filecontent"
     */
    private Transformer filecontentIndexTransformer;

    /**
     * @plexus.requirement role-hint="hashcodes"
     */
    private Transformer hashcodesIndexTransformer;

    /**
     * @plexus.requirement role-hint="searchable"
     */
    private Transformer searchableTransformer;

    /**
     * @plexus.requirement role-hint="index-exists"
     */
    private Predicate indexExistsPredicate;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    private List localIndexedRepositories = new ArrayList();

    public SearchResults searchForChecksum( String checksum, SearchResultLimits limits )
    {
        List indexes = getHashcodeIndexes();

        try
        {
            QueryParser parser = new MultiFieldQueryParser( new String[] { HashcodesKeys.MD5, HashcodesKeys.SHA1 },
                                                            new HashcodesHandlers().getAnalyzer() );
            LuceneQuery query = new LuceneQuery( parser.parse( checksum ) );
            SearchResults results = searchAll( query, limits, indexes );
            results.getRepositories().addAll( this.localIndexedRepositories );

            return results;
        }
        catch ( ParseException e )
        {
            getLogger().warn( "Unable to parse query [" + checksum + "]: " + e.getMessage(), e );
        }

        // empty results.
        return new SearchResults();
    }

    public SearchResults searchForBytecode( String term, SearchResultLimits limits )
    {
        List indexes = getHashcodeIndexes();

        try
        {
            QueryParser parser = new BytecodeHandlers().getQueryParser();
            LuceneQuery query = new LuceneQuery( parser.parse( term ) );
            SearchResults results = searchAll( query, limits, indexes );
            results.getRepositories().addAll( this.localIndexedRepositories );

            return results;
        }
        catch ( ParseException e )
        {
            getLogger().warn( "Unable to parse query [" + term + "]: " + e.getMessage(), e );
        }

        // empty results.
        return new SearchResults();
    }

    public SearchResults searchForTerm( String term, SearchResultLimits limits )
    {
        List indexes = getFileContentIndexes();

        try
        {
            QueryParser parser = new FileContentHandlers().getQueryParser();
            LuceneQuery query = new LuceneQuery( parser.parse( term ) );
            SearchResults results = searchAll( query, limits, indexes );
            results.getRepositories().addAll( this.localIndexedRepositories );

            return results;
        }
        catch ( ParseException e )
        {
            getLogger().warn( "Unable to parse query [" + term + "]: " + e.getMessage(), e );
        }

        // empty results.
        return new SearchResults();
    }

    private SearchResults searchAll( LuceneQuery luceneQuery, SearchResultLimits limits, List indexes )
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
        RepositoryContentIndex index = (RepositoryContentIndex) indexes.get( 0 );
        converter = index.getEntryConverter();

        // Process indexes into an array of Searchables.
        List searchableList = new ArrayList( indexes );
        CollectionUtils.transform( searchableList, searchableTransformer );

        Searchable searchables[] = new Searchable[searchableList.size()];
        searchableList.toArray( searchables );

        try
        {
            // Create a multi-searcher for looking up the information.
            MultiSearcher searcher = new MultiSearcher( searchables );

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
                        getLogger().warn( "Unable to parse document into record: " + e.getMessage(), e );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            getLogger().error( "Unable to setup multi-search: " + e.getMessage(), e );
        }

        return results;
    }

    private Predicate getAllowedToSearchReposPredicate()
    {
        return new UserAllowedToSearchRepositoryPredicate();
    }

    public List getBytecodeIndexes()
    {
        List ret = new ArrayList();

        synchronized ( this.localIndexedRepositories )
        {
            ret.addAll( CollectionUtils.select( this.localIndexedRepositories, getAllowedToSearchReposPredicate() ) );
            CollectionUtils.transform( ret, bytecodeIndexTransformer );
            CollectionUtils.filter( ret, indexExistsPredicate );
        }

        return ret;
    }

    public List getFileContentIndexes()
    {
        List ret = new ArrayList();

        synchronized ( this.localIndexedRepositories )
        {
            ret.addAll( CollectionUtils.select( this.localIndexedRepositories, getAllowedToSearchReposPredicate() ) );
            CollectionUtils.transform( ret, filecontentIndexTransformer );
            CollectionUtils.filter( ret, indexExistsPredicate );
        }

        return ret;
    }

    public List getHashcodeIndexes()
    {
        List ret = new ArrayList();

        synchronized ( this.localIndexedRepositories )
        {
            ret.addAll( CollectionUtils.select( this.localIndexedRepositories, getAllowedToSearchReposPredicate() ) );
            CollectionUtils.transform( ret, hashcodesIndexTransformer );
            CollectionUtils.filter( ret, indexExistsPredicate );
        }

        return ret;
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
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

            Predicate localIndexedRepos = AndPredicate.getInstance( LocalRepositoryPredicate.getInstance(),
                                                                    IndexedRepositoryPredicate.getInstance() );

            Collection repos = CollectionUtils.select( configuration.getConfiguration().getRepositories(),
                                                       localIndexedRepos );
            
            Transformer toArchivaRepository = new Transformer()
            {

                public Object transform( Object input )
                {
                    if ( input instanceof RepositoryConfiguration )
                    {
                        return ArchivaConfigurationAdaptor.toArchivaRepository( (RepositoryConfiguration) input );
                    }
                    return input;
                }
            };

            CollectionUtils.transform( repos, toArchivaRepository );

            this.localIndexedRepositories.addAll( repos );
        }
    }

    public void initialize()
        throws InitializationException
    {
        initRepositories();
        configuration.addChangeListener( this );
    }
}
