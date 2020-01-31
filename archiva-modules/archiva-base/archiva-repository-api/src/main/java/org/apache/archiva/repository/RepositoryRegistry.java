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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.event.EventSource;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.storage.StorageAsset;

import java.util.Collection;

/**
 *  Registry for repositories. This is the central entry point for repositories. It provides methods for
 *  retrieving, adding and removing repositories.
 *  <p>
 *  The modification methods addXX and removeXX persist the changes immediately to the configuration. If the
 *  configuration save fails the changes are rolled back.
 *  <p>
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface RepositoryRegistry extends EventSource
{
    /**
     * Set the configuration for the registry
     * @param archivaConfiguration
     */
    void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration );

    /**
     * Return the index manager for the given repository type
     * @param type the repository type
     * @return the index manager, if it exists
     */
    ArchivaIndexManager getIndexManager( RepositoryType type );

    /**
     * Returns the metadatareader for the given repository type
     * @param type the repository type
     * @return the metadata reader instance
     */
    MetadataReader getMetadataReader(RepositoryType type) throws UnsupportedRepositoryTypeException;

    /**
     * Returns all registered repositories
     * @return the list of repositories
     */
    Collection<Repository> getRepositories( );

    /**
     * Returns all managed repositories
     * @return the list of managed repositories
     */
    Collection<ManagedRepository> getManagedRepositories( );

    Collection<RemoteRepository> getRemoteRepositories( );

    Collection<RepositoryGroup> getRepositoryGroups( );

    Repository getRepository( String repoId );

    ManagedRepository getManagedRepository( String repoId );

    RemoteRepository getRemoteRepository( String repoId );

    RepositoryGroup getRepositoryGroup( String groupId );

    ManagedRepository putRepository( ManagedRepository managedRepository ) throws RepositoryException;

    ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration ) throws RepositoryException;

    ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration ) throws RepositoryException;

    RepositoryGroup putRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException;

    RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration ) throws RepositoryException;

    RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration ) throws RepositoryException;

    RemoteRepository putRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException;

    RemoteRepository putRepository( RemoteRepository remoteRepository ) throws RepositoryException;

    RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration ) throws RepositoryException;

    RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration ) throws RepositoryException;

    void removeRepository( String repoId ) throws RepositoryException;

    void removeRepository( Repository repo ) throws RepositoryException;

    void removeRepository( ManagedRepository managedRepository ) throws RepositoryException;

    void removeRepository( ManagedRepository managedRepository, Configuration configuration ) throws RepositoryException;

    void removeRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException;

    void removeRepositoryGroup( RepositoryGroup repositoryGroup, Configuration configuration ) throws RepositoryException;

    void removeRepository( RemoteRepository remoteRepository ) throws RepositoryException;

    void removeRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException;

    void reload( );

    void resetIndexingContext( Repository repository ) throws IndexUpdateFailedException;

    ManagedRepository clone( ManagedRepository repo, String newId ) throws RepositoryException;

    <T extends Repository> Repository clone( T repo, String newId ) throws RepositoryException;

    RemoteRepository clone( RemoteRepository repo, String newId ) throws RepositoryException;

    /**
     * Return the repository that stores the given asset.
     * @param asset the asset
     * @return the repository or <code>null</code> if no matching repository is found
     */
    Repository getRepositoryOfAsset( StorageAsset asset );
}
