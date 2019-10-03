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

import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.event.EventHandler;

import java.io.IOException;
import java.util.Set;

/**
 *
 * This interface must be implemented by the repository implementations. The repository provider knows all
 * about the repositories and should be the only part that uses the repository specific classes and libraries
 * (e.g. the maven libraries).
 *
 * Newly created instances should always be filled with default values that make sense. null values should
 * be avoided.
 *
 * References like staging repositories must not be set.
 *
 *
 */
public interface RepositoryProvider extends EventHandler
{

    /**
     * Returns the types of repositories this provider can handle.
     *
     * @return the set of supported repository types
     */
    Set<RepositoryType> provides();

    /**
     * Creates a editable managed repository instance. The provider must not check the uniqueness of the
     * id parameter and must not track the already created instances. Each call to this method will create
     * a new instance.
     *
     * @param id the repository identifier
     * @param name the repository name
     * @return a new created managed repository instance
     */
    EditableManagedRepository createManagedInstance(String id, String name) throws IOException;

    /**
     * Creates a editable remote repository instance. The provider must not check the uniqueness of the
     * id parameter and must not track the already created instances. Each call to this method will create
     * a new instance.
     *
     * @param id the repository identifier
     * @param name the repository name
     * @return a new created remote repository instance
     */
    EditableRemoteRepository createRemoteInstance(String id, String name);

    /**
     * Creates a editable repository group. . The provider must not check the uniqueness of the
     * id parameter and must not track the already created instances. Each call to this method will create
     * a new instance.
     *
     * @param id the repository identifier
     * @param name the repository name
     * @return A new instance of the repository group implementation
     */
    EditableRepositoryGroup createRepositoryGroup(String id, String name);

    /**
     * Creates a new managed repository instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param configuration the repository configuration that contains the repository data
     * @return a new created managed repository instance
     * @throws RepositoryException if some of the configuration values are not valid
     */
    ManagedRepository createManagedInstance( ManagedRepositoryConfiguration configuration) throws RepositoryException;

    /**
     * Updates the given managed repository instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param repo the repository instance that should be updated
     * @param configuration the repository configuration that contains the repository data
     * @throws RepositoryException if some of the configuration values are not valid
     */
    void updateManagedInstance( EditableManagedRepository repo, ManagedRepositoryConfiguration configuration) throws RepositoryException;

    /**
     * Creates a new managed staging repository instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param baseConfiguration the repository configuration of the base repository that references the staging repository
     * @return a new created managed staging repository instance
     * @throws RepositoryException if some of the configuration values are not valid
     */
    ManagedRepository createStagingInstance(ManagedRepositoryConfiguration baseConfiguration) throws RepositoryException;

    /**
     * Creates a new remote repository instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param configuration the repository configuration that contains the repository data
     * @return a new created remote repository instance
     * @throws RepositoryException if some of the configuration values are not valid
     */
    RemoteRepository createRemoteInstance( RemoteRepositoryConfiguration configuration) throws RepositoryException;

    /**
     * Updates the given remote repository instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param repo the repository instance that should be updated
     * @param configuration the repository configuration that contains the repository data
     * @throws RepositoryException if some of the configuration values are not valid
     */
    void updateRemoteInstance(EditableRemoteRepository repo, RemoteRepositoryConfiguration configuration) throws RepositoryException;


    /**
     * Creates a new repository group instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param configuration the repository group configuration
     * @return a new created repository group instance
     * @throws RepositoryException if some of the configuration values are not valid
     */
    RepositoryGroup createRepositoryGroup(RepositoryGroupConfiguration configuration) throws RepositoryException;

    /**
     * Updates the given remote repository instance from the given configuration. All attributes are filled from the
     * provided configuration object.
     *
     * @param repositoryGroup the repository group instance that should be updated
     * @param configuration the repository group configuration that contains the group data
     * @throws RepositoryException if some of the configuration values are not valid
     */
    void updateRepositoryGroupInstance(EditableRepositoryGroup repositoryGroup, RepositoryGroupConfiguration configuration) throws RepositoryException;

    /**
     * Returns a configuration object from the given remote repository instance.
     *
     * @param remoteRepository the remote repository instance
     * @return the repository configuration with all the data that is stored in the repository instance
     * @throws RepositoryException if the data cannot be converted
     */
    RemoteRepositoryConfiguration getRemoteConfiguration(RemoteRepository remoteRepository) throws RepositoryException;

    /**
     * Returns a configuration object from the given managed repository instance.
     *
     * @param managedRepository the managed repository instance
     * @return the repository configuration with all the data that is stored in the repository instance
     * @throws RepositoryException if the data cannot be converted
     */
    ManagedRepositoryConfiguration getManagedConfiguration(ManagedRepository managedRepository) throws RepositoryException;

    /**
     * Returns a configuration object from the given repository group instance.
     *
     * @param repositoryGroup the repository group
     * @return the repository group configuration with all the data that is stored in the repository instance
     * @throws RepositoryException if the data cannot be converted
     */
    RepositoryGroupConfiguration getRepositoryGroupConfiguration(RepositoryGroup repositoryGroup) throws RepositoryException;
}
