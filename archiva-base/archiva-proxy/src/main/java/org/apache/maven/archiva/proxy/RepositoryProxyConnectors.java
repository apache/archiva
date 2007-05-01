package org.apache.maven.archiva.proxy;

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

import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;

import java.io.File;
import java.util.List;

/**
 * Handler for potential repository proxy connectors.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface RepositoryProxyConnectors
{
    /**
     * Performs the artifact fetch operation against the target repositories
     * of the provided source repository.
     * 
     * If the artifact is found, it is downloaded and placed into the source repository
     * filesystem.
     * 
     * @param repository the source repository to use. (must be a managed repository)
     * @param artifact the artifact to fetch.
     * @return true if the fetch operation succeeded in obtaining content, false if no content was obtained.
     * @throws ProxyException if there was a problem fetching the content from the target repositories.
     */
    public File fetchFromProxies( ArchivaRepository repository, ArtifactReference artifact )
        throws ProxyException;
    
    /**
     * Performs the metadata fetch operation against the target repositories
     * of the provided source repository.
     * 
     * If the metadata is found, it is downloaded and placed into the source repository
     * filesystem.
     * 
     * @param repository the source repository to use. (must be a managed repository)
     * @param metadata the metadata to fetch.
     * @return true if the fetch operation succeeded in obtaining content, false if no content was obtained.
     * @throws ProxyException if there was a problem fetching the content from the target repositories.
     */
    public File fetchFromProxies( ArchivaRepository repository, VersionedReference metadata )
        throws ProxyException;
    
    /**
     * Performs the metadata fetch operation against the target repositories
     * of the provided source repository.
     * 
     * If the metadata is found, it is downloaded and placed into the source repository
     * filesystem.
     * 
     * @param repository the source repository to use. (must be a managed repository)
     * @param metadata the metadata to fetch.
     * @return true if the fetch operation succeeded in obtaining content, false if no content was obtained.
     * @throws ProxyException if there was a problem fetching the content from the target repositories.
     */
    public File fetchFromProxies( ArchivaRepository repository, ProjectReference metadata )
        throws ProxyException;

    /**
     * Get the List of {@link ProxyConnector} objects of the source repository.
     * 
     * @param repository the source repository to look for.
     * @return the List of {@link ProxyConnector} objects.
     */
    public List getProxyConnectors( ArchivaRepository repository );

    /**
     * Tests to see if the provided repository is a source repository for
     * any {@link ProxyConnector} objects.
     * 
     * @param repository the source repository to look for.
     * @return true if there are proxy connectors that use the provided 
     *   repository as a source repository.
     */
    public boolean hasProxies( ArchivaRepository repository );
}
