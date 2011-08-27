package org.apache.maven.archiva.repository;

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

import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.RepositoryURL;
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * RemoteRepositoryContent interface for interacting with a remote repository in an abstract way, 
 * without the need for processing based on URLs, or working with the database. 
 *
 * @version $Id$
 */
public interface RemoteRepositoryContent
{
    /**
     * <p>
     * Convenience method to get the repository id.
     * </p>
     * 
     * <p>
     * Equivalent to calling <code>.getRepository().getId()</code>
     * </p>
     * 
     * @return the repository id.
     */
    String getId();

    /**
     * Get the repository configuration associated with this
     * repository content.
     * 
     * @return the repository that is associated with this repository content.
     */
    RemoteRepositoryConfiguration getRepository();

    /**
     * <p>
     * Convenience method to get the repository url.
     * </p>
     * 
     * <p>
     * Equivalent to calling <code>new RepositoryURL( this.getRepository().getUrl() )</code>
     * </p>
     * 
     * @return the repository url.
     */
    RepositoryURL getURL();

    /**
     * Set the repository configuration to associate with this
     * repository content.
     * 
     * @param repo the repository to associate with this repository content.
     */
    void setRepository( RemoteRepositoryConfiguration repo );

    /**
     * Given a repository relative path to a filename, return the {@link VersionedReference} object suitable for the path.
     *
     * @param path the path relative to the repository base dir for the artifact.
     * @return the {@link ArtifactReference} representing the path.  (or null if path cannot be converted to
     *         a {@link ArtifactReference})
     * @throws LayoutException if there was a problem converting the path to an artifact.
     */
    ArtifactReference toArtifactReference( String path )
        throws LayoutException;

    /**
     * Given an ArtifactReference, return the relative path to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    String toPath( ArtifactReference reference );

    /**
     * Given an ArtifactReference, return the url to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    RepositoryURL toURL( ArtifactReference reference );
}
