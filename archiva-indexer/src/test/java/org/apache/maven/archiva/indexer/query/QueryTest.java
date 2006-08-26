package org.apache.maven.archiva.indexer.query;

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

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author Brett Porter
 */
public class QueryTest
    extends TestCase
{
    private QueryTerm term1 = new QueryTerm( "field1", "value1" );

    private QueryTerm term2 = new QueryTerm( "field2", "value2" );

    private QueryTerm term3 = new QueryTerm( "field3", "value3" );

    public void testQueryTerm()
    {
        QueryTerm query = new QueryTerm( "Field", "Value" );
        assertEquals( "check field setting", "Field", query.getField() );
        assertEquals( "check value setting", "Value", query.getValue() );
    }

    public void testSingleTermQuery()
    {
        SingleTermQuery query = new SingleTermQuery( "Field", "Value" );
        assertEquals( "check field setting", "Field", query.getField() );
        assertEquals( "check value setting", "Value", query.getValue() );

        query = new SingleTermQuery( term1 );
        assertEquals( "check field setting", "field1", query.getField() );
        assertEquals( "check value setting", "value1", query.getValue() );
    }

    public void testRangeQueryOpen()
    {
        RangeQuery rangeQuery = RangeQuery.createOpenRange();
        assertNull( "Check range has no start", rangeQuery.getBegin() );
        assertNull( "Check range has no end", rangeQuery.getEnd() );
    }

    public void testRangeQueryExclusive()
    {
        RangeQuery rangeQuery = RangeQuery.createExclusiveRange( term1, term2 );
        assertEquals( "Check range start", term1, rangeQuery.getBegin() );
        assertEquals( "Check range end", term2, rangeQuery.getEnd() );
        assertFalse( "Check exclusive", rangeQuery.isInclusive() );
    }

    public void testRangeQueryInclusive()
    {
        RangeQuery rangeQuery = RangeQuery.createInclusiveRange( term1, term2 );
        assertEquals( "Check range start", term1, rangeQuery.getBegin() );
        assertEquals( "Check range end", term2, rangeQuery.getEnd() );
        assertTrue( "Check inclusive", rangeQuery.isInclusive() );
    }

    public void testRangeQueryOpenEnded()
    {
        RangeQuery rangeQuery = RangeQuery.createGreaterThanOrEqualToRange( term1 );
        assertEquals( "Check range start", term1, rangeQuery.getBegin() );
        assertNull( "Check range end", rangeQuery.getEnd() );
        assertTrue( "Check inclusive", rangeQuery.isInclusive() );

        rangeQuery = RangeQuery.createGreaterThanRange( term1 );
        assertEquals( "Check range start", term1, rangeQuery.getBegin() );
        assertNull( "Check range end", rangeQuery.getEnd() );
        assertFalse( "Check exclusive", rangeQuery.isInclusive() );

        rangeQuery = RangeQuery.createLessThanOrEqualToRange( term1 );
        assertNull( "Check range start", rangeQuery.getBegin() );
        assertEquals( "Check range end", term1, rangeQuery.getEnd() );
        assertTrue( "Check inclusive", rangeQuery.isInclusive() );

        rangeQuery = RangeQuery.createLessThanRange( term1 );
        assertNull( "Check range start", rangeQuery.getBegin() );
        assertEquals( "Check range end", term1, rangeQuery.getEnd() );
        assertFalse( "Check exclusive", rangeQuery.isInclusive() );
    }

    public void testCompundQuery()
    {
        CompoundQuery query = new CompoundQuery();
        assertTrue( "check query is empty", query.getCompoundQueryTerms().isEmpty() );

        query.and( term1 );
        query.or( term2 );
        query.not( term3 );

        Iterator i = query.getCompoundQueryTerms().iterator();
        CompoundQueryTerm term = (CompoundQueryTerm) i.next();
        assertEquals( "Check first term", "field1", getQuery( term ).getField() );
        assertEquals( "Check first term", "value1", getQuery( term ).getValue() );
        assertTrue( "Check first term", term.isRequired() );
        assertFalse( "Check first term", term.isProhibited() );

        term = (CompoundQueryTerm) i.next();
        assertEquals( "Check second term", "field2", getQuery( term ).getField() );
        assertEquals( "Check second term", "value2", getQuery( term ).getValue() );
        assertFalse( "Check second term", term.isRequired() );
        assertFalse( "Check second term", term.isProhibited() );

        term = (CompoundQueryTerm) i.next();
        assertEquals( "Check third term", "field3", getQuery( term ).getField() );
        assertEquals( "Check third term", "value3", getQuery( term ).getValue() );
        assertFalse( "Check third term", term.isRequired() );
        assertTrue( "Check third term", term.isProhibited() );

        CompoundQuery query2 = new CompoundQuery();
        query2.and( query );
        query2.or( new SingleTermQuery( term2 ) );
        query2.not( new SingleTermQuery( term3 ) );

        i = query2.getCompoundQueryTerms().iterator();
        term = (CompoundQueryTerm) i.next();
        assertEquals( "Check first term", query, term.getQuery() );
        assertTrue( "Check first term", term.isRequired() );
        assertFalse( "Check first term", term.isProhibited() );

        term = (CompoundQueryTerm) i.next();
        assertEquals( "Check second term", "field2", getQuery( term ).getField() );
        assertEquals( "Check second term", "value2", getQuery( term ).getValue() );
        assertFalse( "Check second term", term.isRequired() );
        assertFalse( "Check second term", term.isProhibited() );

        term = (CompoundQueryTerm) i.next();
        assertEquals( "Check third term", "field3", getQuery( term ).getField() );
        assertEquals( "Check third term", "value3", getQuery( term ).getValue() );
        assertFalse( "Check third term", term.isRequired() );
        assertTrue( "Check third term", term.isProhibited() );
    }

    private static SingleTermQuery getQuery( CompoundQueryTerm term )
    {
        return (SingleTermQuery) term.getQuery();
    }
}

