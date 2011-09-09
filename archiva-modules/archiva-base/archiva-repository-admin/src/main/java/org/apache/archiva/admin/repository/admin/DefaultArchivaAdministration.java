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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.audit.AuditEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "archivaAdministration#default" )
public class DefaultArchivaAdministration
    extends AbstractRepositoryAdmin
    implements ArchivaAdministration
{
    public List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws RepositoryAdminException
    {
        List<LegacyArtifactPath> legacyArtifactPaths = new ArrayList<LegacyArtifactPath>();
        for ( org.apache.maven.archiva.configuration.LegacyArtifactPath legacyArtifactPath : getArchivaConfiguration().getConfiguration().getLegacyArtifactPaths() )
        {
            legacyArtifactPaths.add(
                new BeanReplicator().replicateBean( legacyArtifactPath, LegacyArtifactPath.class ) );
        }
        return legacyArtifactPaths;

    }

    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        configuration.addLegacyArtifactPath( new BeanReplicator().replicateBean( legacyArtifactPath,
                                                                                 org.apache.maven.archiva.configuration.LegacyArtifactPath.class ) );

        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ADD_LEGACY_PATH, auditInformation );
    }

    public void deleteLegacyArtifactPath( String path, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        org.apache.maven.archiva.configuration.LegacyArtifactPath legacyArtifactPath =
            new org.apache.maven.archiva.configuration.LegacyArtifactPath();

        legacyArtifactPath.setPath( path );
        configuration.removeLegacyArtifactPath( legacyArtifactPath );

        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.REMOVE_LEGACY_PATH, auditInformation );
    }

    public void updateRepositoryScanning( RepositoryScanning repositoryScanning, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        configuration.setRepositoryScanning(
            new BeanReplicator().replicateBean( repositoryScanning, RepositoryScanningConfiguration.class ) );

        saveConfiguration( configuration );
    }

    public RepositoryScanning getRepositoryScanning()
        throws RepositoryAdminException
    {
        return new BeanReplicator().replicateBean( getArchivaConfiguration().getConfiguration().getRepositoryScanning(),
                                                   RepositoryScanning.class );
    }

    public void addFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        org.apache.maven.archiva.configuration.FileType fileType = getFileTypeById( fileTypeId, configuration );
        if ( fileType == null )
        {
            return;
        }
        fileType.addPattern( pattern );

        saveConfiguration( configuration );
    }

    public void removeFileTypePattern( String fileTypeId, String pattern, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        org.apache.maven.archiva.configuration.FileType fileType = getFileTypeById( fileTypeId, configuration );
        if ( fileType == null )
        {
            return;
        }
        fileType.removePattern( pattern );

        saveConfiguration( configuration );
    }

    public FileType getFileType( String fileTypeId )
        throws RepositoryAdminException
    {
        org.apache.maven.archiva.configuration.FileType fileType =
            getFileTypeById( fileTypeId, getArchivaConfiguration().getConfiguration() );
        if ( fileType == null )
        {
            return null;
        }
        return new BeanReplicator().replicateBean( fileType, FileType.class );
    }

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
            new BeanReplicator().replicateBean( fileType, org.apache.maven.archiva.configuration.FileType.class ) );
        saveConfiguration( configuration );
    }

    public void removeFileType( String fileTypeId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        org.apache.maven.archiva.configuration.FileType fileType =
            new org.apache.maven.archiva.configuration.FileType();
        fileType.setId( fileTypeId );
        configuration.getRepositoryScanning().removeFileType( fileType );
        saveConfiguration( configuration );
    }

    public void addKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getRepositoryScanning().addKnownContentConsumer( knownContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ENABLE_REPO_CONSUMER, auditInformation );
    }

    public void removeKnownContentConsumer( String knownContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getRepositoryScanning().removeKnownContentConsumer( knownContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.DISABLE_REPO_CONSUMER, auditInformation );
    }

    public void addInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getRepositoryScanning().addInvalidContentConsumer( invalidContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.ENABLE_REPO_CONSUMER, auditInformation );
    }

    public void removeInvalidContentConsumer( String invalidContentConsumer, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getRepositoryScanning().removeInvalidContentConsumer( invalidContentConsumer );
        saveConfiguration( configuration );
        triggerAuditEvent( "", "", AuditEvent.DISABLE_REPO_CONSUMER, auditInformation );
    }

//-------------------------
    //
    //-------------------------

    private org.apache.maven.archiva.configuration.FileType getFileTypeById( String id, Configuration configuration )
    {
        for ( org.apache.maven.archiva.configuration.FileType fileType : configuration.getRepositoryScanning().getFileTypes() )
        {
            if ( StringUtils.equals( id, fileType.getId() ) )
            {
                return fileType;
            }
        }
        return null;
    }
}
