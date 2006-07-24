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

import java.util.List;

/**
 * Interface for discovering metadata files.
 */
public interface MetadataDiscoverer
    extends Discoverer
{
    String ROLE = MetadataDiscoverer.class.getName();

    /**
     * Search for metadata files in the repository.
     *
     * @param repository          The repository.
     * @param operation           the operation being performed (used for timestamp comparison)
     * @param blacklistedPatterns Patterns that are to be excluded from the discovery process.
     * @return the list of artifacts found
     */
    List discoverMetadata( ArtifactRepository repository, String operation, String blacklistedPatterns );
}
