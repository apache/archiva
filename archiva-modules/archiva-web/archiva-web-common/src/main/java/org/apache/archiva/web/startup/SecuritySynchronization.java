package org.apache.archiva.web.startup;

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

import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * ConfigurationSynchronization
 */
@Service
public class SecuritySynchronization
    implements RegistryListener
{
    private Logger log = LoggerFactory.getLogger( SecuritySynchronization.class );

    @Inject
    private RoleManager roleManager;

    @Inject
    @Named(value = "rbacManager#cached")
    private RBACManager rbacManager;

    private Map<String, EnvironmentCheck> checkers;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    private ApplicationContext applicationContext;

    @PostConstruct
    public void initialize()
    {
        checkers = getBeansOfType( EnvironmentCheck.class );
    }

    protected <T> Map<String, T> getBeansOfType( Class<T> clazz )
    {
        //TODO do some caching here !!!
        // olamy : with plexus we get only roleHint
        // as per convention we named spring bean role#hint remove role# if exists
        Map<String, T> springBeans = applicationContext.getBeansOfType( clazz );

        Map<String, T> beans = new HashMap<>( springBeans.size() );

        for ( Entry<String, T> entry : springBeans.entrySet() )
        {
            String key = StringUtils.substringAfterLast( entry.getKey(), "#" );
            beans.put( key, entry.getValue() );
        }
        return beans;
    }

    @Override
    public void afterConfigurationChange( org.apache.archiva.redback.components.registry.Registry registry,
                                          String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName ) && propertyName.endsWith( ".id" ) )
        {
            if ( propertyValue != null )
            {
                syncRepoConfiguration( (String) propertyValue );
            }
        }
    }

    @Override
    public void beforeConfigurationChange( org.apache.archiva.redback.components.registry.Registry registry,
                                           String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void synchConfiguration( List<ManagedRepositoryConfiguration> repos )
    {
        // NOTE: Remote Repositories do not have roles or security placed around them.

        for ( ManagedRepositoryConfiguration repoConfig : repos )
        {
            syncRepoConfiguration( repoConfig.getId() );
        }
    }

    private void syncRepoConfiguration( String id )
    {
        // manage roles for repositories
        try
        {
            if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, id ) )
            {
                roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, id );
            }
            else
            {
                roleManager.verifyTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, id );
            }

            if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, id ) )
            {
                roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, id );
            }
            else
            {
                roleManager.verifyTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, id );
            }
        }
        catch ( RoleManagerException e )
        {
            // Log error.
            log.error( "Unable to create roles for configured repositories: " + e.getMessage(), e );
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
            throw new ArchivaException(
                "Unable to initialize the Redback Security Environment, " + "no Environment Check components found." );
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();

        List<String> violations = new ArrayList<>();

        for ( Entry<String, EnvironmentCheck> entry : checkers.entrySet() )
        {
            EnvironmentCheck check = entry.getValue();
            List<String> v = new ArrayList<>();
            check.validateEnvironment( v );
            log.info( "Environment Check: {} -> {} violation(s)", entry.getKey(), v.size() );
            for ( String s : v )
            {
                violations.add( "[" + entry.getKey() + "] " + s );
            }
        }

        if ( CollectionUtils.isNotEmpty( violations ) )
        {
            StringBuilder msg = new StringBuilder();
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

        stopWatch.stop();
        log.info( "time to execute all EnvironmentCheck: {} ms", stopWatch.getTime() );
    }


    private void assignRepositoryObserverToGuestUser( List<ManagedRepositoryConfiguration> repos )
    {
        for ( ManagedRepositoryConfiguration repoConfig : repos )
        {
            String repoId = repoConfig.getId();

            String principal = UserManager.GUEST_USERNAME;

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
                log.warn( "Unable to add role [{}] to {} user.", ArchivaRoleConstants.toRepositoryObserverRoleName( repoId ), principal, e );
            }
        }
    }
}
