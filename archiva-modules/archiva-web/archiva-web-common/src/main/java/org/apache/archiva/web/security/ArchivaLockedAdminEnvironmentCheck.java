package org.apache.archiva.web.security;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "environmentCheck#archiva-locked-admin-check" )
public class ArchivaLockedAdminEnvironmentCheck
    implements EnvironmentCheck
{

    protected Logger log = LoggerFactory.getLogger( getClass() );


    @Inject
    @Named( value = "rbacManager#cached" )
    private RBACManager rbacManager;

    /**
     * boolean detailing if this environment check has been executed
     */
    private boolean checked = false;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    private List<UserManager> userManagers;

    @PostConstruct
    protected void initialize()
        throws RepositoryAdminException
    {
        List<String> userManagerImpls =
            redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getUserManagerImpls();

        List<String> updated = new ArrayList<>(  );
        userManagers = new ArrayList<>( userManagerImpls.size() );

        for ( String beanId : userManagerImpls )
        {
            // for migration purpose to help users
            if ( StringUtils.equalsIgnoreCase( beanId, "jdo" ))
            {
                log.info( "jdo is not anymore supported we auto update to jpa" );
                beanId = "jpa";
            }
            updated.add( beanId );
            userManagers.add( applicationContext.getBean( "userManager#" + beanId, UserManager.class ) );
        }
        redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().setUserManagerImpls( updated );
    }

    /**
     * This environment check will unlock system administrator accounts that are locked on the restart of the
     * application when the environment checks are processed.
     *
     * @param violations
     */
    @Override
    public void validateEnvironment( List<String> violations )
    {
        if ( !checked )
        {

            for ( UserManager userManager : userManagers )
            {
                if ( userManager.isReadOnly() )
                {
                    continue;
                }
                List<String> roles = new ArrayList<>();
                roles.add( RedbackRoleConstants.SYSTEM_ADMINISTRATOR_ROLE );

                List<? extends UserAssignment> systemAdminstrators;
                try
                {
                    systemAdminstrators = rbacManager.getUserAssignmentsForRoles( roles );

                    for ( UserAssignment userAssignment : systemAdminstrators )
                    {
                        try
                        {
                            User admin = userManager.findUser( userAssignment.getPrincipal() );

                            if ( admin.isLocked() )
                            {
                                log.info( "Unlocking system administrator: {}", admin.getUsername() );
                                admin.setLocked( false );
                                userManager.updateUser( admin );
                            }
                        }
                        catch ( UserNotFoundException ne )
                        {
                            log.warn( "Dangling UserAssignment -> {}", userAssignment.getPrincipal() );
                        }
                        catch ( UserManagerException e )
                        {
                            log.warn( "fail to find user {} for admin unlock check: {}", userAssignment.getPrincipal(),
                                      e.getMessage() );
                        }
                    }
                }
                catch ( RbacManagerException e )
                {
                    log.warn( "Exception when checking for locked admin user: {}", e.getMessage(), e );
                }

                checked = true;
            }

        }

    }
}
