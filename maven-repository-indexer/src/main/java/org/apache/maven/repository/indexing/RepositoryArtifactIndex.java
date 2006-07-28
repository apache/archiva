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

import java.util.Collection;
import java.util.List;

/**
 * Maintain an artifact index on the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryArtifactIndex
{
    /**
     * Indexes the artifacts found within the specified list of index records. If the artifacts are already in the
     * repository they are updated.
     *
     * @param records the artifacts to index
     * @throws RepositoryIndexException if there is a problem indexing the records
     */
    void indexRecords( Collection records )
        throws RepositoryIndexException;

    /**
     * Search the index based on the search criteria specified. Returns a list of index records.
     *
     * @param query The query that contains the search criteria
     * @return the index records found
     * @throws RepositoryIndexSearchException if there is a problem searching
     * @todo should it return "SearchResult" instances that contain the index record and other search data (like score?)
     */
    List search( Query query )
        throws RepositoryIndexSearchException;

    /**
     * Check if the index already exists.
     *
     * @return true if the index already exists
     * @throws RepositoryIndexException if the index location is not valid
     */
    boolean exists()
        throws RepositoryIndexException;
}
