package org.apache.maven.archiva.indexer.lucene;

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

import org.apache.maven.archiva.indexer.query.Query;

/**
 * A holder for a lucene query to pass to the indexer API.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneQuery
    implements Query
{
    private final org.apache.lucene.search.Query query;

    public LuceneQuery( org.apache.lucene.search.Query query )
    {
        this.query = query;
    }

    org.apache.lucene.search.Query getLuceneQuery()
    {
        return query;
    }
}
