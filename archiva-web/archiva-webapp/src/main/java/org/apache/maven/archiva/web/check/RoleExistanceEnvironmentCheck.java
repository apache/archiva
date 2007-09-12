package org.apache.maven.archiva.web.check;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.system.check.EnvironmentCheck;

import java.util.List;

/**
 * RoleExistanceEnvironmentCheck:
 * <p/>
 * Under certain circumstances it is possible that the user store and/or role store
 * have been wiped or reset and its important to see if there are repositories already
 * configured in archiva that need to reinitialized in terms of having their roles created.
 *
 * @author: Jesse McConnell <jmcconnell@apache.org>
 * @version: $ID:
 * @plexus.component role="org.codehaus.plexus.security.system.check.EnvironmentCheck"
 * role-hint="repository-role-check"
 */
public class RoleExistanceEnvironmentCheck
    extends AbstractLogEnabled
    implements EnvironmentCheck
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement role-hint="default"
     */
    private RoleManager roleManager;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    private boolean checked;

    public void validateEnvironment( List list )
    {
        if ( !checked )
        {
            try
            {
                Configuration config = configuration.getConfiguration();
                for ( ManagedRepositoryConfiguration repository : config.getManagedRepositoriesAsMap().values() )
                {
                    if ( !roleManager.templatedRoleExists( "archiva-repository-manager", repository.getId() ) )
                    {
                        roleManager.createTemplatedRole( "archiva-repository-manager", repository.getId() );
                    }

                    if ( !roleManager.templatedRoleExists( "archiva-repository-observer", repository.getId() ) )
                    {
                        roleManager.createTemplatedRole( "archiva-repository-observer", repository.getId() );
                    }
                }
            }
            catch ( RoleManagerException rpe )
            {
                list.add( this.getClass().getName() + "error initializing roles: " + rpe.getMessage() );
                getLogger().info( "error initializing roles", rpe );
            }

            checked = true;
        }
    }

}
