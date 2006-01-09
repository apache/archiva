package org.apache.maven.repository.indexing;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.repository.indexing.query.CompoundQuery;
import org.apache.maven.repository.indexing.query.CompoundQueryTerm;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public abstract class AbstractRepositoryIndexSearcher
    implements RepositoryIndexSearcher
{
    protected RepositoryIndex index;

    /**
     * Constructor
     *
     * @param index the index object
     */
    protected AbstractRepositoryIndexSearcher( RepositoryIndex index )
    {
        this.index = index;
    }

    /**
     * Search the artifact based on the search criteria specified in the query
     * object. Returns a list of artifact objects
     *
     * @param query the query object that contains the search criteria
     * @return List
     * @throws RepositoryIndexSearchException
     */
    public List search( Query query )
        throws RepositoryIndexSearchException
    {
        IndexSearcher searcher;

        try
        {
            searcher = new IndexSearcher( index.getIndexPath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( e.getMessage(), e );
        }

        Hits hits;
        try
        {
            hits = searcher.search( createLuceneQuery( query ) );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( e.getMessage(), e );
        }
        catch ( ParseException e )
        {
            throw new RepositoryIndexSearchException( e.getMessage(), e );
        }

        List artifactList;
        try
        {
            artifactList = buildList( hits );
            searcher.close();
        }
        catch ( IOException ie )
        {
            throw new RepositoryIndexSearchException( ie.getMessage(), ie );
        }

        return artifactList;
    }

    private org.apache.lucene.search.Query createLuceneQuery( String field, String value )
        throws ParseException
    {
        org.apache.lucene.search.Query qry;
        if ( index.isKeywordField( field ) )
        {
            Term term = new Term( field, value );
            qry = new TermQuery( term );
        }
        else
        {
            QueryParser parser = new QueryParser( field, index.getAnalyzer() );
            qry = parser.parse( value );
        }
        return qry;
    }

    private org.apache.lucene.search.Query createLuceneQuery( Query query )
        throws ParseException
    {
        org.apache.lucene.search.Query retVal;

        if ( query instanceof CompoundQuery )
        {
            BooleanQuery booleanQuery = new BooleanQuery();
            CompoundQuery compoundQuery = (CompoundQuery) query;
            List queries = compoundQuery.getQueries();
            for ( Iterator i = queries.iterator(); i.hasNext(); )
            {
                CompoundQueryTerm subquery = (CompoundQueryTerm) i.next();

                org.apache.lucene.search.Query luceneQuery = createLuceneQuery( subquery.getQuery() );

                booleanQuery.add( luceneQuery, subquery.isRequired(), subquery.isProhibited() );
            }
            retVal = booleanQuery;
        }
        else
        {
            SinglePhraseQuery singlePhraseQuery = (SinglePhraseQuery) query;
            retVal = createLuceneQuery( singlePhraseQuery.getField(), singlePhraseQuery.getValue() );
        }
        return retVal;
    }

    /**
     * Create a list of artifact objects from the result set.
     *
     * @param hits the search result set
     * @return List
     * @throws IOException
     */
    private List buildList( Hits hits )
        throws IOException
    {
        List artifactList = new ArrayList();

        for ( int i = 0; i < hits.length(); i++ )
        {
            Document doc = hits.doc( i );

            artifactList.add( createSearchedObjectFromIndexDocument( doc ) );
        }

        return artifactList;
    }

    protected abstract Object createSearchedObjectFromIndexDocument( Document doc );
}
