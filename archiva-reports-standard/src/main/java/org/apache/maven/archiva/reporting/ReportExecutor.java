package org.apache.maven.archiva.reporting;

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

import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.List;

/**
 * Executes a report or report group.
 */
public interface ReportExecutor
{
    /**
     * Plexus component role name.
     */
    String ROLE = ReportExecutor.class.getName();

    /**
     * Run reports on a set of metadata.
     *
     * @param reportGroup the report set to run
     * @param metadata    the RepositoryMetadata objects to report on
     * @param repository  the repository that they come from
     * @throws ReportingStoreException if there is a problem reading/writing the report database
     */
    public void runMetadataReports( ReportGroup reportGroup, List metadata, ArtifactRepository repository )
        throws ReportingStoreException;

    /**
     * Run reports on a set of artifacts.
     *
     * @param reportGroup the report set to run
     * @param artifacts   the Artifact objects to report on
     * @param repository  the repository that they come from
     * @throws ReportingStoreException if there is a problem reading/writing the report database
     */
    public void runArtifactReports( ReportGroup reportGroup, List artifacts, ArtifactRepository repository )
        throws ReportingStoreException;

    /**
     * Get the report database in use for a given repository.
     *
     * @param repository  the repository
     * @param reportGroup the report set to run
     * @return the report database
     * @throws ReportingStoreException if there is a problem reading the report database
     */
    ReportingDatabase getReportDatabase( ArtifactRepository repository, ReportGroup reportGroup )
        throws ReportingStoreException;

    /**
     * Run the artifact and metadata reports for the repository. The artifacts and metadata will be discovered.
     *
     * @param repository          the repository to run from
     * @param blacklistedPatterns the patterns to exclude during discovery
     * @param filter              the filter to use during discovery to get a consistent list of artifacts
     * @param reportGroup         the report set to run
     * @throws ReportingStoreException if there is a problem reading/writing the report database
     * @throws org.apache.maven.archiva.discoverer.DiscovererException
     *                                 if there is a problem finding the artifacts and metadata to report on
     */
    public void runReports( ReportGroup reportGroup, ArtifactRepository repository, List blacklistedPatterns,
                            ArtifactFilter filter )
        throws DiscovererException, ReportingStoreException;
}
