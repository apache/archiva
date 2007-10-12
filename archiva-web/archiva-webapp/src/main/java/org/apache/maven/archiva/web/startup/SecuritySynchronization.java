package org.apache.maven.archiva.web.startup;

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

import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.List;

/**
 * ConfigurationSynchronization
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.web.startup.SecuritySynchronization"
 * role-hint="default"
 */
public class SecuritySynchronization
    extends AbstractLogEnabled
    implements RegistryListener
{
    /**
     * @plexus.requirement role-hint="default"
     */
    private RoleManager roleManager;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName ) )
        {
            synchConfiguration( archivaConfiguration.getConfiguration().getManagedRepositories() );
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void synchConfiguration( List<ManagedRepositoryConfiguration> repos )
    {
        // NOTE: Remote Repositories do not have roles or security placed around them.
        
        for ( ManagedRepositoryConfiguration repoConfig : repos )
        {
            // manage roles for repositories
            try
            {
                if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, 
                                                       repoConfig.getId() ) )
                {
                    roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, 
                                                     repoConfig.getId() );
                }

                if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, 
                                                       repoConfig.getId() ) )
                {
                    roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, 
                                                     repoConfig.getId() );
                }
            }
            catch ( RoleManagerException e )
            {
                // Log error.
                getLogger().error( "Unable to create roles for configured repositories: " + e.getMessage(), e );
            }
        }
    }

    public void startup()
        throws ArchivaException
    {
        synchConfiguration( archivaConfiguration.getConfiguration().getManagedRepositories() );
        archivaConfiguration.addChangeListener( this );
    }
}
