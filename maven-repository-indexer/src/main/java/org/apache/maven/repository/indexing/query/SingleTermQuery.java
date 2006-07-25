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

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.repository.indexing.RepositoryIndex;

/**
 * Query for a single term.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class SingleTermQuery
    implements Query
{
    /**
     * The term to query for.
     */
    private final QueryTerm term;

    /**
     * Constructor.
     *
     * @param term the term to query
     */
    public SingleTermQuery( QueryTerm term )
    {
        this.term = term;
    }

    /**
     * Shorthand constructor - create a single term query from a field and value
     *
     * @param field the field name
     * @param value the value to check for
     */
    public SingleTermQuery( String field, String value )
    {
        this.term = new QueryTerm( field, value );
    }

    /**
     * @todo! this seems like the wrong place for this (it's back to front - create the query from the index
     */
    public org.apache.lucene.search.Query createLuceneQuery( RepositoryIndex index )
        throws ParseException
    {
        org.apache.lucene.search.Query qry;
        if ( index.isKeywordField( term.getField() ) )
        {
            qry = new TermQuery( new Term( term.getField(), term.getValue() ) );
        }
        else
        {
            // TODO: doesn't seem like the right place for this here!
            QueryParser parser = new QueryParser( term.getField(), index.getAnalyzer() );
            qry = parser.parse( term.getValue() );
        }
        return qry;
    }

    public String getField()
    {
        return term.getField();
    }

    public String getValue()
    {
        return term.getValue();
    }
}
