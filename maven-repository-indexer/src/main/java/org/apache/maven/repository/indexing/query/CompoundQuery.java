package org.apache.maven.repository.indexing.query;

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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.repository.indexing.RepositoryIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold multiple SinglePhraseQueries and/or other CompoundQueries.
 *
 * @author Edwin Punzalan
 */
public class CompoundQuery
    implements Query
{
    /**
     * The query terms.
     */
    private final List compoundQueryTerms = new ArrayList();

    /**
     * Appends a required term to this query.
     *
     * @param term the term to be appended to this query
     */
    public void and( QueryTerm term )
    {
        compoundQueryTerms.add( CompoundQueryTerm.and( new SingleTermQuery( term ) ) );
    }

    /**
     * Appends an optional term to this query.
     *
     * @param term the term to be appended to this query
     */
    public void or( QueryTerm term )
    {
        compoundQueryTerms.add( CompoundQueryTerm.or( new SingleTermQuery( term ) ) );
    }

    /**
     * Appends a prohibited term to this query.
     *
     * @param term the term to be appended to this query
     */
    public void not( QueryTerm term )
    {
        compoundQueryTerms.add( CompoundQueryTerm.not( new SingleTermQuery( term ) ) );
    }

    /**
     * Appends a required subquery to this query.
     *
     * @param query the subquery to be appended to this query
     */
    public void and( Query query )
    {
        compoundQueryTerms.add( CompoundQueryTerm.and( query ) );
    }

    /**
     * Appends an optional subquery to this query.
     *
     * @param query the subquery to be appended to this query
     */
    public void or( Query query )
    {
        compoundQueryTerms.add( CompoundQueryTerm.or( query ) );
    }

    /**
     * Appends a prohibited subquery to this query.
     *
     * @param query the subquery to be appended to this query
     */
    public void not( Query query )
    {
        compoundQueryTerms.add( CompoundQueryTerm.not( query ) );
    }

    /**
     * Method to get the List of Queries appended into this
     *
     * @return List of all Queries added to this Query
     */
    public List getCompoundQueryTerms()
    {
        return compoundQueryTerms;
    }

    // TODO! relocate
    public org.apache.lucene.search.Query createLuceneQuery( RepositoryIndex index )
        throws ParseException
    {
        BooleanQuery booleanQuery = new BooleanQuery();
        List queries = this.compoundQueryTerms;
        for ( Iterator i = queries.iterator(); i.hasNext(); )
        {
            CompoundQueryTerm queryTerm = (CompoundQueryTerm) i.next();

            booleanQuery.add( queryTerm.getQuery().createLuceneQuery( index ), queryTerm.isRequired()
                ? BooleanClause.Occur.MUST
                : queryTerm.isProhibited() ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.SHOULD );
        }
        return booleanQuery;
    }
}
