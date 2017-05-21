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
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RepositoryGroup;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public interface RepositoryGroupAdmin
{
    List<RepositoryGroup> getRepositoriesGroups()
        throws RepositoryAdminException;

    RepositoryGroup getRepositoryGroup( String repositoryGroupId )
        throws RepositoryAdminException;

    Boolean addRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation )
        throws RepositoryAdminException;

    Boolean updateRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation )
        throws RepositoryAdminException;

    Boolean deleteRepositoryGroup( String repositoryGroupId, AuditInformation auditInformation )
        throws RepositoryAdminException;

    Boolean addRepositoryToGroup( String repositoryGroupId, String repositoryId, AuditInformation auditInformation )
        throws RepositoryAdminException;

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

    File getMergedIndexDirectory( String repositoryGroupId );
}
