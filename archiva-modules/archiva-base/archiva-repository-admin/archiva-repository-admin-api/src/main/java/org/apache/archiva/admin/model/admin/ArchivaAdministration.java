package org.apache.archiva.admin.model.admin;
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
import org.apache.archiva.admin.model.beans.FileType;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.admin.model.beans.NetworkConfiguration;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.admin.model.beans.UiConfiguration;

import java.util.List;

/**
 * Base administration interface for Archiva. Provides methods for managing archiva base tasks.
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public interface ArchivaAdministration
{

    List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws RepositoryAdminException;

    void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void deleteLegacyArtifactPath( String path, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void addFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void removeFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException;

    List<FileType> getFileTypes()
        throws RepositoryAdminException;

    FileType getFileType( String fileTypeId )
        throws RepositoryAdminException;

    void addFileType( FileType fileType, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void removeFileType( String fileTypeId, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void addKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void setKnownContentConsumers( List<String> knownContentConsumers, AuditInformation auditInformation )
        throws RepositoryAdminException;

    List<String> getKnownContentConsumers()
        throws RepositoryAdminException;

    void removeKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void addInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException;

    void setInvalidContentConsumers( List<String> invalidContentConsumers, AuditInformation auditInformation )
        throws RepositoryAdminException;

    List<String> getInvalidContentConsumers()
        throws RepositoryAdminException;

    void removeInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException;

    OrganisationInformation getOrganisationInformation()
        throws RepositoryAdminException;

    void setOrganisationInformation( OrganisationInformation organisationInformation )
        throws RepositoryAdminException;

    UiConfiguration getUiConfiguration()
        throws RepositoryAdminException;

    void updateUiConfiguration( UiConfiguration uiConfiguration )
        throws RepositoryAdminException;

    NetworkConfiguration getNetworkConfiguration()
        throws RepositoryAdminException;

    void setNetworkConfiguration( NetworkConfiguration networkConfiguration )
        throws RepositoryAdminException;

}
