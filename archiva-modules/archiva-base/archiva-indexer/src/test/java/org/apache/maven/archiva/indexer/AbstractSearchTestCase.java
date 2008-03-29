package org.apache.maven.archiva.indexer;

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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.ComparisonFailure;

/**
 * AbstractSearchTestCase 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractSearchTestCase
    extends AbstractIndexerTestCase
{
    protected Map records;

    protected abstract Map createSampleRecordsMap();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        records = createSampleRecordsMap();

        index.indexRecords( records.values() );
    }

    protected Query createExactMatchQuery( String field, String value )
    {
        return new TermQuery( new Term( field, value ) );
    }

    protected Query createMatchQuery( String field, String value )
        throws ParseException
    {
        QueryParser queryParser = new QueryParser( field, indexHandlers.getAnalyzer() );
        queryParser.setLowercaseExpandedTerms( true );
        return queryParser.parse( value );
    }

    protected void assertResults( String expectedKeys[], List actualResults )
    {
        if ( actualResults == null )
        {
            fail( "Got null results, expected <" + expectedKeys.length + "> results." );
        }

        if ( actualResults.isEmpty() )
        {
            fail( "Got empty results, expected <" + expectedKeys.length + "> results." );
        }

        if ( expectedKeys.length != actualResults.size() )
        {
            dumpResults( actualResults );
            throw new ComparisonFailure( "Results count", String.valueOf( expectedKeys.length ), String
                .valueOf( actualResults.size() ) );
        }

        assertEquals( "Results count", expectedKeys.length, actualResults.size() );

        for ( int i = 0; i < expectedKeys.length; i++ )
        {
            String key = expectedKeys[i];
            LuceneRepositoryContentRecord record = (LuceneRepositoryContentRecord) records.get( key );

            if ( record == null )
            {
                dumpResults( actualResults );
                fail( "Expected record <" + key
                    + "> not in records map (smack the unit test developer, tell them to fix method " + getName() + ")" );
            }

            if ( !actualResults.contains( record ) )
            {
                dumpResults( actualResults );
                fail( "Results should contain expected record: " + record );
            }
        }
    }

    protected void dumpResults( List results )
    {
        System.out.println( "Results <" + results.size() + "> - " + getName() );
        int i = 1;
        for ( Iterator iter = results.iterator(); iter.hasNext(); )
        {
            Object result = (Object) iter.next();
            System.out.println( "Result [" + ( i++ ) + "] : " + result );
        }
    }

    protected void assertNoResults( List results )
    {
        if ( results == null )
        {
            return;
        }

        if ( !results.isEmpty() )
        {
            dumpResults( results );
            fail( "Expected no results, but actually got <" + results.size() + "> entries." );
        }
    }

    protected void assertQueryExactMatchNoResults( String key, String term )
        throws Exception
    {
        Query query = createExactMatchQuery( key, term );
        List results = search( query );
        assertNoResults( results );
    }

    protected void assertQueryExactMatch( String key, String names[], String term )
        throws Exception
    {
        Query query = createExactMatchQuery( key, term );
        List results = search( query );
        assertResults( names, results );
    }

    protected void assertQueryMatch( String key, String names[], String term )
        throws Exception
    {
        Query query = createMatchQuery( key, term );
        List results = search( query );
        assertResults( names, results );
    }

    protected void assertQueryMatchNoResults( String key, String term )
        throws Exception
    {
        Query query = createMatchQuery( key, term );

        List results = search( query );

        assertNoResults( results );
    }

    protected List search( Query query )
        throws RepositoryIndexSearchException, IOException, java.text.ParseException
    {
        Searcher searcher = (Searcher) index.getSearchable();; // this shouldn't cause a problem.

        Hits hits = searcher.search( query );

        List results = new ArrayList();
        Iterator it = hits.iterator();
        while ( it.hasNext() )
        {
            Hit hit = (Hit) it.next();
            Document doc = hit.getDocument();
            LuceneRepositoryContentRecord record = index.getEntryConverter().convert( doc );
            results.add( record );
        }
        return results;
    }
}
