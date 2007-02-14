package org.apache.maven.archiva.discoverer;

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

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * Discoverer - generic discoverer of content in an ArtifactRepository. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Discoverer
{
    /**
     * Scan the repository for changes.
     * Report changes to the appropriate Consumer.
     * 
     * @param repository the repository to change.
     * @param consumers use the provided list of consumers.
     * @param includeSnapshots true to include snapshots in the scanning of this repository.
     * @return the statistics for this scan.
     */
    public DiscovererStatistics scanRepository( ArtifactRepository repository, List consumers, boolean includeSnapshots );
    
    /**
     * Walk the entire repository, regardless of change.
     * Report changes to the appropriate Consumer.
     * 
     * @param repository the repository to change.
     * @param consumers use the provided list of consumers.
     * @param includeSnapshots true to include snapshots in the walking of this repository.
     * @return the statistics for this scan.
     */
    public DiscovererStatistics walkRepository( ArtifactRepository repository, List consumers, boolean includeSnapshots );
}
