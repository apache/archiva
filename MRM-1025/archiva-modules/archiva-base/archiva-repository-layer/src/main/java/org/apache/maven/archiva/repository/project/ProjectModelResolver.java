package org.apache.maven.archiva.repository.project;

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

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;

/**
 * Interface for ProjectModel resolution. 
 *
 * @version $Id$
 */
public interface ProjectModelResolver
{
    /**
     * Get the ProjectModel given a specific {@link RepositoryContent} key.
     * 
     * @param reference the reference to the other project. 
     * @return the ArchivaProjectModel representing the provided {@link RepositoryContent} key.
     * @throws ProjectModelException if the project model cannot be resolved.
     */
    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
        throws ProjectModelException;
}
