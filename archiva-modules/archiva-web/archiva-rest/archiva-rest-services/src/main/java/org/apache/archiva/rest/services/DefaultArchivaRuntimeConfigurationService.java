package org.apache.archiva.rest.services;
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
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ArchivaRuntimeConfigurationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service ("archivaRuntimeConfigurationService#rest")
public class DefaultArchivaRuntimeConfigurationService
    extends AbstractRestService
    implements ArchivaRuntimeConfigurationService
{
    @Inject
    private ArchivaRuntimeConfigurationAdmin archivaRuntimeConfigurationAdmin;

    @Inject
    @Named ( value = "userManager#archiva" )
    private UserManager userManager;

    public ArchivaRuntimeConfiguration getArchivaRuntimeConfigurationAdmin()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaRuntimeConfigurationAdmin.getArchivaRuntimeConfigurationAdmin();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public Boolean updateArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
        throws ArchivaRestServiceException
    {
        try
        {
            // has user manager impl changed ?
            boolean userManagerChanged = !StringUtils.equals( archivaRuntimeConfiguration.getUserManagerImpl(),
                                                             archivaRuntimeConfigurationAdmin.getArchivaRuntimeConfigurationAdmin().getUserManagerImpl() );
            archivaRuntimeConfigurationAdmin.updateArchivaRuntimeConfiguration( archivaRuntimeConfiguration );

            if ( userManagerChanged )
            {
                log.info( "user manager impl changed to {} reload it",
                          archivaRuntimeConfiguration.getUserManagerImpl() );
                userManager.initialize();
            }

            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }

    }
}


