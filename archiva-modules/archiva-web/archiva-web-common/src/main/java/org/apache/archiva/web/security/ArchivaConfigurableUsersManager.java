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
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.configurable.ConfigurableUserManager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service ( "userManager#archiva" )
public class ArchivaConfigurableUsersManager
    extends ConfigurableUserManager
{

    @Inject
    private ArchivaRuntimeConfigurationAdmin archivaRuntimeConfigurationAdmin;

    @Inject
    private ApplicationContext applicationContext;

    @Override
    public void initialize()
    {
        try
        {
            String userManagerImplStr =
                archivaRuntimeConfigurationAdmin.getArchivaRuntimeConfigurationAdmin().getUserManagerImpl();
            log.info( "use userManagerImpl: '{}'", userManagerImplStr );
            UserManager userManagerImpl =
                applicationContext.getBean( "userManager#" + userManagerImplStr, UserManager.class );
            setUserManagerImpl( userManagerImpl );
        }
        catch ( RepositoryAdminException e )
        {
            // revert to a default one ?
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.configurable.archiva";
    }
}
