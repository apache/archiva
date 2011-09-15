package org.apache.archiva.admin.model.managed;
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
import org.apache.archiva.admin.model.beans.ManagedRepository;

import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public interface ManagedRepositoryAdmin
{
    List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException;

    Map<String, ManagedRepository> getManagedRepositoriesAsMap()
        throws RepositoryAdminException;

    ManagedRepository getManagedRepository( String repositoryId )
        throws RepositoryAdminException;

    Boolean deleteManagedRepository( String repositoryId, AuditInformation auditInformation, boolean deleteContent )
        throws RepositoryAdminException;

    Boolean addManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                  AuditInformation auditInformation )
        throws RepositoryAdminException;


    Boolean updateManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                     AuditInformation auditInformation, boolean resetStats )
        throws RepositoryAdminException;


}
