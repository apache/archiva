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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.Iterator;

/**
 * This interface is used by the single artifact processor.
 * <p/>
 * The initial implementation of this will just need to be a mock implementation in src/test/java, used to track the
 * failures and successes for checking assertions. Later, implementations will be made to present reports on the
 * web interface, send them via mail, and so on.
 *
 * @todo i18n
 */
public interface ArtifactReporter
{
    String ROLE = ArtifactReporter.class.getName();

    String NULL_MODEL = "Provided model was null";

    String NULL_ARTIFACT = "Provided artifact was null";

    String EMPTY_GROUP_ID = "Group id was empty or null";

    String EMPTY_ARTIFACT_ID = "Artifact id was empty or null";

    String EMPTY_VERSION = "Version was empty or null";

    String EMPTY_DEPENDENCY_GROUP_ID = "Group id was empty or null";

    String EMPTY_DEPENDENCY_ARTIFACT_ID = "Artifact id was empty or null";

    String EMPTY_DEPENDENCY_VERSION = "Version was empty or null";

    String NO_DEPENDENCIES = "Artifact has no dependencies";

    String ARTIFACT_NOT_FOUND = "Artifact does not exist in the repository";

    String DEPENDENCY_NOT_FOUND = "Artifact's dependency does not exist in the repository";

    void addFailure( Artifact artifact, String reason );

    void addSuccess( Artifact artifact );

    void addWarning( Artifact artifact, String message );

    void addFailure( RepositoryMetadata metadata, String reason );

    void addSuccess( RepositoryMetadata metadata );

    void addWarning( RepositoryMetadata metadata, String message );

    Iterator getArtifactFailureIterator();

    Iterator getArtifactSuccessIterator();

    Iterator getArtifactWarningIterator();

    Iterator getRepositoryMetadataFailureIterator();

    Iterator getRepositoryMetadataSuccessIterator();

    Iterator getRepositoryMetadataWarningIterator();

    int getFailures();

    int getSuccesses();

    int getWarnings();
}
