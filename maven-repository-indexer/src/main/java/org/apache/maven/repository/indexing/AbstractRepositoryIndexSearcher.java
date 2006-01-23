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
import org.apache.maven.repository.indexing.query.RangeQuery;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract Class to hold common codes for the different RepositoryIndexSearcher
 */
public abstract class AbstractRepositoryIndexSearcher
    extends AbstractLogEnabled
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
     * @see RepositoryIndexSearcher#search(org.apache.maven.repository.indexing.query.Query)
     */
    public List search( Query query )
        throws RepositoryIndexSearchException
    {

        org.apache.lucene.search.Query luceneQuery;
        try
        {
            luceneQuery = createLuceneQuery( query );
        }
        catch ( ParseException e )
        {
            throw new RepositoryIndexSearchException( "Unable to construct query: " + e.getMessage(), e );
        }

        IndexSearcher searcher;
        try
        {
            searcher = new IndexSearcher( index.getIndexPath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
        }

        List docs;
        try
        {
            Hits hits = searcher.search( luceneQuery );
            docs = buildList( hits );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
        }
        finally
        {
            try
            {
                searcher.close();
            }
            catch ( IOException e )
            {
                getLogger().error( "Unable to close index searcher", e );
            }
        }

        return docs;
    }

    /**
     * Method to create a lucene Query object from a single query phrase
     *
     * @param field the index field name to search into
     * @param value the index field value to match the field with
     * @return a lucene Query object representing the query phrase field = value
     * @throws ParseException
     */
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

    /**
     * Method to create a lucene Query object by converting a prepared Query object
     *
     * @param query the prepared Query object to be converted into a lucene Query object
     * @return a lucene Query object to represent the passed Query object
     * @throws ParseException
     */
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
        else if ( query instanceof RangeQuery )
        {
            RangeQuery rq = (RangeQuery) query;
            List queries = rq.getQueries();
            Iterator iter = queries.iterator();
            Term begin = null, end = null;
            if ( queries.size() == 2 )
            {
                SinglePhraseQuery qry = (SinglePhraseQuery) iter.next();
                begin = new Term( qry.getField(), qry.getValue() );
                qry = (SinglePhraseQuery) iter.next();
                end = new Term( qry.getField(), qry.getValue() );
            }
            retVal = new org.apache.lucene.search.RangeQuery( begin, end, rq.isInclusive() );
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
