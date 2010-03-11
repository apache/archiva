package org.apache.maven.archiva.web.action.admin.repositories;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.audit.Auditable;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract AdminRepositories Action base.
 * 
 * Base class for all repository administrative functions.
 * This should be neutral to the type of action (add/edit/delete) and type of repo (managed/remote)
 *
 * @version $Id$
 */
public abstract class AbstractRepositoriesAdminAction
    extends PlexusActionSupport
    implements SecureAction, Auditable
{
    /**
     * @plexus.requirement
     */
    protected ArchivaConfiguration archivaConfiguration;

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    /**
     * Save the configuration.
     * 
     * @param configuration the configuration to save.
     * @return the webwork result code to issue.
     * @throws IOException thrown if unable to save file to disk.
     * @throws InvalidConfigurationException thrown if configuration is invalid.
     * @throws RegistryException thrown if configuration subsystem has a problem saving the configuration to disk.
     */
    protected String saveConfiguration( Configuration configuration )
    {
        try
        {
            archivaConfiguration.save( configuration );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
            return INPUT;
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
    }

    /**
     * Get the list of ProxyConnectors that are present in the configuration.
     * 
     * @return a new list of ProxyConnectors present in the configuration.
     */
    protected List<ProxyConnectorConfiguration> getProxyConnectors()
    {
        return new ArrayList<ProxyConnectorConfiguration>( archivaConfiguration.getConfiguration().getProxyConnectors() );
    }
}
