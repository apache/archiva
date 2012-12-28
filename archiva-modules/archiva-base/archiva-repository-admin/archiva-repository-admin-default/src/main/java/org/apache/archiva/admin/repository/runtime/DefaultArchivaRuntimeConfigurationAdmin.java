package org.apache.archiva.admin.repository.runtime;
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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ArchivaRuntimeConfiguration;
import org.apache.archiva.admin.model.beans.CacheConfiguration;
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.redback.components.cache.Cache;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "archivaRuntimeConfigurationAdmin#default" )
public class DefaultArchivaRuntimeConfigurationAdmin
    implements ArchivaRuntimeConfigurationAdmin
{

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "cache#url-failures-cache" )
    private Cache usersCache;

    @PostConstruct
    public void initialize()
        throws RepositoryAdminException
    {
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration = getArchivaRuntimeConfiguration();

        boolean save = false;

        // NPE free
        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration() == null )
        {
            archivaRuntimeConfiguration.setUrlFailureCacheConfiguration( new CacheConfiguration() );
        }

        // if -1 it means non initialized to take values from the spring bean
        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToIdleSeconds() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setTimeToIdleSeconds(
                usersCache.getTimeToIdleSeconds() );
            save = true;

        }
        usersCache.setTimeToIdleSeconds(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToIdleSeconds() );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToLiveSeconds() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setTimeToLiveSeconds(
                usersCache.getTimeToLiveSeconds() );
            save = true;

        }
        usersCache.setTimeToLiveSeconds(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getTimeToLiveSeconds() );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsInMemory() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setMaxElementsInMemory(
                usersCache.getMaxElementsInMemory() );
            save = true;
        }
        usersCache.setMaxElementsInMemory(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsInMemory() );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsOnDisk() < 0 )
        {
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().setMaxElementsOnDisk(
                usersCache.getMaxElementsOnDisk() );
            save = true;
        }
        usersCache.setMaxElementsOnDisk(
            archivaRuntimeConfiguration.getUrlFailureCacheConfiguration().getMaxElementsOnDisk() );

        if ( save )
        {
            updateArchivaRuntimeConfiguration( archivaRuntimeConfiguration );
        }

    }

    public ArchivaRuntimeConfiguration getArchivaRuntimeConfiguration()
        throws RepositoryAdminException
    {
        return build( archivaConfiguration.getConfiguration().getArchivaRuntimeConfiguration() );
    }

    public void updateArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
        throws RepositoryAdminException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected ArchivaRuntimeConfiguration build(
        org.apache.archiva.configuration.ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
    {
        if ( archivaRuntimeConfiguration == null )
        {
            return new ArchivaRuntimeConfiguration();
        }

        ArchivaRuntimeConfiguration res =
            new BeanReplicator().replicateBean( archivaRuntimeConfiguration, ArchivaRuntimeConfiguration.class );

        if ( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration() != null )
        {

            res.setUrlFailureCacheConfiguration(
                new BeanReplicator().replicateBean( archivaRuntimeConfiguration.getUrlFailureCacheConfiguration(),
                                                    CacheConfiguration.class ) );

        }

        return res;
    }
}


