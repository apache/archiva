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
 * SearchResultLimits - used to provide the search some limits on how the results are returned.
 * This can provide paging for the 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class SearchResultLimits
{
    /** 
     * Constant to use for {@link #setSelectedPage(int)} to indicate a desire to get ALL PAGES.
     * USE WITH CAUTION!!
     */
    public static final int ALL_PAGES = ( -1 );

    private int pageSize = 30;

    private int selectedPage = 0;

    public SearchResultLimits( int selectedPage )
    {
        this.selectedPage = selectedPage;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    /**
     * Set page size for maximum # of hits to return per page.
     * 
     * @param pageSize size of page by # of hits. (maximum value is 200)
     */
    public void setPageSize( int pageSize )
    {
        this.pageSize = Math.min( 200, pageSize );
    }

    public int getSelectedPage()
    {
        return selectedPage;
    }

    public void setSelectedPage( int selectedPage )
    {
        this.selectedPage = selectedPage;
    }
}
