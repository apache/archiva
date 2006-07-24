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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Interface for implementation that can discover artifacts within a repository.
 *
 * @author John Casey
 * @author Brett Porter
 * @todo do we want blacklisted patterns in another form? Part of the object construction?
 * @todo should includeSnapshots be configuration on the component? If not, should the methods be changed to include alternates for both possibilities (discoverReleaseArtifacts, discoverReleaseAndSnapshotArtifacts)?
 * @todo instead of a returned list, should a listener be passed in?
 */
public interface ArtifactDiscoverer
    extends Discoverer
{
    String ROLE = ArtifactDiscoverer.class.getName();

    /**
     * Discover artifacts in the repository. Only artifacts added since the last attempt at discovery will be found.
     * This process guarantees never to miss an artifact, however it is possible that an artifact will be received twice
     * consecutively even if unchanged, so any users of this list must handle such a situation gracefully.
     *
     * @param repository          the location of the repository
     * @param operation           the operation being used to discover for timestamp checking
     * @param blacklistedPatterns pattern that lists any files to prevent from being included when scanning
     * @param includeSnapshots    whether to discover snapshots
     * @return the list of artifacts discovered
     * @throws DiscovererException if there was an unrecoverable problem discovering artifacts or recording progress
     */
    List discoverArtifacts( ArtifactRepository repository, String operation, String blacklistedPatterns,
                            boolean includeSnapshots )
        throws DiscovererException;

    /**
     * Build an artifact from a path in the repository
     *
     * @param path the path
     * @return the artifact
     * @throws DiscovererException if the file is not a valid artifact
     * @todo this should be in maven-artifact
     */
    Artifact buildArtifact( String path )
        throws DiscovererException;

    /**
     * Reset the time in the repository that indicates the last time a check was performed.
     *
     * @param repository the location of the repository
     * @param operation  the operation to record the timestamp for
     * @throws java.io.IOException if there is a non-recoverable problem reading or writing the metadata
     */
    void resetLastCheckedTime( ArtifactRepository repository, String operation )
        throws IOException;

    /**
     * Set the time in the repository that indicates the last time a check was performed.
     *
     * @param repository the location of the repository
     * @param operation  the operation to record the timestamp for
     * @param date       the date to set the last check to
     * @throws java.io.IOException if there is a non-recoverable problem reading or writing the metadata
     */
    void setLastCheckedTime( ArtifactRepository repository, String operation, Date date )
        throws IOException;
}
