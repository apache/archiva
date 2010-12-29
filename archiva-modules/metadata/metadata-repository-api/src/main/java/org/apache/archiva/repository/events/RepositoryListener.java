package org.apache.archiva.repository.events;

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

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataException;

/**
 * Listen to events on the repository. This class is a stopgap
 * refactoring measure until an event bus is in place to handle
 * generic events such as these.
 *
 * This assumes that the events occur before the action has completed, though they don't currently offer any mechanism
 * to prevent an event from occurring or guarantee that it will happen.
 *
 * FIXME: this needs to be made more permanent since 3rd party plugins will depend on it heavily
 */
public interface RepositoryListener
{
    void deleteArtifact( MetadataRepository metadataRepository, String repositoryId, String namespace, String project,
                         String version, String id );

    void addArtifact( RepositorySession session, String repoId, String namespace, String projectId,
                      ProjectVersionMetadata metadata );

    // FIXME: this would be better as a "processException" method, with the event information captured in a single class
    void addArtifactProblem( RepositorySession session, String repoId, String namespace, String projectId,
                             String projectVersion, RepositoryStorageMetadataException exception );
}
