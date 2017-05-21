package org.apache.archiva.security;

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

import java.util.List;

import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.redback.rbac.RBACManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * ArchivaStandardRolesCheck tests for the existance of expected / standard roles and permissions.
 */
@Service("environmentCheck#archiva-required-roles")
public class ArchivaStandardRolesCheck
    implements EnvironmentCheck
{
    private Logger log = LoggerFactory.getLogger( ArchivaStandardRolesCheck.class );

    /**
     *
     */
    @Inject
    @Named(value = "rbacManager#cached")
    private RBACManager rbacManager;

    /**
     * boolean detailing if this environment check has been executed
     */
    private boolean checked = false;

    @Override
    public void validateEnvironment( List<String> violations )
    {
        if ( !checked )
        {
            String expectedRoles[] = new String[]{ ArchivaRoleConstants.SYSTEM_ADMINISTRATOR_ROLE,
                ArchivaRoleConstants.GLOBAL_REPOSITORY_MANAGER_ROLE,
                ArchivaRoleConstants.GLOBAL_REPOSITORY_OBSERVER_ROLE, ArchivaRoleConstants.GUEST_ROLE,
                ArchivaRoleConstants.REGISTERED_USER_ROLE, ArchivaRoleConstants.USER_ADMINISTRATOR_ROLE };

            log.info( "Checking the existance of required roles." );

            for ( String roleName : expectedRoles )
            {
                try
                {
                    if ( !rbacManager.roleExists( roleName ) )
                    {
                        violations.add( "Unable to validate the existances of the '" + roleName + "' role." );
                    }
                }
                catch ( RbacManagerException e )
                {
                    log.warn( "fail to verify existence of role '{}'", roleName );
                    violations.add( "Unable to validate the existances of the '" + roleName + "' role." );
                }
            }

            String expectedOperations[] = new String[]{ ArchivaRoleConstants.OPERATION_MANAGE_USERS,
                ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, ArchivaRoleConstants.OPERATION_REGENERATE_INDEX,
                ArchivaRoleConstants.OPERATION_RUN_INDEXER, ArchivaRoleConstants.OPERATION_ACCESS_REPORT,
                ArchivaRoleConstants.OPERATION_ADD_REPOSITORY, ArchivaRoleConstants.OPERATION_DELETE_REPOSITORY,
                ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS, ArchivaRoleConstants.OPERATION_EDIT_REPOSITORY,
                ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD, ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS,
                "archiva-guest" };

            log.info( "Checking the existance of required operations." );

            for ( String operation : expectedOperations )
            {
                if ( !rbacManager.operationExists( operation ) )
                {
                    violations.add( "Unable to validate the existances of the '" + operation + "' operation." );
                }
            }

            checked = true;
        }

    }

}
