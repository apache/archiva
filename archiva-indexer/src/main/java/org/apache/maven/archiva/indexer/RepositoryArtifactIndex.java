package org.apache.maven.archiva.indexer;

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

import org.apache.maven.archiva.indexer.query.Query;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;

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
     * @param records the records to index
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

    /**
     * Delete records from the index. Simply ignore the request any did not exist.
     *
     * @param records the records to delete
     * @throws RepositoryIndexException if there is a problem removing the record
     */
    void deleteRecords( Collection records )
        throws RepositoryIndexException;

    /**
     * Retrieve all records in the index.
     *
     * @return the records
     * @throws RepositoryIndexSearchException if there was an error searching the index
     */
    Collection getAllRecords()
        throws RepositoryIndexSearchException;

    /**
     * Retrieve all primary keys of records in the index.
     *
     * @return the keys
     * @throws RepositoryIndexException if there was an error searching the index
     */
    Collection getAllRecordKeys()
        throws RepositoryIndexException;

    /**
     * Indexes the artifacts found within the specified list. If the artifacts are already in the
     * repository they are updated. This method should use less memory than indexRecords as the records can be
     * created and disposed of on the fly.
     *
     * @param artifacts the artifacts to index
     * @param factory   the artifact to record factory
     * @throws RepositoryIndexException if there is a problem indexing the artifacts
     */
    void indexArtifacts( List artifacts, RepositoryIndexRecordFactory factory )
        throws RepositoryIndexException;

    /**
     * Get all the group IDs in the index.
     *
     * @return list of groups as strings
     * @throws RepositoryIndexException if there is a problem searching for the group ID
     */
    List getAllGroupIds()
        throws RepositoryIndexException;

    /**
     * Get the list of artifact IDs in a group in the index.
     *
     * @param groupId the group ID to search
     * @return the list of artifact ID strings
     * @throws RepositoryIndexSearchException if there is a problem searching for the group ID
     */
    List getArtifactIds( String groupId )
        throws RepositoryIndexSearchException;

    /**
     * Get the list of available versions for a given artifact.
     *
     * @param groupId    the group ID to search for
     * @param artifactId the artifact ID to search for
     * @return the list of version strings
     * @throws RepositoryIndexSearchException if there is a problem searching for the artifact
     */
    List getVersions( String groupId, String artifactId )
        throws RepositoryIndexSearchException;

    /**
     * Get the time when the index was last updated. Note that this does not monitor external processes.
     *
     * @return the last updated time, or 0 if it has not been updated since the class was instantiated.
     */
    long getLastUpdatedTime();
}
