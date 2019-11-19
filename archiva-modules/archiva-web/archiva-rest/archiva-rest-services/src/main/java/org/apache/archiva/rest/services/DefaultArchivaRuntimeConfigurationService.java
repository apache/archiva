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
import org.apache.archiva.admin.model.beans.ArchivaRuntimeConfiguration;
import org.apache.archiva.admin.model.beans.CacheConfiguration;
import org.apache.archiva.admin.model.beans.FileLockConfiguration;
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ArchivaRuntimeConfigurationService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "archivaRuntimeConfigurationService#rest" )
public class DefaultArchivaRuntimeConfigurationService
    extends AbstractRestService
    implements ArchivaRuntimeConfigurationService
{
    @Inject
    private ArchivaRuntimeConfigurationAdmin archivaRuntimeConfigurationAdmin;

    @Inject
    @Named( value = "cache#url-failures-cache" )
    private Cache usersCache;

    @Inject
    @Named( value = "fileLockManager#default" )
    private FileLockManager fileLockManager;

    @Override
    public ArchivaRuntimeConfiguration getArchivaRuntimeConfiguration()
        throws ArchivaRestServiceException
    {
        try
        {
            return archivaRuntimeConfigurationAdmin.getArchivaRuntimeConfiguration();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean updateArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
        throws ArchivaRestServiceException
    {
        try
        {
            archivaRuntimeConfigurationAdmin.updateArchivaRuntimeConfiguration( archivaRuntimeConfiguration );
            CacheConfiguration cacheConfiguration = archivaRuntimeConfiguration.getUrlFailureCacheConfiguration();
            if ( cacheConfiguration != null )
            {
                usersCache.setTimeToLiveSeconds( cacheConfiguration.getTimeToLiveSeconds() );
                usersCache.setTimeToIdleSeconds( cacheConfiguration.getTimeToIdleSeconds() );
                usersCache.setMaxElementsOnDisk( cacheConfiguration.getMaxElementsOnDisk() );
                usersCache.setMaxElementsInMemory( cacheConfiguration.getMaxElementsInMemory() );
            }

            FileLockConfiguration fileLockConfiguration = archivaRuntimeConfiguration.getFileLockConfiguration();
            if ( fileLockConfiguration != null )
            {
                fileLockManager.setTimeout( fileLockConfiguration.getLockingTimeout() );
                fileLockManager.setSkipLocking( fileLockConfiguration.isSkipLocking() );
            }


        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        return Boolean.TRUE;
    }
}
