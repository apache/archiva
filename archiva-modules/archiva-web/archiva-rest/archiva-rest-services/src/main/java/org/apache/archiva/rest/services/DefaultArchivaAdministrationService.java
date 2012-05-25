package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.FileType;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.admin.model.beans.NetworkConfiguration;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.admin.model.beans.UiConfiguration;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.rest.api.model.AdminRepositoryConsumer;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.services.utils.AddAdminRepoConsumerClosure;
import org.apache.archiva.rest.services.utils.AdminRepositoryConsumerComparator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "archivaAdministrationService#default" )
public class DefaultArchivaAdministrationService
    extends AbstractRestService
    implements ArchivaAdministrationService
{
    @Inject
    private ArchivaAdministration archivaAdministration;

    @Inject
    @Named( value = "managedRepositoryContent#legacy" )
    private ManagedRepositoryContent repositoryContent;

    @Inject
    private RepositoryContentConsumers repoConsumerUtil;

    public List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaAdministration.getLegacyArtifactPaths();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
        throws ArchivaRestServiceException
    {

        // Check the proposed Artifact matches the path
        ArtifactReference artifact = new ArtifactReference();

        artifact.setGroupId( legacyArtifactPath.getGroupId() );
        artifact.setArtifactId( legacyArtifactPath.getArtifactId() );
        artifact.setClassifier( legacyArtifactPath.getClassifier() );
        artifact.setVersion( legacyArtifactPath.getVersion() );
        artifact.setType( legacyArtifactPath.getType() );
        String path = repositoryContent.toPath( artifact );
        if ( !StringUtils.equals( path, legacyArtifactPath.getPath() ) )
        {
            throw new ArchivaRestServiceException(
                "artifact path reference '" + legacyArtifactPath.getPath() + "' does not match the initial path: '"
                    + path + "'", Response.Status.BAD_REQUEST.getStatusCode(), null );
        }

        try
        {

            archivaAdministration.addLegacyArtifactPath( legacyArtifactPath, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean deleteLegacyArtifactPath( String path )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.deleteLegacyArtifactPath( path, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }


    public Boolean addFileTypePattern( String fileTypeId, String pattern )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addFileTypePattern( fileTypeId, pattern, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean removeFileTypePattern( String fileTypeId, String pattern )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.removeFileTypePattern( fileTypeId, pattern, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public FileType getFileType( String fileTypeId )
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaAdministration.getFileType( fileTypeId );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void addFileType( FileType fileType )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addFileType( fileType, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean removeFileType( String fileTypeId )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.removeFileType( fileTypeId, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean enabledKnownContentConsumer( String knownContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addKnownContentConsumer( knownContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void enabledKnownContentConsumers( List<String> knownContentConsumers )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.setKnownContentConsumers( knownContentConsumers, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean disabledKnownContentConsumer( String knownContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.removeKnownContentConsumer( knownContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean enabledInvalidContentConsumer( String invalidContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addInvalidContentConsumer( invalidContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void enabledInvalidContentConsumers( List<String> invalidContentConsumers )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.setInvalidContentConsumers( invalidContentConsumers, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean disabledInvalidContentConsumer( String invalidContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.removeInvalidContentConsumer( invalidContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public List<FileType> getFileTypes()
        throws ArchivaRestServiceException
    {
        try
        {
            List<FileType> modelfileTypes = archivaAdministration.getFileTypes();
            if ( modelfileTypes == null || modelfileTypes.isEmpty() )
            {
                return Collections.emptyList();
            }
            return modelfileTypes;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public List<String> getKnownContentConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            return new ArrayList<String>( archivaAdministration.getKnownContentConsumers() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public List<String> getInvalidContentConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            return new ArrayList<String>( archivaAdministration.getInvalidContentConsumers() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public OrganisationInformation getOrganisationInformation()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaAdministration.getOrganisationInformation();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void setOrganisationInformation( OrganisationInformation organisationInformation )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.setOrganisationInformation( organisationInformation );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }


    public UiConfiguration getUiConfiguration()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaAdministration.getUiConfiguration();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void setUiConfiguration( UiConfiguration uiConfiguration )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.updateUiConfiguration( uiConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public String getApplicationUrl()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaAdministration.getUiConfiguration().getApplicationUrl();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public NetworkConfiguration getNetworkConfiguration()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaAdministration.getNetworkConfiguration();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public void setNetworkConfiguration( NetworkConfiguration networkConfiguration )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.setNetworkConfiguration( networkConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public List<AdminRepositoryConsumer> getKnownContentAdminRepositoryConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            AddAdminRepoConsumerClosure addAdminRepoConsumer =
                new AddAdminRepoConsumerClosure( archivaAdministration.getKnownContentConsumers() );
            CollectionUtils.forAllDo( repoConsumerUtil.getAvailableKnownConsumers(), addAdminRepoConsumer );
            List<AdminRepositoryConsumer> knownContentConsumers = addAdminRepoConsumer.getList();
            Collections.sort( knownContentConsumers, AdminRepositoryConsumerComparator.getInstance() );
            return knownContentConsumers;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public List<AdminRepositoryConsumer> getInvalidContentAdminRepositoryConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            AddAdminRepoConsumerClosure addAdminRepoConsumer =
                new AddAdminRepoConsumerClosure( archivaAdministration.getInvalidContentConsumers() );
            CollectionUtils.forAllDo( repoConsumerUtil.getAvailableInvalidConsumers(), addAdminRepoConsumer );
            List<AdminRepositoryConsumer> invalidContentConsumers = addAdminRepoConsumer.getList();
            Collections.sort( invalidContentConsumers, AdminRepositoryConsumerComparator.getInstance() );
            return invalidContentConsumers;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }
}
