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

import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public abstract class AbstractRepositoryAdmin
{

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();


    @Inject
    private RepositoryCommonValidator repositoryCommonValidator;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;

    protected void triggerAuditEvent( String repositoryId, String resource, String action,
                                      AuditInformation auditInformation )
    {
        User user = auditInformation == null ? null : auditInformation.getUser();
        AuditEvent event =
            new AuditEvent( repositoryId, user == null ? "null" : (String) user.getPrincipal(), resource, action );
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
        catch ( RegistryException e )
        {
            throw new RepositoryAdminException( "Error occurred in the registry.", e );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new RepositoryAdminException( "Error occurred while saving the configuration.", e );
        }
    }

    protected List<ProxyConnectorConfiguration> getProxyConnectors()
    {
        return new ArrayList<ProxyConnectorConfiguration>(
            archivaConfiguration.getConfiguration().getProxyConnectors() );
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

    public void setRegistry( Registry registry )
    {
        this.registry = registry;
    }
}
