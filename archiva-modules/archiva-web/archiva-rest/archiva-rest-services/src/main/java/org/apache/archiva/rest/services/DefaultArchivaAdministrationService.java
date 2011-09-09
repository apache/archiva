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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.admin.ArchivaAdministration;
import org.apache.archiva.rest.api.model.FileType;
import org.apache.archiva.rest.api.model.LegacyArtifactPath;
import org.apache.archiva.rest.api.model.RepositoryScanning;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "archivaAdministrationService#default" )
public class DefaultArchivaAdministrationService
    extends AbstractRestService
    implements ArchivaAdministrationService
{
    @Inject
    private ArchivaAdministration archivaAdministration;

    public List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws RepositoryAdminException
    {
        List<LegacyArtifactPath> legacyArtifactPaths = new ArrayList<LegacyArtifactPath>();
        for ( org.apache.archiva.admin.repository.admin.LegacyArtifactPath legacyArtifactPath : archivaAdministration.getLegacyArtifactPaths() )
        {
            legacyArtifactPaths.add(
                new BeanReplicator().replicateBean( legacyArtifactPath, LegacyArtifactPath.class ) );
        }
        return legacyArtifactPaths;
    }

    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
        throws RepositoryAdminException
    {
        archivaAdministration.addLegacyArtifactPath( new BeanReplicator().replicateBean( legacyArtifactPath,
                                                                                         org.apache.archiva.admin.repository.admin.LegacyArtifactPath.class ),
                                                     getAuditInformation() );
    }

    public Boolean deleteLegacyArtifactPath( String path )
        throws RepositoryAdminException
    {
        archivaAdministration.deleteLegacyArtifactPath( path, getAuditInformation() );
        return Boolean.TRUE;
    }

    public RepositoryScanning getRepositoryScanning()
        throws RepositoryAdminException
    {
        return new BeanReplicator().replicateBean( archivaAdministration.getRepositoryScanning(),
                                                   RepositoryScanning.class );
    }

    public void updateRepositoryScanning( RepositoryScanning repositoryScanning )
        throws RepositoryAdminException
    {
        archivaAdministration.updateRepositoryScanning( new BeanReplicator().replicateBean( getRepositoryScanning(),
                                                                                            org.apache.archiva.admin.repository.admin.RepositoryScanning.class ),
                                                        getAuditInformation() );
    }

    public Boolean addFileTypePattern( String fileTypeId, String pattern )
        throws RepositoryAdminException
    {
        archivaAdministration.addFileTypePattern( fileTypeId, pattern, getAuditInformation() );
        return Boolean.TRUE;
    }

    public Boolean removeFileTypePattern( String fileTypeId, String pattern )
        throws RepositoryAdminException
    {
        archivaAdministration.removeFileTypePattern( fileTypeId, pattern, getAuditInformation() );
        return Boolean.TRUE;
    }

    public FileType getFileType( String fileTypeId )
        throws RepositoryAdminException
    {
        org.apache.archiva.admin.repository.admin.FileType fileType = archivaAdministration.getFileType( fileTypeId );
        if ( fileType == null )
        {
            return null;
        }
        return new BeanReplicator().replicateBean( fileType, FileType.class );
    }

    public void addFileType( FileType fileType )
        throws RepositoryAdminException
    {
        archivaAdministration.addFileType(
            new BeanReplicator().replicateBean( fileType, org.apache.archiva.admin.repository.admin.FileType.class ),
            getAuditInformation() );

    }

    public Boolean removeFileType( String fileTypeId )
        throws RepositoryAdminException
    {
        archivaAdministration.removeFileType( fileTypeId, getAuditInformation() );
        return Boolean.TRUE;
    }

    public Boolean addKnownContentConsumer( String knownContentConsumer )
        throws RepositoryAdminException
    {
        archivaAdministration.addKnownContentConsumer( knownContentConsumer, getAuditInformation() );
        return Boolean.TRUE;
    }

    public void setKnownContentConsumers( List<String> knownContentConsumers )
        throws RepositoryAdminException
    {
        archivaAdministration.setKnownContentConsumers( knownContentConsumers, getAuditInformation() );
    }

    public Boolean removeKnownContentConsumer( String knownContentConsumer )
        throws RepositoryAdminException
    {
        archivaAdministration.removeKnownContentConsumer( knownContentConsumer, getAuditInformation() );
        return Boolean.TRUE;
    }

    public Boolean addInvalidContentConsumer( String invalidContentConsumer )
        throws RepositoryAdminException
    {
        archivaAdministration.addInvalidContentConsumer( invalidContentConsumer, getAuditInformation() );
        return Boolean.TRUE;
    }

    public void setInvalidContentConsumers( List<String> invalidContentConsumers )
        throws RepositoryAdminException
    {
        archivaAdministration.setInvalidContentConsumers( invalidContentConsumers, getAuditInformation() );
    }

    public Boolean removeInvalidContentConsumer( String invalidContentConsumer )
        throws RepositoryAdminException
    {
        archivaAdministration.removeInvalidContentConsumer( invalidContentConsumer, getAuditInformation() );
        return Boolean.TRUE;
    }
}
