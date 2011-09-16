package org.apache.archiva.repository.scanner;
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
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.FileType;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service
public class ArchivaAdministrationStub
    implements ArchivaAdministration
{

    private ArchivaConfiguration archivaConfiguration;

    public ArchivaAdministrationStub()
    {
        // no op
    }


    public ArchivaAdministrationStub( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws RepositoryAdminException
    {
        return null;
    }

    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void deleteLegacyArtifactPath( String path, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void addFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void removeFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public List<FileType> getFileTypes()
        throws RepositoryAdminException
    {
        return null;
    }

    public FileType getFileType( String fileTypeId )
        throws RepositoryAdminException
    {
        return null;
    }

    public void addFileType( FileType fileType, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void removeFileType( String fileTypeId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void addKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void setKnownContentConsumers( List<String> knownContentConsumers, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public List<String> getKnownContentConsumers()
        throws RepositoryAdminException
    {
        return archivaConfiguration.getConfiguration().getRepositoryScanning().getKnownContentConsumers();
    }

    public void removeKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void addInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public void setInvalidContentConsumers( List<String> invalidContentConsumers, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public List<String> getInvalidContentConsumers()
        throws RepositoryAdminException
    {
        return archivaConfiguration.getConfiguration().getRepositoryScanning().getInvalidContentConsumers();
    }

    public void removeInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

    }

    public OrganisationInformation getOrganisationInformation()
        throws RepositoryAdminException
    {
        return null;
    }

    public void setOrganisationInformation( OrganisationInformation organisationInformation )
        throws RepositoryAdminException
    {

    }
}
