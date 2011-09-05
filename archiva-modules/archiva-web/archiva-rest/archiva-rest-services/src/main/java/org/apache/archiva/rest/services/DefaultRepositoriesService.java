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

import org.apache.archiva.admin.repository.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "repositoriesService#rest" )
public class DefaultRepositoriesService
    extends AbstractRestService
    implements RepositoriesService
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    // FIXME duplicate from xmlrpc
    // olamy move this to a common remote services api
    private static final String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";

    private static final String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";

    private static final String REPOSITORY_LOCATION_VALID_EXPRESSION = "^[-a-zA-Z0-9._/~:?!&amp;=\\\\]+$";

    @Inject
    protected RoleManager roleManager;

    @Inject
    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    // FIXME olamy move this to repository admin component !
    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled" );
        }
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setScanAll( fullScan );
        try
        {
            repositoryTaskScheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to schedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
        return true;
    }

    public Boolean alreadyScanning( String repositoryId )
    {
        return repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId );
    }

    public Boolean removeScanningTaskFromQueue( @PathParam( "repositoryId" ) String repositoryId )
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        try
        {
            return repositoryTaskScheduler.unQueueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to unschedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
    }
}


