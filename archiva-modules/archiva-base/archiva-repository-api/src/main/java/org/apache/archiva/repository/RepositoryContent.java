package org.apache.archiva.repository;

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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.content.ItemSelector;


/**
 * Common aspects of content provider interfaces
 */
public interface RepositoryContent
{


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
     * Given an {@link ArtifactReference}, return the relative path to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    String toPath( ArtifactReference reference );


    /**
     * Return the path, that represents the item specified by the selector.
     * @param selector the selector with the artifact coordinates
     * @return the path to the content item
     */
    String toPath( ItemSelector selector );

    /**
     * Return a item selector that matches the given path. This is kind of reverse method for the {@link #toPath(ItemSelector)}
     * method and fills the selector with the known information. It may not make sense for every path, and the following
     * must <b>not always be true</b>:
     * <pre>
     *  selector.equals(r.toItemSelector(r.toPath(selector)))
     * </pre>
     *
     * The methods on the ManagedRepository give more reliable results.
     *
     * @param path the repository path
     * @return a item selector that would select the given path
     */
    ItemSelector toItemSelector(String path) throws LayoutException;


}
