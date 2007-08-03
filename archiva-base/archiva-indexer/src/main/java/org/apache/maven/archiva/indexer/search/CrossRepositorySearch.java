package org.apache.maven.archiva.indexer.search;

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

/**
 * Search across repositories in lucene indexes. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @todo add security to not perform search in repositories you don't have access to.
 */
public interface CrossRepositorySearch
{
    /**
     * Search for the specific term across all repositories.
     * 
     * @param term the term to search for.
     * @param limits the limits to apply to the search results.
     * @return the results.
     */
    public SearchResults searchForTerm( String term, SearchResultLimits limits );
    
    /**
     * Search for the specific bytecode across all repositories.
     * 
     * @param term the term to search for.
     * @param limits the limits to apply to the search results.
     * @return the results.
     */
    public SearchResults searchForBytecode( String term, SearchResultLimits limits );

    /**
     * Search for the specific checksum string across all repositories.
     * 
     * @param checksum the checksum string to search for.
     * @param limits the limits to apply to the search results.
     * @return the results.
     */
    public SearchResults searchForChecksum( String checksum, SearchResultLimits limits );
}
