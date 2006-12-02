package org.apache.maven.archiva.reporting.group;

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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;

import java.util.Map;

/**
 * A grouping or report processors for execution as a visible report from the web interface - eg, "health",
 * "old artifacts", etc.
 */
public interface ReportGroup
{
    /**
     * Plexus component role.
     */
    String ROLE = ReportGroup.class.getName();

    /**
     * Run any artifact related reports in the report set.
     *
     * @param artifact          the artifact to process
     * @param model             the POM associated with the artifact to process
     * @param reportingDatabase the report database to store results in
     */
    void processArtifact( Artifact artifact, Model model, ReportingDatabase reportingDatabase );

    /**
     * Run any metadata related reports in the report set.
     *
     * @param repositoryMetadata the metadata to process
     * @param repository         the repository the metadata is located in
     * @param reportingDatabase  the report database to store results in
     */
    void processMetadata( RepositoryMetadata repositoryMetadata, ArtifactRepository repository,
                          ReportingDatabase reportingDatabase );

    /**
     * Whether a report with the given role hint is included in this report set.
     *
     * @param key the report role hint.
     * @return whether the report is included
     */
    boolean includeReport( String key );

    /**
     * Get the report processors in this set. The map is keyed by the report's role hint, and the value is it's
     * display name.
     *
     * @return the reports
     */
    Map getReports();

    /**
     * Get the user-friendly name of this report.
     *
     * @return the report name
     */
    String getName();

    /**
     * Get the filename of the reports within the repository's reports directory.
     *
     * @return the filename
     */
    String getFilename();
}
