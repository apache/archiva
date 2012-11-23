package org.apache.archiva.admin.repository.runtime;
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
import org.apache.archiva.admin.model.beans.ArchivaRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.RuntimeConfiguration;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.springframework.stereotype.Service;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service ( "archivaRuntimeConfigurationAdmin#default" )
public class DefaultArchivaRuntimeConfigurationAdmin
    extends AbstractRepositoryAdmin
    implements ArchivaRuntimeConfigurationAdmin
{


    public ArchivaRuntimeConfiguration getArchivaRuntimeConfigurationAdmin()
        throws RepositoryAdminException
    {
        return build( getArchivaConfiguration().getConfiguration().getRuntimeConfiguration() );
    }

    public void updateArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
        throws RepositoryAdminException
    {
        RuntimeConfiguration runtimeConfiguration = build( archivaRuntimeConfiguration );
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.setRuntimeConfiguration( runtimeConfiguration );
        try
        {
            getArchivaConfiguration().save( configuration );
        }
        catch ( RegistryException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    private ArchivaRuntimeConfiguration build( RuntimeConfiguration runtimeConfiguration )
    {
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration = new ArchivaRuntimeConfiguration();
        archivaRuntimeConfiguration.setUserManagerImpl( runtimeConfiguration.getUserManagerImpl() );
        return archivaRuntimeConfiguration;
    }

    private RuntimeConfiguration build( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
    {
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration();
        runtimeConfiguration.setUserManagerImpl( archivaRuntimeConfiguration.getUserManagerImpl() );
        return runtimeConfiguration;
    }
}
