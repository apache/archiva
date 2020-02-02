package org.apache.archiva.admin.repository.admin;
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
import org.apache.archiva.admin.model.beans.*;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.UserInterfaceOptions;
import org.apache.archiva.configuration.WebappConfiguration;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "archivaAdministration#default" )
public class DefaultArchivaAdministration
    extends AbstractRepositoryAdmin
    implements ArchivaAdministration
{


    @Override
    public List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws RepositoryAdminException
    {
        List<LegacyArtifactPath> legacyArtifactPaths = new ArrayList<>(
            getArchivaConfiguration().getConfiguration().getLegacyArtifactPaths().size() );
        for ( org.apache.archiva.configuration.LegacyArtifactPath legacyArtifactPath : getArchivaConfiguration().getConfiguration().getLegacyArtifactPaths() )
        {
            legacyArtifactPaths.add(
                getModelMapper().map( legacyArtifactPath, LegacyArtifactPath.class ) );
        }
        return legacyArtifactPaths;
    }

    @Override
    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        configuration.addLegacyArtifactPath( getModelMapper().map( legacyArtifactPath,
                                                                   org.apache.archiva.configuration.LegacyArtifactPath.class ) );

        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ADD_LEGACY_PATH, auditInformation );
    }

    @Override
    public void deleteLegacyArtifactPath( String path, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        org.apache.archiva.configuration.LegacyArtifactPath legacyArtifactPath =
            new org.apache.archiva.configuration.LegacyArtifactPath();

        legacyArtifactPath.setPath( path );
        configuration.removeLegacyArtifactPath( legacyArtifactPath );

        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.REMOVE_LEGACY_PATH, auditInformation );
    }


    @Override
    public void addFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        org.apache.archiva.configuration.FileType fileType = getFileTypeById( fileTypeId, configuration );
        if ( fileType == null )
        {
            return;
        }

        if ( fileType.getPatterns().contains( pattern ) )
        {
            throw new RepositoryAdminException(
                "File type [" + fileTypeId + "] already contains pattern [" + pattern + "]" );
        }
        fileType.addPattern( pattern );

        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ADD_PATTERN, auditInformation );
    }

    @Override
    public void removeFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        org.apache.archiva.configuration.FileType fileType = getFileTypeById( fileTypeId, configuration );
        if ( fileType == null )
        {
            return;
        }
        fileType.removePattern( pattern );

        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.REMOVE_PATTERN, auditInformation );
    }

    @Override
    public FileType getFileType( String fileTypeId )
        throws RepositoryAdminException
    {
        org.apache.archiva.configuration.FileType fileType =
            getFileTypeById( fileTypeId, getArchivaConfiguration().getConfiguration() );
        if ( fileType == null )
        {
            return null;
        }
        return getModelMapper().map( fileType, FileType.class );
    }

    @Override
    public void addFileType( FileType fileType, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        if ( getFileTypeById( fileType.getId(), configuration ) != null )
        {
            throw new RepositoryAdminException(
                "impossible to FileType with id " + fileType.getId() + " already exists" );
        }

        configuration.getRepositoryScanning().addFileType(
            getModelMapper().map( fileType, org.apache.archiva.configuration.FileType.class ) );
        saveConfiguration( configuration );
    }

    @Override
    public void removeFileType( String fileTypeId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        org.apache.archiva.configuration.FileType fileType = new org.apache.archiva.configuration.FileType();
        fileType.setId( fileTypeId );
        configuration.getRepositoryScanning().removeFileType( fileType );
        saveConfiguration( configuration );
    }

    @Override
    public void addKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        if ( configuration.getRepositoryScanning().getKnownContentConsumers().contains( knownContentConsumer ) )
        {
            log.warn( "skip adding knownContentConsumer {} as already here", knownContentConsumer );
            return;
        }
        configuration.getRepositoryScanning().addKnownContentConsumer( knownContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ENABLE_REPO_CONSUMER, auditInformation );
    }

    @Override
    public void removeKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getRepositoryScanning().removeKnownContentConsumer( knownContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.DISABLE_REPO_CONSUMER, auditInformation );
    }

    @Override
    public void addInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        if ( configuration.getRepositoryScanning().getInvalidContentConsumers().contains( invalidContentConsumer ) )
        {
            log.warn( "skip adding invalidContentConsumer {} as already here", invalidContentConsumer );
            return;
        }
        configuration.getRepositoryScanning().addInvalidContentConsumer( invalidContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ENABLE_REPO_CONSUMER, auditInformation );
    }

    @Override
    public void removeInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getRepositoryScanning().removeInvalidContentConsumer( invalidContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.DISABLE_REPO_CONSUMER, auditInformation );
    }

    @Override
    public void setKnownContentConsumers( List<String> knownContentConsumers, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        if ( knownContentConsumers == null )
        {
            return;
        }
        for ( String knowContentConsumer : knownContentConsumers )
        {
            addKnownContentConsumer( knowContentConsumer, auditInformation );
        }
    }

    @Override
    public void setInvalidContentConsumers( List<String> invalidContentConsumers, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        if ( invalidContentConsumers == null )
        {
            return;
        }
        for ( String invalidContentConsumer : invalidContentConsumers )
        {
            addKnownContentConsumer( invalidContentConsumer, auditInformation );
        }
    }

    @Override
    public List<FileType> getFileTypes()
        throws RepositoryAdminException
    {
        List<org.apache.archiva.configuration.FileType> configFileTypes =
            getArchivaConfiguration().getConfiguration().getRepositoryScanning().getFileTypes();
        if ( configFileTypes == null || configFileTypes.isEmpty() )
        {
            return Collections.emptyList();
        }
        List<FileType> fileTypes = new ArrayList<>( configFileTypes.size() );
        for ( org.apache.archiva.configuration.FileType fileType : configFileTypes )
        {
            fileTypes.add( getModelMapper().map( fileType, FileType.class ) );
        }
        return fileTypes;
    }

    @Override
    public List<String> getKnownContentConsumers()
        throws RepositoryAdminException
    {
        return new ArrayList<>(
            getArchivaConfiguration().getConfiguration().getRepositoryScanning().getKnownContentConsumers() );
    }

    @Override
    public List<String> getInvalidContentConsumers()
        throws RepositoryAdminException
    {
        return new ArrayList<>(
            getArchivaConfiguration().getConfiguration().getRepositoryScanning().getInvalidContentConsumers() );
    }

    @Override
    public OrganisationInformation getOrganisationInformation()
        throws RepositoryAdminException
    {
        org.apache.archiva.configuration.OrganisationInformation organisationInformation =
            getArchivaConfiguration().getConfiguration().getOrganisationInfo();
        if ( organisationInformation == null )
        {
            return null;
        }
        return getModelMapper().map( organisationInformation, OrganisationInformation.class );
    }

    private String fixUrl(String url, String propertyName)  throws RepositoryAdminException {
        if ( StringUtils.isNotEmpty( url ) )
        {
            if ( !ResourceUtils.isUrl( url ) )
            {
                throw new RepositoryAdminException( "Bad URL in " + propertyName + ": " + url );
            }
            try {
                URI urlToCheck = new URI(url);
                return urlToCheck.toString();
            } catch (URISyntaxException e) {
                throw new RepositoryAdminException( "Bad URL in " + propertyName + ": " + url );
            }
        }
        return url;

    }

    private String convertName(String name) {
        return StringEscapeUtils.escapeHtml4( StringUtils.trimToEmpty( name ) );
    }

    @Override
    public void setOrganisationInformation( OrganisationInformation organisationInformation )
        throws RepositoryAdminException
    {

        organisationInformation.setUrl(fixUrl(organisationInformation.getUrl(), "url"));
        organisationInformation.setLogoLocation(fixUrl( organisationInformation.getLogoLocation(), "logoLocation" ));
        Configuration configuration = getArchivaConfiguration( ).getConfiguration( );
        if ( organisationInformation != null )
        {
            organisationInformation.setName( convertName( organisationInformation.getName() ));
            org.apache.archiva.configuration.OrganisationInformation organisationInformationModel =
                getModelMapper( ).map( organisationInformation,
                    org.apache.archiva.configuration.OrganisationInformation.class );
            configuration.setOrganisationInfo( organisationInformationModel );
        }
        else
        {
            configuration.setOrganisationInfo( null );
        }
        saveConfiguration( configuration );
    }

    @Override
    public UiConfiguration getUiConfiguration()
        throws RepositoryAdminException
    {
        WebappConfiguration webappConfiguration = getArchivaConfiguration().getConfiguration().getWebapp();
        if ( webappConfiguration == null )
        {
            return null;
        }
        UserInterfaceOptions userInterfaceOptions = webappConfiguration.getUi();
        if ( userInterfaceOptions == null )
        {
            return null;
        }
        return getModelMapper().map( userInterfaceOptions, UiConfiguration.class );
    }

    @Override
    public void updateUiConfiguration( UiConfiguration uiConfiguration )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        if ( uiConfiguration != null )
        {

            UserInterfaceOptions userInterfaceOptions =
                getModelMapper().map( uiConfiguration, UserInterfaceOptions.class );
            configuration.getWebapp().setUi( userInterfaceOptions );
        }
        else
        {
            configuration.getWebapp().setUi( null );
        }
        saveConfiguration( configuration );

    }

    @Override
    public NetworkConfiguration getNetworkConfiguration()
        throws RepositoryAdminException
    {
        org.apache.archiva.configuration.NetworkConfiguration networkConfiguration =
            getArchivaConfiguration().getConfiguration().getNetworkConfiguration();

        if ( networkConfiguration == null )
        {
            return null;
        }
        return getModelMapper().map( networkConfiguration, NetworkConfiguration.class );
    }

    @Override
    public void setNetworkConfiguration( NetworkConfiguration networkConfiguration )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        if ( networkConfiguration == null )
        {
            configuration.setNetworkConfiguration( null );
        }
        else
        {
            configuration.setNetworkConfiguration( getModelMapper().map( networkConfiguration,
                                                                         org.apache.archiva.configuration.NetworkConfiguration.class ) );
        }
        // setupWagon( networkConfiguration );
        saveConfiguration( configuration );
    }


    //-------------------------
    //
    //-------------------------

    private org.apache.archiva.configuration.FileType getFileTypeById( String id, Configuration configuration )
    {
        for ( org.apache.archiva.configuration.FileType fileType : configuration.getRepositoryScanning().getFileTypes() )
        {
            if ( StringUtils.equals( id, fileType.getId() ) )
            {
                return fileType;
            }
        }
        return null;
    }

}
