package org.apache.maven.archiva.reporting;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Limits on how much data should be returned by the report sources.
 *
 * @version $Id$
 */
public class DataLimits
{
    private int currentPage = 0;

    private int perPageCount = 25;

    private int countOfPages = 1;

    private int totalCount = 0;

    public int getCountOfPages()
    {
        return countOfPages;
    }

    public void setCountOfPages( int countOfPages )
    {
        this.countOfPages = countOfPages;
    }

    public int getCurrentPage()
    {
        return currentPage;
    }

    public void setCurrentPage( int currentPage )
    {
        this.currentPage = currentPage;
    }

    public int getPerPageCount()
    {
        return perPageCount;
    }

    public void setPerPageCount( int perPageCount )
    {
        this.perPageCount = perPageCount;
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount( int totalCount )
    {
        this.totalCount = totalCount;
    }
}
