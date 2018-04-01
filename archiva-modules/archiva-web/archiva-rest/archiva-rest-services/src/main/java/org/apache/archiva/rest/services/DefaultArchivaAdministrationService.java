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
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.rest.api.model.AdminRepositoryConsumer;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.services.utils.AddAdminRepoConsumerClosure;
import org.apache.archiva.rest.services.utils.AdminRepositoryConsumerComparator;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service ( "archivaAdministrationService#default" )
public class DefaultArchivaAdministrationService
    extends AbstractRestService
    implements ArchivaAdministrationService
{
    @Inject
    private ArchivaAdministration archivaAdministration;


    @Inject
    private RepositoryContentConsumers repoConsumerUtil;

    @Override
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


    @Override
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


    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public List<String> getKnownContentConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            return new ArrayList<>( archivaAdministration.getKnownContentConsumers( ) );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<String> getInvalidContentConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            return new ArrayList<>( archivaAdministration.getInvalidContentConsumers( ) );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
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

    @Override
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

    @Override
    public Boolean registrationDisabled()
        throws ArchivaRestServiceException
    {
        return getUiConfiguration().isDisableRegistration();
    }

    @Override
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

    @Override
    public void setUiConfiguration( UiConfiguration uiConfiguration )
        throws ArchivaRestServiceException
    {
        try
        {
            // fix for MRM-1757
            // strip any trailing '/' at the end of the url so it won't affect url/link calculations in UI
            uiConfiguration.setApplicationUrl(StringUtils.stripEnd(uiConfiguration.getApplicationUrl(), "/"));

            archivaAdministration.updateUiConfiguration( uiConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public List<AdminRepositoryConsumer> getKnownContentAdminRepositoryConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            AddAdminRepoConsumerClosure addAdminRepoConsumer =
                new AddAdminRepoConsumerClosure( archivaAdministration.getKnownContentConsumers() );
            IterableUtils.forEach( repoConsumerUtil.getAvailableKnownConsumers(), addAdminRepoConsumer );
            List<AdminRepositoryConsumer> knownContentConsumers = addAdminRepoConsumer.getList();
            knownContentConsumers.sort( AdminRepositoryConsumerComparator.getInstance( ) );
            return knownContentConsumers;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<AdminRepositoryConsumer> getInvalidContentAdminRepositoryConsumers()
        throws ArchivaRestServiceException
    {
        try
        {
            AddAdminRepoConsumerClosure addAdminRepoConsumer =
                new AddAdminRepoConsumerClosure( archivaAdministration.getInvalidContentConsumers() );
            IterableUtils.forEach( repoConsumerUtil.getAvailableInvalidConsumers(), addAdminRepoConsumer );
            List<AdminRepositoryConsumer> invalidContentConsumers = addAdminRepoConsumer.getList();
            invalidContentConsumers.sort( AdminRepositoryConsumerComparator.getInstance( ) );
            return invalidContentConsumers;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }
}
