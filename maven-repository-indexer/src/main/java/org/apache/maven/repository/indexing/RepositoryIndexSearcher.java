package org.apache.maven.repository.indexing;

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

import org.apache.maven.repository.indexing.query.Query;

import java.util.List;

/**
 * 
 */
public interface RepositoryIndexSearcher
{
    /**
     * Search the artifact based on the search criteria specified in the query object. Returns a list of
     * artifact objects.
     *
     * @param query The query object that contains the search criteria.
     * @return List
     * @throws RepositoryIndexSearchException
     */
    List search( Query query )
        throws RepositoryIndexSearchException;
}
