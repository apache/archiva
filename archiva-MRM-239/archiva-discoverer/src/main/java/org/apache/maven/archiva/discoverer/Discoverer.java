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

import java.io.File;
import java.util.List;

/**
 * Discoverer - generic discoverer of content in an ArtifactRepository. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Discoverer
{
    public static final String ROLE = Discoverer.class.getName();
    
    /**
     * Walk the repository, and report to the consumers the files found.
     * 
     * Report changes to the appropriate Consumer.
     * 
     * This is just a convenience method to {@link #walkRepository(ArtifactRepository, List, boolean, long, List, List)}
     * equivalent to calling <code>walkRepository( repository, consumers, includeSnapshots, 0, null, null );</code>
     * 
     * @param repository the repository to change.
     * @param consumers use the provided list of consumers.
     * @param includeSnapshots true to include snapshots in the walking of this repository.
     * @return the statistics for this scan.
     * @throws DiscovererException if there was a fundamental problem with getting the discoverer started.
     */
    public DiscovererStatistics walkRepository( ArtifactRepository repository, List consumers, boolean includeSnapshots )
        throws DiscovererException;

    /**
     * Walk the repository, and report to the consumers the files found.
     * 
     * Report changes to the appropriate Consumer.
     * 
     * @param repository the repository to change.
     * @param consumers use the provided list of consumers.
     * @param includeSnapshots true to include snapshots in the scanning of this repository.
     * @param onlyModifiedAfterTimestamp Only report to the consumers, files that have a {@link File#lastModified()}) 
     *          after the provided timestamp.
     * @param extraFileExclusions an optional list of file exclusions on the walk.
     * @param extraFileInclusions an optional list of file inclusions on the walk.
     * @return the statistics for this scan.
     * @throws DiscovererException if there was a fundamental problem with getting the discoverer started. 
     */
    public DiscovererStatistics walkRepository( ArtifactRepository repository, List consumers,
                                                boolean includeSnapshots, long onlyModifiedAfterTimestamp,
                                                List extraFileExclusions, List extraFileInclusions )
        throws DiscovererException;
}
