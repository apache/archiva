package org.apache.archiva.indexer.search;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * SearchResults
 *
 */
public class SearchResults
{
    private Map<String, SearchResultHit> hits = new HashMap<>();

    private int totalHits;

    private int totalHitsMapSize;

    private int returnedHitsCount;

    private SearchResultLimits limits;

    public SearchResults()
    {
        /* do nothing */
    }

    // for new RepositorySearch
    public void addHit( String id, SearchResultHit hit )
    {
        hits.put( id, hit );
    }

    /**
     * Get the list of {@link SearchResultHit} objects.
     *
     * @return the list of {@link SearchResultHit} objects.
     */
    public List<SearchResultHit> getHits()
    {
        return new ArrayList<>( hits.values() );
    }

    /**
     * see SearchUtil on how to generate the key
     *
     * @param key
     * @return
     */
    public SearchResultHit getSearchResultHit( String key )
    {
        return hits.get( key );
    }

    public Map<String, SearchResultHit> getHitsMap()
    {
        return hits;
    }

    public boolean isEmpty()
    {
        return hits.isEmpty();
    }

    public SearchResultLimits getLimits()
    {
        return limits;
    }

    public void setLimits( SearchResultLimits limits )
    {
        this.limits = limits;
    }

    public int getTotalHits()
    {
        return totalHits;
    }

    public void setTotalHits( int totalHits )
    {
        this.totalHits = totalHits;
    }

    /**
     * @return
     * @since 1.4-M1
     */
    public int getReturnedHitsCount()
    {
        return returnedHitsCount;
    }

    /**
     * @param returnedHitsCount
     * @since 1.4-M1
     */
    public void setReturnedHitsCount( int returnedHitsCount )
    {
        this.returnedHitsCount = returnedHitsCount;
    }

    /**
     * @return
     * @since 1.4-M1
     */
    public int getTotalHitsMapSize()
    {
        return totalHitsMapSize;
    }

    /**
     * @param totalHitsMapSize
     * @since 1.4-M1
     */
    public void setTotalHitsMapSize( int totalHitsMapSize )
    {
        this.totalHitsMapSize = totalHitsMapSize;
    }

    @Override
    public String toString()
    {
        return "SearchResults{" + "hits=" + hits + ", totalHits=" + totalHits + ", returnedHitsCount="
            + returnedHitsCount + ", limits=" + limits + '}';
    }
}
