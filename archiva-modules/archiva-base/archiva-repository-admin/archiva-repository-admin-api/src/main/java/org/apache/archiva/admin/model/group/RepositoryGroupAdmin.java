package org.apache.archiva.admin.model.group;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.EntityExistsException;
import org.apache.archiva.admin.model.EntityNotFoundException;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.repository.storage.StorageAsset;

import java.util.List;
import java.util.Map;

/**
 * Methods for administering repository groups (virtual repositories)
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public interface RepositoryGroupAdmin
{
    List<RepositoryGroup> getRepositoriesGroups()
        throws RepositoryAdminException;

    /**
     * Returns the repository group. If it is not found a {@link org.apache.archiva.admin.model.EntityNotFoundException}
     * will be thrown.
     *
     * @param repositoryGroupId the identifier of the repository group
     * @return the repository group object
     * @throws RepositoryAdminException
     * @throws EntityNotFoundException
     */
    RepositoryGroup getRepositoryGroup( String repositoryGroupId )
        throws RepositoryAdminException, EntityNotFoundException;

    Boolean addRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation )
        throws RepositoryAdminException;

    Boolean updateRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation )
        throws RepositoryAdminException;

    Boolean deleteRepositoryGroup( String repositoryGroupId, AuditInformation auditInformation )
        throws RepositoryAdminException;

    /**
     * Adds the given managed repository to the repository group.
     *
     * @param repositoryGroupId the id of the repository group
     * @param repositoryId the id of the managed repository
     * @param auditInformation audit information
     * @return <code>true</code>, if the repository was added, otherwise <code>false</code>
     * @throws RepositoryAdminException If an error occurred , while adding the group.
     * @throws EntityNotFoundException If the repository group or the managed repository with the given id does not exist
     * @throws EntityExistsException If the managed repository is already member of the group
     */
    Boolean addRepositoryToGroup( String repositoryGroupId, String repositoryId, AuditInformation auditInformation )
        throws RepositoryAdminException, EntityNotFoundException, EntityExistsException;

    Boolean deleteRepositoryFromGroup( String repositoryGroupId, String repositoryId,
                                       AuditInformation auditInformation )
        throws RepositoryAdminException;

    /**
     * @return Map with key repoGroupId and value repoGroup
     * @throws RepositoryAdminException
     */
    Map<String, RepositoryGroup> getRepositoryGroupsAsMap()
        throws RepositoryAdminException;

    /**
     * @return Map with key repoGroupId and value List of ManagedRepositories
     * @throws RepositoryAdminException
     */
    Map<String, List<String>> getGroupToRepositoryMap()
        throws RepositoryAdminException;

    /**
     * @return Map with key managedRepo id and value List of repositoryGroup ids where the repo is
     * @throws RepositoryAdminException
     */
    Map<String, List<String>> getRepositoryToGroupMap()
        throws RepositoryAdminException;

    StorageAsset getMergedIndexDirectory(String repositoryGroupId );
}
