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

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold multiple SinglePhraseQueries and/or other CompoundQueries.
 *
 * @author Edwin Punzalan
 */
public class CompoundQuery
    implements Query
{
    protected List queries;

    /**
     * Class constructor
     */
    public CompoundQuery()
    {
        queries = new ArrayList();
    }

    /**
     * Appends a required Query object to this Query object. The Query object will be encapsulated inside an
     * AndQueryTerm object.
     *
     * @param query the Query object to be appended to this Query object
     */
    public void and( Query query )
    {
        queries.add( new AndQueryTerm( query ) );
    }

    /**
     * Appends an optional Query object to this Query object. The Query object will be encapsulated inside an
     * OrQueryTerm object.
     *
     * @param query the Query object to be appended to this Query object
     */
    public void or( Query query )
    {
        queries.add( new OrQueryTerm( query ) );
    }

    /**
     * Appends a prohibited Query object to this Query object. The Query object will be encapsulated inside an
     * NotQueryTerm object.
     *
     * @param query the Query object to be appended to this Query object
     */
    public void not( Query query )
    {
        queries.add( new NotQueryTerm( query ) );
    }

    /**
     * Method to get the List of Queries appended into this
     *
     * @return List of all Queries added to this Query
     */
    public List getQueries()
    {
        return queries;
    }
}
