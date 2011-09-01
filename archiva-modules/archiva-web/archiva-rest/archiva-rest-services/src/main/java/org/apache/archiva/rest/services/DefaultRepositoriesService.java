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

import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepositoryAdmin;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.codehaus.redback.rest.services.RedbackRequestInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PathParam;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "repositoriesService#rest" )
public class DefaultRepositoriesService
    implements RepositoriesService
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    // FIXME duplicate from xmlrpc
    // olamy move this to a common remote services api
    private static final String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";

    private static final String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";

    private static final String REPOSITORY_LOCATION_VALID_EXPRESSION = "^[-a-zA-Z0-9._/~:?!&amp;=\\\\]+$";

    // TODO move this field to an abstract class
    @Inject
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

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

    public List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {
        List<org.apache.archiva.admin.repository.managed.ManagedRepository> repos =
            managedRepositoryAdmin.getManagedRepositories();

        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>( repos.size() );

        for ( org.apache.archiva.admin.repository.managed.ManagedRepository repoConfig : repos )
        {
            // TODO staging repo too
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                       repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases(),
                                       repoConfig.isBlockRedeployments(), false, repoConfig.getCronExpression() );
            managedRepos.add( repo );
        }

        return managedRepos;
    }

    public ManagedRepository getManagedRepository( String repositoryId )
        throws RepositoryAdminException
    {
        List<ManagedRepository> repos = getManagedRepositories();
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( repo.getId(), repositoryId ) )
            {
                return repo;
            }
        }
        return null;
    }

    public Boolean deleteManagedRepository( String repoId, boolean deleteContent )
        throws Exception
    {

        return managedRepositoryAdmin.deleteManagedRepository( repoId, getAuditInformation(), deleteContent );
    }

    public List<RemoteRepository> getRemoteRepositories()
    {
        Configuration config = archivaConfiguration.getConfiguration();
        List<RemoteRepositoryConfiguration> remoteRepoConfigs = config.getRemoteRepositories();

        List<RemoteRepository> remoteRepos = new ArrayList<RemoteRepository>( remoteRepoConfigs.size() );

        for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
        {
            RemoteRepository repo = new RemoteRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getUrl(),
                                                          repoConfig.getLayout() );
            remoteRepos.add( repo );
        }

        return remoteRepos;
    }

    public Boolean addManagedRepository( ManagedRepository managedRepository )
        throws Exception
    {
        org.apache.archiva.admin.repository.managed.ManagedRepository repo =
            new org.apache.archiva.admin.repository.managed.ManagedRepository();
        repo.setLocation( managedRepository.getLocation() );
        repo.setBlockRedeployments( managedRepository.isBlockRedeployments() );
        repo.setCronExpression( managedRepository.getCronExpression() );
        repo.setId( managedRepository.getId() );
        repo.setLayout( managedRepository.getLayout() );
        repo.setName( managedRepository.getName() );
        repo.setReleases( managedRepository.isReleases() );
        repo.setSnapshots( managedRepository.isSnapshots() );
        return managedRepositoryAdmin.addManagedRepository( repo, managedRepository.isStageRepoNeeded(),
                                                            getAuditInformation() );
    }


    public Boolean updateManagedRepository( ManagedRepository managedRepository )
        throws Exception
    {
        org.apache.archiva.admin.repository.managed.ManagedRepository repo =
            new org.apache.archiva.admin.repository.managed.ManagedRepository();
        repo.setLocation( managedRepository.getLocation() );
        repo.setBlockRedeployments( managedRepository.isBlockRedeployments() );
        repo.setCronExpression( managedRepository.getCronExpression() );
        repo.setId( managedRepository.getId() );
        repo.setLayout( managedRepository.getLayout() );
        repo.setName( managedRepository.getName() );
        repo.setReleases( managedRepository.isReleases() );
        repo.setSnapshots( managedRepository.isSnapshots() );
        return managedRepositoryAdmin.updateManagedRepository( repo, managedRepository.isStageRepoNeeded(),
                                                               getAuditInformation(), managedRepository.isResetStats() );
    }


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

    //-----------------------------------------------
    // util methods
    // FIXME most are copied from xmlrpc
    // olamt move those in common utility classes
    //-----------------------------------------------

    protected void triggerAuditEvent( String repositoryId, String resource, String action )
    {
        User user = RedbackAuthenticationThreadLocal.get().getUser();
        if ( user == null )
        {
            log.warn( "no user found in Redback ThreadLocal" );
            AuditEvent event =
                new AuditEvent( repositoryId, user == null ? "null" : user.getUsername(), resource, action );
            // FIXME use a thread local through cxf interceptors to store this
            //event.setRemoteIP( getRemoteAddr() );

            for ( AuditListener listener : auditListeners )
            {
                listener.auditEvent( event );
            }
        }
    }

    protected void addRepositoryRoles( ManagedRepositoryConfiguration newRepository )
        throws RoleManagerException
    {
        String repoId = newRepository.getId();

        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic

        if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }
    }

    public Boolean executeRepositoryScanner( String repoId )
        throws Exception
    {
        return scanRepository( repoId, true );
    }

    private void saveConfiguration( Configuration config )
        throws Exception
    {
        try
        {
            archivaConfiguration.save( config );
        }
        catch ( RegistryException e )
        {
            throw new Exception( "Error occurred in the registry." );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new Exception( "Error occurred while saving the configuration." );
        }
    }

    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws IOException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        if ( !file.exists() || !file.isDirectory() )
        {
            throw new IOException(
                "Unable to add repository - no write access, can not create the root directory: " + file );
        }

        configuration.addManagedRepository( repository );
    }

    private ManagedRepositoryConfiguration getStageRepoConfig( ManagedRepositoryConfiguration repository )
    {
        ManagedRepositoryConfiguration stagingRepository = new ManagedRepositoryConfiguration();
        stagingRepository.setId( repository.getId() + "-stage" );
        stagingRepository.setLayout( repository.getLayout() );
        stagingRepository.setName( repository.getName() + "-stage" );
        stagingRepository.setBlockRedeployments( repository.isBlockRedeployments() );
        stagingRepository.setDaysOlder( repository.getDaysOlder() );
        stagingRepository.setDeleteReleasedSnapshots( repository.isDeleteReleasedSnapshots() );
        stagingRepository.setIndexDir( repository.getIndexDir() );
        String path = repository.getLocation();
        int lastIndex = path.lastIndexOf( '/' );
        stagingRepository.setLocation( path.substring( 0, lastIndex ) + "/" + stagingRepository.getId() );
        stagingRepository.setRefreshCronExpression( repository.getRefreshCronExpression() );
        stagingRepository.setReleases( repository.isReleases() );
        stagingRepository.setRetentionCount( repository.getRetentionCount() );
        stagingRepository.setScanned( repository.isScanned() );
        stagingRepository.setSnapshots( repository.isSnapshots() );
        return stagingRepository;
    }


    private String removeExpressions( String directory )
    {
        String value = StringUtils.replace( directory, "${appserver.base}",
                                            registry.getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
                                     registry.getString( "appserver.home", "${appserver.home}" ) );
        return value;
    }


    private AuditInformation getAuditInformation()
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        User user = redbackRequestInformation == null ? null : redbackRequestInformation.getUser();
        String remoteAddr = redbackRequestInformation == null ? null : redbackRequestInformation.getRemoteAddr();
        return new AuditInformation( user, remoteAddr );
    }

}


