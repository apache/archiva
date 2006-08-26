package org.apache.maven.archiva.indexing.query;

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
 * Base of all query terms.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CompoundQueryTerm
{
    /**
     * The query to add to the compound query.
     */
    private final Query query;

    /**
     * Whether the term is required (an AND).
     */
    private final boolean required;

    /**
     * Whether the term is prohibited (a NOT).
     */
    private final boolean prohibited;

    /**
     * Class constructor
     *
     * @param query      the subquery to add
     * @param required   whether the term is required (an AND)
     * @param prohibited whether the term is prohibited (a NOT)
     */
    private CompoundQueryTerm( Query query, boolean required, boolean prohibited )
    {
        this.query = query;
        this.prohibited = prohibited;
        this.required = required;
    }

    /**
     * Method to test if the Query is a search requirement
     *
     * @return true if this Query is a search requirement, otherwise returns false
     */
    public boolean isRequired()
    {
        return required;
    }

    /**
     * Method to test if the Query is prohibited in the search result
     *
     * @return true if this Query is prohibited in the search result
     */
    public boolean isProhibited()
    {
        return prohibited;
    }


    /**
     * The subquery to execute.
     *
     * @return the query
     */
    public Query getQuery()
    {
        return query;
    }

    static CompoundQueryTerm and( Query query )
    {
        return new CompoundQueryTerm( query, true, false );
    }

    static CompoundQueryTerm or( Query query )
    {
        return new CompoundQueryTerm( query, false, false );
    }

    static CompoundQueryTerm not( Query query )
    {
        return new CompoundQueryTerm( query, false, true );
    }
}
