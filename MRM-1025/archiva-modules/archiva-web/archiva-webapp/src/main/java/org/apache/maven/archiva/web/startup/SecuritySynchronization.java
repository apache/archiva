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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.system.check.EnvironmentCheck;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigurationSynchronization
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.web.startup.SecuritySynchronization"
 * role-hint="default"
 */
public class SecuritySynchronization
    implements RegistryListener
{
    private Logger log = LoggerFactory.getLogger( SecuritySynchronization.class );
    
    /**
     * @plexus.requirement role-hint="default"
     */
    private RoleManager roleManager;

    /**
     * @plexus.requirement role-hint="cached"
     */
    private RBACManager rbacManager;

    /**
     * @plexus.requirement role="org.codehaus.plexus.redback.system.check.EnvironmentCheck"
     */
    private Map<String, EnvironmentCheck> checkers;

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
                if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoConfig
                    .getId() ) )
                {
                    roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoConfig
                        .getId() );
                }

                if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoConfig
                    .getId() ) )
                {
                    roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoConfig
                        .getId() );
                }
            }
            catch ( RoleManagerException e )
            {
                // Log error.
                log.error( "Unable to create roles for configured repositories: " + e.getMessage(), e );
            }
        }
    }

    public void startup()
        throws ArchivaException
    {
        executeEnvironmentChecks();

        synchConfiguration( archivaConfiguration.getConfiguration().getManagedRepositories() );
        archivaConfiguration.addChangeListener( this );

        if ( archivaConfiguration.isDefaulted() )
        {
            assignRepositoryObserverToGuestUser( archivaConfiguration.getConfiguration().getManagedRepositories() );
        }
    }

    private void executeEnvironmentChecks()
        throws ArchivaException
    {
        if ( ( checkers == null ) || CollectionUtils.isEmpty( checkers.values() ) )
        {
            throw new ArchivaException( "Unable to initialize the Redback Security Environment, "
                + "no Environment Check components found." );
        }

        List<String> violations = new ArrayList<String>();

        for ( Entry<String, EnvironmentCheck> entry : checkers.entrySet() )
        {
            EnvironmentCheck check = entry.getValue();
            log.info( "Running Environment Check: " + entry.getKey() );
            check.validateEnvironment( violations );
        }

        if ( CollectionUtils.isNotEmpty( violations ) )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "EnvironmentCheck Failure.\n" );
            msg.append( "======================================================================\n" );
            msg.append( " ENVIRONMENT FAILURE !! \n" );
            msg.append( "\n" );

            for ( String violation : violations )
            {
                msg.append( violation ).append( "\n" );
            }

            msg.append( "\n" );
            msg.append( "======================================================================" );
            log.error( msg.toString() );

            throw new ArchivaException( "Unable to initialize Redback Security Environment, [" + violations.size()
                + "] violation(s) encountered, See log for details." );
        }
    }

    private void assignRepositoryObserverToGuestUser( List<ManagedRepositoryConfiguration> repos )
    {
        for ( ManagedRepositoryConfiguration repoConfig : repos )
        {
            String repoId = repoConfig.getId();
            
            // TODO: Use the Redback / UserConfiguration..getString( "redback.default.guest" ) to get the right name.
            String principal = "guest";
            
            try
            {
                UserAssignment ua;

                if ( rbacManager.userAssignmentExists( principal ) )
                {
                    ua = rbacManager.getUserAssignment( principal );
                }
                else
                {
                    ua = rbacManager.createUserAssignment( principal );
                }

                ua.addRoleName( ArchivaRoleConstants.toRepositoryObserverRoleName( repoId ) );
                rbacManager.saveUserAssignment( ua );
            }
            catch ( RbacManagerException e )
            {
                log.warn( "Unable to add role [" + ArchivaRoleConstants.toRepositoryObserverRoleName( repoId )
                                      + "] to " + principal + " user.", e );
            }
        }
    }
}
