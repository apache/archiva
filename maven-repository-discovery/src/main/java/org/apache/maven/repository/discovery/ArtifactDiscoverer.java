package org.apache.maven.repository.discovery;

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

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Iterator;
import java.util.List;

/**
 * Interface for implementation that can discover artifacts within a repository.
 *
 * @author John Casey
 * @author Brett Porter
 */
public interface ArtifactDiscoverer
{
    String ROLE = ArtifactDiscoverer.class.getName();

    /**
     * Discover artifacts in the repository.
     *
     * @param repository          the location of the repository
     * @param blacklistedPatterns pattern that lists any files to prevent from being included when scanning
     * @param includeSnapshots    whether to discover snapshots
     * @return the list of artifacts discovered
     * @todo replace repositoryBase with wagon repository
     * @todo do we want blacklisted patterns in another form? Part of the object construction?
     * @todo should includeSnapshots be configuration on the component?
     * @todo instead of a returned list, should a listener be passed in?
     */
    List discoverArtifacts( ArtifactRepository repository, String blacklistedPatterns, boolean includeSnapshots );

    /**
     * Discover standalone POM artifacts in the repository.
     *
     * @param repository          the location of the repository
     * @param blacklistedPatterns pattern that lists any files to prevent from being included when scanning
     * @param includeSnapshots    whether to discover snapshots
     * @return the list of artifacts discovered
     * @todo replace repositoryBase with wagon repository
     * @todo do we want blacklisted patterns in another form? Part of the object construction?
     * @todo should includeSnapshots be configuration on the component?
     * @todo instead of a returned list, should a listener be passed in?
     */
    List discoverStandalonePoms( ArtifactRepository repository, String blacklistedPatterns, boolean includeSnapshots );

    /**
     * Get the list of paths kicked out during the discovery process.
     *
     * @return the paths as Strings.
     */
    Iterator getKickedOutPathsIterator();

    /**
     * Get the list of paths excluded during the discovery process.
     *
     * @return the paths as Strings.
     */
    Iterator getExcludedPathsIterator();
}
