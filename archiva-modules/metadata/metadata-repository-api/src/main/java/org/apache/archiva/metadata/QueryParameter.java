package org.apache.archiva.metadata;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * This class is used to provide additional query parameters to search queries.
 * These parameters are hints for the metadata repository implementation, some parameters may be ignored.
 *
 * The defaults are:
 * <li>
 *     <ul>Sort order: ascending</ul>
 *     <ul>Offset: 0</ul>
 *     <ul>Limit: Long.MAX_VALUE</ul>
 *     <ul>Sort fields: empty, which means it depends on the query</ul>
 * </li>
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class QueryParameter {

    final boolean ascending;
    final List<String> sortFields;
    final long offset;
    final long limit;

    public QueryParameter(boolean isAscending, long offset, long limit, String... sortFields) {
        this.ascending = isAscending;
        this.offset = offset;
        this.limit = limit;
        this.sortFields = Arrays.asList(sortFields);
    }

    public QueryParameter(long offset, long limit) {
        this.offset=offset;
        this.limit = limit;
        this.ascending = true;
        this.sortFields = Collections.emptyList();
    }

    public QueryParameter(boolean isAscending, long offset, long limit) {
        this.ascending = isAscending;
        this.offset = offset;
        this.limit = limit;
        this.sortFields = Collections.emptyList();
    }

    public QueryParameter(long limit) {
        this.offset=0;
        this.ascending=true;
        this.limit=limit;
        this.sortFields = Collections.emptyList();
    }

    public QueryParameter() {
        this.ascending = true;
        this.sortFields = Collections.emptyList();
        this.offset = 0;
        this.limit = Long.MAX_VALUE;
    }

    public boolean isAscending() {
        return ascending;
    }

    public List<String> getSortFields() {
        return sortFields;
    }

    public long getOffset() {
        return offset;
    }

    public long getLimit() {
        return limit;
    }
}
