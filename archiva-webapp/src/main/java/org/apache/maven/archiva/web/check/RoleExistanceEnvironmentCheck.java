package org.apache.maven.archiva.web.check;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
import org.codehaus.plexus.rbac.profile.RoleProfileManager;
import org.codehaus.plexus.security.system.check.EnvironmentCheck;

import java.util.Iterator;
import java.util.List;

/**
 * RoleExistanceEnvironmentCheck:
 *
 * Under certain circumstances it is possible that the user store and/or role store
 * have been wiped or reset and its important to see if there are repositories already
 * configured in archiva that need to reinitialized in terms of having their roles created.
 *
 * @author: Jesse McConnell <jmcconnell@apache.org>
 * @version: $ID:
 *
 * @plexus.component
 *   role="org.codehaus.plexus.security.system.check.EnvironmentCheck"
 *   role-hint="repository-role-check"
 */
public class RoleExistanceEnvironmentCheck
    extends AbstractLogEnabled
    implements EnvironmentCheck
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement role-hint="archiva"
     */
    private RoleProfileManager roleProfileManager;

    private boolean checked = false;

    public void validateEnvironment( List list )
    {
        if ( !checked )
        {
            try
            {
                // check if there is potential for role/repo disconnect
                if ( configurationStore.getConfigurationFromStore().isValid() )
                {
                    List repos = configurationStore.getConfigurationFromStore().getRepositories();

                    for ( Iterator i = repos.iterator(); i.hasNext(); )
                    {
                        RepositoryConfiguration repository = (RepositoryConfiguration) i.next();

                        roleProfileManager.getDynamicRole( "archiva-repository-manager", repository.getId() );

                        roleProfileManager.getDynamicRole( "archiva-repository-observer", repository.getId() );
                    }
                }
            }
            catch ( ConfigurationStoreException cse )
            {
                list.add( this.getClass().getName() + " error loading configuration store: " + cse.getMessage() );
                getLogger().info( "error loading configuration store", cse );
            }
            catch ( RoleProfileException rpe )
            {
                list.add( this.getClass().getName() + "error initializing roles: " + rpe.getMessage() );
                getLogger().info( "error initializing roles", rpe );
            }

            checked = true;
        }
    }

}
