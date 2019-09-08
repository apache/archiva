package org.apache.archiva.metadata.repository.storage;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.filter.Filter;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.xml.XMLException;

import java.io.IOException;
import java.util.Collection;

// FIXME: we should drop the repositoryId parameters and attach this to an instance of a repository storage
public interface RepositoryStorage
{
    ProjectMetadata readProjectMetadata( String repoId, String namespace, String projectId );

    ProjectVersionMetadata readProjectVersionMetadata( ReadMetadataRequest readMetadataRequest )
        throws RepositoryStorageMetadataInvalidException, RepositoryStorageMetadataNotFoundException,
        RepositoryStorageRuntimeException;

    Collection<String> listRootNamespaces( String repoId, Filter<String> filter )
        throws RepositoryStorageRuntimeException;

    Collection<String> listNamespaces( String repoId, String namespace, Filter<String> filter )
        throws RepositoryStorageRuntimeException;

    Collection<String> listProjects( String repoId, String namespace, Filter<String> filter )
        throws RepositoryStorageRuntimeException;

    Collection<String> listProjectVersions( String repoId, String namespace, String projectId, Filter<String> filter )
        throws RepositoryStorageRuntimeException;

    Collection<ArtifactMetadata> readArtifactsMetadata( ReadMetadataRequest readMetadataRequest )
        throws RepositoryStorageRuntimeException;

    // FIXME: reconsider this API, do we want to expose storage format in the form of a path?
    ArtifactMetadata readArtifactMetadataFromPath( String repoId, String path )
        throws RepositoryStorageRuntimeException;
    
    /**
     * A relocation capable client will request the POM prior to the artifact, and will then read meta-data and do
     * client side relocation. A simplier client (like maven 1) will only request the artifact and not use the
     * metadatas.
     * <p>
     * For such clients, archiva does server-side relocation by reading itself the &lt;relocation&gt; element in
     * metadatas and serving the expected artifact.
     * @param managedRepository the used managed repository
     * @param artifact the artifact reference
     * @throws org.apache.archiva.policies.ProxyDownloadException
     */    
    void applyServerSideRelocation( ManagedRepository managedRepository, ArtifactReference artifact )
        throws ProxyDownloadException;

    /**
     * add an other method to evaluate real path as when receiving -SNAPSHOT (for maven storage)
     * request redirect to the last build
     * @param requestPath the web uri request
     * @param managedRepository the used managed repository can be <code>null</code> so last version won't be resolved
     * @return the file path
     * @since 2.0.0
     */
    String getFilePath( String requestPath, org.apache.archiva.repository.ManagedRepository managedRepository );

    String getFilePathWithVersion( final String requestPath, ManagedRepositoryContent managedRepositoryContent )
            throws RelocationException, XMLException, IOException;


}
