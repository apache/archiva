package org.apache.archiva.admin.repository;
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
import org.apache.archiva.admin.model.RepositoryCommonValidator;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.repository.events.AuditListener;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public abstract class AbstractRepositoryAdmin
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Autowired(required = false)
    private List<AuditListener> auditListeners = new ArrayList<>();

    @Inject
    private RepositoryCommonValidator repositoryCommonValidator;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named(value = "commons-configuration")
    private Registry registry;

    protected void triggerAuditEvent( String repositoryId, String resource, String action,
                                      AuditInformation auditInformation )
    {
        User user = auditInformation == null ? null : auditInformation.getUser();
        AuditEvent event = new AuditEvent( repositoryId, user == null ? "null" : user.getUsername(), resource, action );
        event.setRemoteIP( auditInformation == null ? "null" : auditInformation.getRemoteAddr() );

        for ( AuditListener listener : getAuditListeners() )
        {
            listener.auditEvent( event );
        }

    }

    protected void saveConfiguration( Configuration config )
        throws RepositoryAdminException
    {
        try
        {
            getArchivaConfiguration().save( config );
        }
        catch ( org.apache.archiva.redback.components.registry.RegistryException e )
        {
            throw new RepositoryAdminException( "Error occurred in the registry: " + e.getLocalizedMessage(), e );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new RepositoryAdminException(
                "Error occurred while saving the configuration: " + e.getLocalizedMessage(), e );
        }
    }

    private static class ModelMapperHolder
    {
        private static ModelMapper MODEL_MAPPER = new ModelMapper();

        static
        {
            MODEL_MAPPER.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
        }

    }

    protected ModelMapper getModelMapper()
    {
        return ModelMapperHolder.MODEL_MAPPER;
    }

    public List<AuditListener> getAuditListeners()
    {
        return auditListeners;
    }

    public void setAuditListeners( List<AuditListener> auditListeners )
    {
        this.auditListeners = auditListeners;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public RepositoryCommonValidator getRepositoryCommonValidator()
    {
        return repositoryCommonValidator;
    }

    public void setRepositoryCommonValidator( RepositoryCommonValidator repositoryCommonValidator )
    {
        this.repositoryCommonValidator = repositoryCommonValidator;
    }

    public Registry getRegistry()
    {
        return registry;
    }

    public void setRegistry( org.apache.archiva.redback.components.registry.Registry registry )
    {
        this.registry = registry;
    }
}
