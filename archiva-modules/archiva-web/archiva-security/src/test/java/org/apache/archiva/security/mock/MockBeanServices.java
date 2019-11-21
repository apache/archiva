package org.apache.archiva.security.mock;
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
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.filter.Filter;
import org.apache.archiva.metadata.repository.storage.*;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.xml.XMLException;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Olivier Lamy
 */
public class MockBeanServices
    implements RepositoryStorage, RepositoryListener, RepositoryArchivaTaskScheduler
{
    @Override
    public ProjectMetadata readProjectMetadata( String repoId, String namespace, String projectId )
    {
        return null;
    }

    @Override
    public ProjectVersionMetadata readProjectVersionMetadata( ReadMetadataRequest readMetadataRequest )
        throws RepositoryStorageMetadataInvalidException, RepositoryStorageMetadataNotFoundException,
        RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public Collection<String> listRootNamespaces( String repoId, Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public Collection<String> listNamespaces( String repoId, String namespace, Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public Collection<String> listProjects( String repoId, String namespace, Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public Collection<String> listProjectVersions( String repoId, String namespace, String projectId,
                                                   Filter<String> filter )
        throws RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public Collection<ArtifactMetadata> readArtifactsMetadata( ReadMetadataRequest readMetadataRequest )
        throws RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public ArtifactMetadata readArtifactMetadataFromPath( String repoId, String path )
        throws RepositoryStorageRuntimeException
    {
        return null;
    }

    @Override
    public void applyServerSideRelocation( ManagedRepository managedRepository, ArtifactReference artifact )
        throws ProxyDownloadException
    {

    }


    @Override
    public void deleteArtifact( MetadataRepository metadataRepository, String repositoryId, String namespace,
                                String project, String version, String id )
    {

    }

    @Override
    public void addArtifact( RepositorySession session, String repoId, String namespace, String projectId,
                             ProjectVersionMetadata metadata )
    {

    }

    @Override
    public void addArtifactProblem( RepositorySession session, String repoId, String namespace, String projectId,
                                    String projectVersion, RepositoryStorageMetadataException exception )
    {

    }


    @Override
    public boolean isProcessingRepositoryTask( String repositoryId )
    {
        return false;
    }

    @Override
    public boolean isProcessingRepositoryTask( RepositoryTask task )
    {
        return false;
    }

    @Override
    public void queueTask( RepositoryTask task )
        throws TaskQueueException
    {

    }

    @Override
    public boolean unQueueTask( RepositoryTask task )
        throws TaskQueueException
    {
        return false;
    }

    @Override
    public String getFilePath( String requestPath, org.apache.archiva.repository.ManagedRepository managedRepository )
    {
        return null;
    }

    @Override
    public String getFilePathWithVersion( String requestPath, ManagedRepositoryContent managedRepositoryContent ) throws RelocationException, XMLException, IOException
    {
        return null;
    }
}
