package org.apache.maven.archiva.database.browsing;

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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaProjectModel;

import java.util.List;

/**
 * Repository Browsing component 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface RepositoryBrowsing
{
    /**
     * Get the {@link BrowsingResults} for the root of the repository.
     * 
     * @return the root browsing results.
     */
    public BrowsingResults getRoot( String principle, List<String> observableRepositoryIds );

    /**
     * Get the {@link BrowsingResults} for the selected groupId.
     * 
     * @param groupId the groupId to select.
     * @return the {@link BrowsingResults} for the specified groupId.
     */
    public BrowsingResults selectGroupId( String principle, List<String> observableRepositoryIds, String groupId );

    /**
     * Get the {@link BrowsingResults} for the selected groupId & artifactId.
     * 
     * @param groupId the groupId selected
     * @param artifactId the artifactId selected
     * @return the {@link BrowsingResults} for the specified groupId / artifactId combo.
     */
    public BrowsingResults selectArtifactId( String principle, List<String> observableRepositoryIds, String groupId,
                                             String artifactId );

    /**
     * Get the {@link ArchivaProjectModel} for the selected groupId / artifactId / version combo.
     * 
     * @param groupId the groupId selected
     * @param artifactId the artifactId selected
     * @param version the version selected
     * @return the {@link ArchivaProjectModel} for the selected groupId / artifactId / version combo.
     * @throws ObjectNotFoundException if the artifact object or project object isn't found in the database.
     * @throws ArchivaDatabaseException if there is a fundamental database error.
     */
    public ArchivaProjectModel selectVersion( String principle, List<String> observableRepositoryIds, String groupId,
                                              String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException;

    /**
     * Get the {@link List} of {@link ArchivaProjectModel} that are used by the provided
     * groupId, artifactId, and version specified.
     * 
     * @param groupId the groupId selected
     * @param artifactId the artifactId selected
     * @param version the version selected
     * @return the {@link List} of {@link ArchivaProjectModel} objects. (never null, but can be empty)
     * @throws ArchivaDatabaseException if there is a fundamental database error.
     */
    public List<ArchivaProjectModel> getUsedBy( String principle, List<String> observableRepositoryIds, String groupId,
                                                String artifactId, String version )
        throws ArchivaDatabaseException;

    
    public String getRepositoryId( String principle, List<String> observableRepositoryIds, String groupId,
                                       String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException;
}
