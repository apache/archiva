package org.apache.maven.archiva.repository.events;

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;

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

/**
 * Listen to events on the repository. This class is a stopgap 
 * refactoring measure until an event bus is in place to handle 
 * generic events such as these.
 */
public interface RepositoryListener 
{
    /**
     * Event for the deletion of a given artifact.
     * @param artifactPath the path to the artifact that was deleted.
     */
    void deleteArtifact( ManagedRepositoryContent repository, ArchivaArtifact artifact );
}
