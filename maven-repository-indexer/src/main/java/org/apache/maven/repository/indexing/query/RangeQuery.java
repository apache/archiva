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

/**
 * Query object that handles range queries (presently used for dates).
 *
 * @author Maria Odea Ching
 * @author Brett Porter
 */
public class RangeQuery
    implements Query
{
    /**
     * Whether values equal to the boundaries are included in the query results.
     */
    private final boolean inclusive;

    /**
     * The lower bound.
     */
    private final QueryTerm begin;

    /**
     * The upper bound.
     */
    private final QueryTerm end;

    /**
     * Constructor.
     *
     * @param begin     the lower bound
     * @param end       the upper bound
     * @param inclusive whether to include the boundaries in the query
     */
    private RangeQuery( QueryTerm begin, QueryTerm end, boolean inclusive )
    {
        this.begin = begin;
        this.end = end;
        this.inclusive = inclusive;
    }

    /**
     * Create an open range, including all results.
     *
     * @return the query object
     */
    public static RangeQuery createOpenRange()
    {
        return new RangeQuery( null, null, false );
    }

    /**
     * Create a bounded range, excluding the endpoints.
     *
     * @param begin the lower bound value to compare to
     * @param end   the upper bound value to compare to
     * @return the query object
     */
    public static RangeQuery createExclusiveRange( QueryTerm begin, QueryTerm end )
    {
        return new RangeQuery( begin, end, false );
    }

    /**
     * Create a bounded range, including the endpoints.
     *
     * @param begin the lower bound value to compare to
     * @param end   the upper bound value to compare to
     * @return the query object
     */
    public static RangeQuery createInclusiveRange( QueryTerm begin, QueryTerm end )
    {
        return new RangeQuery( begin, end, true );
    }

    /**
     * Create a range that is greater than or equal to a given term.
     *
     * @param begin the value to compare to
     * @return the query object
     */
    public static RangeQuery createGreaterThanOrEqualToRange( QueryTerm begin )
    {
        return new RangeQuery( begin, null, true );
    }

    /**
     * Create a range that is greater than a given term.
     *
     * @param begin the value to compare to
     * @return the query object
     */
    public static RangeQuery createGreaterThanRange( QueryTerm begin )
    {
        return new RangeQuery( begin, null, false );
    }

    /**
     * Create a range that is less than or equal to a given term.
     *
     * @param end the value to compare to
     * @return the query object
     */
    public static RangeQuery createLessThanOrEqualToRange( QueryTerm end )
    {
        return new RangeQuery( null, end, true );
    }

    /**
     * Create a range that is less than a given term.
     *
     * @param end the value to compare to
     * @return the query object
     */
    public static RangeQuery createLessThanRange( QueryTerm end )
    {
        return new RangeQuery( null, end, false );
    }

    public QueryTerm getBegin()
    {
        return begin;
    }

    public QueryTerm getEnd()
    {
        return end;
    }

    public boolean isInclusive()
    {
        return inclusive;
    }

}
