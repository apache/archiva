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

import net.sf.beanlib.provider.BeanTransformer;
import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.admin.ArchivaAdministration;
import org.apache.archiva.rest.api.model.FileType;
import org.apache.archiva.rest.api.model.LegacyArtifactPath;
import org.apache.archiva.rest.api.model.RepositoryScanning;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
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
        throws ArchivaRestServiceException
    {
        try
        {
            List<LegacyArtifactPath> legacyArtifactPaths = new ArrayList<LegacyArtifactPath>();
            for ( org.apache.archiva.admin.repository.admin.LegacyArtifactPath legacyArtifactPath : archivaAdministration.getLegacyArtifactPaths() )
            {
                legacyArtifactPaths.add(
                    new BeanReplicator().replicateBean( legacyArtifactPath, LegacyArtifactPath.class ) );
            }
            return legacyArtifactPaths;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addLegacyArtifactPath( new BeanReplicator().replicateBean( legacyArtifactPath,
                                                                                             org.apache.archiva.admin.repository.admin.LegacyArtifactPath.class ),
                                                         getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
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
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public RepositoryScanning getRepositoryScanning()
        throws ArchivaRestServiceException
    {
        try
        {
            BeanTransformer beanTransformer = new BeanTransformer()
            {
                @Override
                protected <T> T createToInstance( Object from, Class<T> toClass )
                    throws InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException
                {
                    if ( from.getClass().equals( org.apache.maven.archiva.configuration.FileType.class ) )
                    {
                        return (T) new FileType();
                    }
                    return super.createToInstance( from, toClass );
                }
            };
            BeanReplicator beanReplicator = new BeanReplicator( beanTransformer );

            RepositoryScanning repositoryScanning =
                beanReplicator.replicateBean( archivaAdministration.getRepositoryScanning(), RepositoryScanning.class );

            return repositoryScanning;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public void updateRepositoryScanning( RepositoryScanning repositoryScanning )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.updateRepositoryScanning( new BeanReplicator().replicateBean( getRepositoryScanning(),
                                                                                                org.apache.archiva.admin.repository.admin.RepositoryScanning.class ),
                                                            getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
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
            throw new ArchivaRestServiceException( e.getMessage() );
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
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public FileType getFileType( String fileTypeId )
        throws ArchivaRestServiceException
    {
        try
        {
            org.apache.archiva.admin.repository.admin.FileType fileType =
                archivaAdministration.getFileType( fileTypeId );
            if ( fileType == null )
            {
                return null;
            }
            return new BeanReplicator().replicateBean( fileType, FileType.class );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public void addFileType( FileType fileType )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addFileType( new BeanReplicator().replicateBean( fileType,
                                                                                   org.apache.archiva.admin.repository.admin.FileType.class ),
                                               getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
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
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean addKnownContentConsumer( String knownContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addKnownContentConsumer( knownContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public void setKnownContentConsumers( List<String> knownContentConsumers )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.setKnownContentConsumers( knownContentConsumers, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean removeKnownContentConsumer( String knownContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.removeKnownContentConsumer( knownContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean addInvalidContentConsumer( String invalidContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.addInvalidContentConsumer( invalidContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public void setInvalidContentConsumers( List<String> invalidContentConsumers )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.setInvalidContentConsumers( invalidContentConsumers, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean removeInvalidContentConsumer( String invalidContentConsumer )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaAdministration.removeInvalidContentConsumer( invalidContentConsumer, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }
}
