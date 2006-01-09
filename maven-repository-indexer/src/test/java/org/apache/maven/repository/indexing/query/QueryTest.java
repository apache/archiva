package org.apache.maven.repository.indexing.query;

import junit.framework.TestCase;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * @author Edwin Punzalan
 */
public class QueryTest
    extends TestCase
{
    public void testSinglePhraseQueryObject()
    {
        SinglePhraseQuery query = new SinglePhraseQuery( "Field", "Value" );
        assertEquals( "Field", query.getField() );
        assertEquals( "Value", query.getValue() );
    }

    public void testCompoundQueries()
    {
        CompoundQuery rQuery = new CompoundQuery();
        rQuery.and( new SinglePhraseQuery( "r1Field", "r1Value" ) );
        rQuery.and( new SinglePhraseQuery( "r2Field", "r2Value" ) );

        CompoundQuery oQuery = new CompoundQuery();
        oQuery.or( new SinglePhraseQuery( "oField", "oValue" ) );

        CompoundQuery all = new CompoundQuery();
        all.and( rQuery );
        all.or( oQuery );
        assertEquals( 2, all.getQueries().size() );

        CompoundQueryTerm queryTerm = (CompoundQueryTerm) all.getQueries().get( 0 );
        assertTrue( queryTerm.getQuery() instanceof CompoundQuery );
        rQuery = (CompoundQuery) queryTerm.getQuery();
        assertEquals( 2, rQuery.getQueries().size() );
        queryTerm = (CompoundQueryTerm) rQuery.getQueries().get( 0 );
        assertTrue( queryTerm.getQuery() instanceof SinglePhraseQuery );
        SinglePhraseQuery sQuery = (SinglePhraseQuery) queryTerm.getQuery();
        assertEquals( "r1Field", sQuery.getField() );
        assertEquals( "r1Value", sQuery.getValue() );
        queryTerm = (CompoundQueryTerm) rQuery.getQueries().get( 1 );
        assertTrue( queryTerm.getQuery() instanceof SinglePhraseQuery );
        sQuery = (SinglePhraseQuery) queryTerm.getQuery();
        assertEquals( "r2Field", sQuery.getField() );
        assertEquals( "r2Value", sQuery.getValue() );

        queryTerm = (CompoundQueryTerm) all.getQueries().get( 1 );
        assertTrue( queryTerm.getQuery() instanceof CompoundQuery );
        rQuery = (CompoundQuery) queryTerm.getQuery();
        assertEquals( 1, rQuery.getQueries().size() );
        queryTerm = (CompoundQueryTerm) rQuery.getQueries().get( 0 );
        assertTrue( queryTerm.getQuery() instanceof SinglePhraseQuery );
        sQuery = (SinglePhraseQuery) queryTerm.getQuery();
        assertEquals( "oField", sQuery.getField() );
        assertEquals( "oValue", sQuery.getValue() );

    }
}

