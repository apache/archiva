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

import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.model.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.model.RepositoryGroupConfiguration;
import org.apache.archiva.event.EventSource;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *  Registry for repositories. This is the central entry point for repositories. It provides methods for
 *  retrieving, adding and removing repositories.
 *  <p>
 *  The modification methods putXX and removeXX without configuration object persist the changes immediately to the archiva configuration. If the
 *  configuration save fails the changes are rolled back.
 *  </p>
 *  <p>
 *    The modification methods with configuration object do only update the given configuration. The configuration is not saved.
 *  </p>
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@SuppressWarnings( "UnusedReturnValue" )
public interface RepositoryRegistry extends EventSource, RepositoryHandlerManager
{
    /**
     * Set the configuration for the registry
     * @param archivaConfiguration the archiva configuration instance
     */
    void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration );

    /**
     * Return the index manager for the given repository type
     * @param type the repository type
     * @return the index manager, if it exists
     */
    ArchivaIndexManager getIndexManager( RepositoryType type );

    /**
     * Returns the metadata reader for the given repository type
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

    /**
     * Returns a collection of all registered remote repositories
     * @return the collection of remote repositories
     */
    Collection<RemoteRepository> getRemoteRepositories( );

    /**
     * Returns a collection of all registered repository groups.
     *
     * @return the collection of repository groups
     */
    Collection<RepositoryGroup> getRepositoryGroups( );

    /**
     * Returns the repository (managed, remote, group) with the given id
     * @param repoId the id of the repository
     * @return the repository or <code>null</code> if no repository with this ID is registered.
     */
    Repository getRepository( String repoId );

    /**
     * Returns the managed repository with the given id
     * @param repoId the id of the repository
     * @return the managed repository instance or <code>null</code>, if no managed repository with this ID is registered.
     */
    ManagedRepository getManagedRepository( String repoId );

    /**
     * Returns the remote repository with the given id
     * @param repoId the id of the repository
     * @return the remote repository instance or <code>null</code>, if no remote repository with this ID is registered.
     */
    RemoteRepository getRemoteRepository( String repoId );

    /**
     * Returns the repository group with the given id
     * @param groupId the id of the repository group
     * @return the repository group instance or <code>null</code>, if no repository group with this ID is registered.
     */
    RepositoryGroup getRepositoryGroup( String groupId );

    /**
     * Returns <code>true</code>, if a repository with the given ID is registered, otherwise <code>false</code>
     * @param repoId the ID of the repository
     * @return <code>true</code>, if a repository with the given ID is registered, otherwise <code>false</code>
     */
    boolean hasRepository(String repoId);

    /**
     * Returns <code>true</code>, if a managed repository with the given ID is registered, otherwise <code>false</code>
     * @param repoId the id of the managed repository
     * @return <code>true</code>, if a managed repository with the given ID is registered, otherwise <code>false</code>
     */
    boolean hasManagedRepository(String repoId);

    /**
     * Returns <code>true</code>, if a remote repository with the given ID is registered, otherwise <code>false</code>
     * @param repoId the id of the remote repository
     * @return <code>true</code>, if a remote repository with the given ID is registered, otherwise <code>false</code>
     */
    boolean hasRemoteRepository(String repoId);

    /**
     * Returns <code>true</code>, if a repository group with the given ID is registered, otherwise <code>false</code>
     * @param groupId the id of the repository group
     * @return <code>true</code>, if a repository group with the given ID is registered, otherwise <code>false</code>
     */
    boolean hasRepositoryGroup( String groupId );

    /**
     * Adds or updates the given managed repository. If a managed repository with the given id exists already, it is updated
     * from the data of the given instance. Otherwise a new repository is created and updated by the data of the given instance.
     *
     * The archiva configuration is updated and saved after updating the registered repository instance.
     *
     * @param managedRepository the managed repository
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    ManagedRepository putRepository( ManagedRepository managedRepository ) throws RepositoryException;

    /**
     * Adds or updates the given managed repository. If a managed repository with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * The archiva configuration is updated and saved after updating the registered repository instance.
     *
     * @param managedRepositoryConfiguration the managed repository configuration
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration ) throws RepositoryException;

    /**
     * Adds or updates the given managed repository. If a managed repository with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * This method can be used, if the archiva configuration should not be saved. It will only update the given configuration object.
     *
     * @param managedRepositoryConfiguration the managed repository configuration
     * @param configuration the archiva configuration that is updated
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration ) throws RepositoryException;

    /**
     * Validates the given repository configuration and adds the repository persistent to the registry, if it is valid.
     * If the validation was not successful, the repository will not be added or persistet, and it will return the list of validation errors.
     *
     * @param configuration the managed repository configuration
     * @return the managed repository or a list of validation errors
     * @throws RepositoryException if there are errors while adding the repository
     */
    CheckedResult<ManagedRepository, Map<String, List<ValidationError>>> putRepositoryAndValidate( ManagedRepositoryConfiguration configuration) throws RepositoryException;

    /**
     * Adds or updates the given repository group. If a repository group with the given id exists already, it is updated
     * from the data of the given instance. Otherwise a new repository is created and updated by the data of the given instance.
     *
     * The archiva configuration is updated and saved after updating the registered repository instance.
     *
     * @param repositoryGroup the repository group
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    RepositoryGroup putRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException;

    /**
     * Adds or updates the given repository group. If a repository group with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * The archiva configuration is updated and saved after updating the registered repository instance.
     *
     * @param repositoryGroupConfiguration the repository group configuration
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration ) throws RepositoryException;


    /**
     * This method creates or updates a repository by the given configuration. It uses the <code>validator</code> to check the
     * result. If the validation is not successful, the repository will not be saved.
     *
     * @param configuration the repository configuration
     * @return the result
     */
    CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> putRepositoryGroupAndValidate( RepositoryGroupConfiguration configuration) throws RepositoryException;

    /**
     * Adds or updates the given repository group. If a repository group with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * This method can be used, if the archiva configuration should not be saved. It will only update the given configuration object.
     *
     * @param repositoryGroupConfiguration the repository group configuration
     * @param configuration the archiva configuration that is updated
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration ) throws RepositoryException;

    /**
     * Adds or updates the given remote repository. If a remote repository with the given id exists already, it is updated
     * from the data of the given instance. Otherwise a new repository is created and updated by the data of the given instance.
     *
     * The archiva configuration is updated and saved after updating the registered repository instance.
     *
     * @param remoteRepository the remote repository
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    RemoteRepository putRepository( RemoteRepository remoteRepository ) throws RepositoryException;

    /**
     * Adds or updates the given remote repository. If a remote repository with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * The archiva configuration is updated and saved after updating the registered repository instance.
     *
     * @param remoteRepositoryConfiguration the remote repository configuration
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration ) throws RepositoryException;

    /**
     * Adds or updates the given remote repository. If a remote repository with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * The remoteRepositoryConfiguration is validated before adding to the registry and persisting. If the validation fails,
     * it is not registered or updated.
     *
     * @param remoteRepositoryConfiguration the remote repository configuration
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    CheckedResult<RemoteRepository, Map<String, List<ValidationError>>>  putRepositoryAndValidate( RemoteRepositoryConfiguration remoteRepositoryConfiguration ) throws RepositoryException;

    /**
     * Adds or updates the given remote repository. If a remote repository with the given id exists already, it is updated
     * from the data of the given configuration. Otherwise a new repository is created and updated by the data of the given configuration.
     *
     * This method can be used, if the archiva configuration should not be saved. It will only update the given configuration object.
     *
     * @param remoteRepositoryConfiguration the remote repository configuration
     * @param configuration the archiva configuration where the updated data is stored into
     * @return the repository instance, that was created or updated
     * @throws RepositoryException if an error occurred while creating or updating the instance
     */
    RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration ) throws RepositoryException;

    /**
     * Removes the repository or repository group with the given id, if it exists. Otherwise, it will do nothing.
     *
     * The configuration is updated and saved, if the deletion was successful
     *
     * @param repoId the id of the repository or repository group to delete
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepository( String repoId ) throws RepositoryException;

    /**
     * Removes the given repository.
     *
     * The configuration is updated and saved, if the deletion was successful
     *
     * @param repo the repository instance that should be deleted
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepository( Repository repo ) throws RepositoryException;

    /**
     * Removes the given managed repository.
     *
     * The configuration is updated and saved, if the deletion was successful
     *
     * @param managedRepository the managed repository to remove
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepository( ManagedRepository managedRepository ) throws RepositoryException;

    /**
     * Removes the given managed repository. The given configuration instance is updated, but the
     * archiva configuration is not saved.
     *
     * @param managedRepository the managed repository to remove
     * @param configuration the configuration instance to update
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepository( ManagedRepository managedRepository, Configuration configuration ) throws RepositoryException;

    /**
     * Removes the given repository group.
     *
     * The configuration is updated and saved, if the deletion was successful
     *
     * @param repositoryGroup the repository group to remove
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException;

    /**
     * Removes the given repository group. The given configuration instance is updated, but the
     * archiva configuration is not saved.
     *
     * @param repositoryGroup the repository group to remove
     * @param configuration the configuration instance to update
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepositoryGroup( RepositoryGroup repositoryGroup, Configuration configuration ) throws RepositoryException;

    /**
     * Removes the given remote repository.
     *
     * The configuration is updated and saved, if the deletion was successful
     *
     * @param remoteRepository the remote repository to remove
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepository( RemoteRepository remoteRepository ) throws RepositoryException;

    /**
     * Removes the given remote repository. The given configuration instance is updated, but the
     * archiva configuration is not saved.
     *
     * @param remoteRepository the remote repository to remove
     * @param configuration the configuration instance to update
     * @throws RepositoryException if the repository deletion failed
     */
    void removeRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException;

    /**
     * Reloads all repositories and groups from the configuration
     */
    void reload( );

    void resetIndexingContext( Repository repository ) throws IndexUpdateFailedException;

    /**
     * Creates a new repository based on the given repository and with the given new id.
     * @param repo the repository to copy from
     * @param newId the new repository id
     * @param <T> the type of the repository (Manage, Remote or RepositoryGroup)
     * @return the newly created repository
     * @throws RepositoryException if the repository could not be created
     */
    <T extends Repository> T clone( T repo, String newId ) throws RepositoryException;

    /**
     * Return the repository that stores the given asset.
     * @param asset the asset
     * @return the repository or <code>null</code> if no matching repository is found
     */
    Repository getRepositoryOfAsset( StorageAsset asset );

    /**
     * Validates the set attributes of the given repository instance and returns the validation result.
     * The repository registry uses all available validators and applies their validateRepository method to the given
     * repository. Validation results will be merged per field.
     *
     * @param repository the repository to validate against
     * @return the result of the validation.
     */
    <R extends Repository> ValidationResponse<R> validateRepository( R repository);

    /**
     * Validates the set attributes of the given repository instance for a repository update and returns the validation result.
     * The repository registry uses all available validators and applies their validateRepositoryForUpdate method to the given
     * repository. Validation results will be merged per field.
     *
     * @param repository the repository to validate against
     * @return the result of the validation.
     */
    <R extends Repository> ValidationResponse<R> validateRepositoryForUpdate( R repository);

}
