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

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Hits;
import org.apache.lucene.document.Document;
import org.apache.maven.repository.indexing.query.OptionalQuery;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.RequiredQuery;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.apache.maven.repository.indexing.query.AbstractCompoundQuery;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 *
 */
public abstract class AbstractRepositoryIndexSearcher
    implements RepositoryIndexSearcher
{
    protected ArtifactRepositoryIndex index;

    private BooleanQuery bQry;

    private BooleanQuery mainQry;

    private boolean isRequired = true;

    /**
     * Constructor
     *
     * @param index   the index object
     */
    public AbstractRepositoryIndexSearcher( ArtifactRepositoryIndex index )
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
        List artifactList = null;
        IndexSearcher searcher = null;
        Hits hits = null;

        try
        {
            searcher = new IndexSearcher( index.getIndexPath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( e.getMessage(), e );
        }

        if ( query instanceof SinglePhraseQuery )
        {
            SinglePhraseQuery singleQry = (SinglePhraseQuery) query;
            createSubQuery();
            try
            {
                addQuery( singleQry.getField(), singleQry.getValue(), true, false );
                hits = searcher.search( bQry );
            }
            catch ( IOException ie )
            {
                throw new RepositoryIndexSearchException( ie.getMessage(), ie );
            }
            catch ( ParseException pe )
            {
                throw new RepositoryIndexSearchException( pe.getMessage(), pe );
            }

        }
        else if ( query instanceof RequiredQuery || query instanceof OptionalQuery )
        {
            createMainQuery();
            try
            {
                buildCompoundQuery( query );
                hits = searcher.search( mainQry );
            }
            catch ( IOException ie )
            {
                throw new RepositoryIndexSearchException( ie.getMessage(), ie );
            }
            catch ( ParseException pe )
            {
                throw new RepositoryIndexSearchException( pe.getMessage(), pe );
            }
        }

        try
        {
            artifactList = buildList( hits );
            searcher.close();
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
            throw new RepositoryIndexSearchException( ie.getMessage(), ie );
        }

        return artifactList;
    }

    /**
     * Create a main BooleanQuery object that will contain the other
     * BooleanQuery objects.
     */
    private void createMainQuery()
    {
        mainQry = new BooleanQuery();
    }

    /**
     * Add the other BooleanQuery objects to the main BooleanQuery object
     *
     * @param required   specifies if the search is AND or OR
     * @param prohibited specifies if NOT will be used in the search
     */
    private void addToMainQuery( boolean required, boolean prohibited )
    {
        mainQry.add( bQry, required, prohibited );
    }

    /**
     * Create a new BooleanQuery object for nested search
     */
    private void createSubQuery()
    {
        bQry = new BooleanQuery();
    }

    /**
     * Add query to the globally declared BooleanQuery object
     *
     * @param field      the name of the field in the index where the value is to be searched
     * @param value      the value to be searched in the index
     * @param required   specifies if the search is AND or OR
     * @param prohibited specifies if NOT will be used in the search
     * @throws ParseException
     */
    private void addQuery( String field, String value, boolean required, boolean prohibited )
        throws ParseException
    {
        QueryParser parser = new QueryParser( field, index.getAnalyzer() );
        org.apache.lucene.search.Query qry = parser.parse( value );
        bQry.add( qry, required, prohibited );
    }

    /**
     * Build or construct the query that will be used by the searcher
     *
     * @param query the query object that contains the search criteria
     * @throws ParseException
     */
    private void buildCompoundQuery( Query query )
        throws ParseException
    {
        AbstractCompoundQuery cQry = null;
        boolean required = false;

        if ( query instanceof RequiredQuery )
        {
            cQry = (RequiredQuery) query;
            required = true;
        }
        else
        {
            cQry = (OptionalQuery) query;
            required = false;
        }

        boolean reset = true;

        // get the query list and iterate through each
        List queries = cQry.getQueryList();
        for ( Iterator iter = queries.iterator(); iter.hasNext(); )
        {
            Query query2 = (Query) iter.next();

            if ( query2 instanceof SinglePhraseQuery )
            {
                SinglePhraseQuery sQry = (SinglePhraseQuery) query2;
                if ( reset )
                {
                    createSubQuery();
                }
                addQuery( sQry.getField(), sQry.getValue(), required, false );
                reset = false;

                if ( !iter.hasNext() )
                {
                    addToMainQuery( isRequired, false );
                }

            }
            else if ( query2 instanceof RequiredQuery || query2 instanceof OptionalQuery )
            {
                isRequired = required;
                buildCompoundQuery( query2 );
            }
        }
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
