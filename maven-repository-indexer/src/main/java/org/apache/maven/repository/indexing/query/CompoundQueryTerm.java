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
 * Term in a compound query.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface CompoundQueryTerm
{
    /**
     * Method to test if the Query is a search requirement
     *
     * @return true if this Query is a search requirement, otherwise returns false
     */
    boolean isRequired();

    /**
     * Method to test if the Query is prohibited in the search result
     *
     * @return true if this Query is prohibited in the search result
     */
    boolean isProhibited();

    /**
     * Method to get the Query object represented by this object
     *
     * @return the Query object represented by this object
     */
    Query getQuery();
}
