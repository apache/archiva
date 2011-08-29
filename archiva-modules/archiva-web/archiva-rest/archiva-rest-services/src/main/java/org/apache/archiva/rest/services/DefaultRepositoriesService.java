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

import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.redback.components.scheduler.CronExpressionValidator;
import org.codehaus.redback.rest.services.RedbackAuthenticationThreadLocal;
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
import java.util.Map;

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

    public List<ManagedRepository> getManagedRepositories()
    {
        List<ManagedRepositoryConfiguration> managedRepoConfigs =
            archivaConfiguration.getConfiguration().getManagedRepositories();

        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>( managedRepoConfigs.size() );

        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            // TODO staging repo too
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                       repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases(),
                                       repoConfig.isBlockRedeployments(), false,
                                       repoConfig.getRefreshCronExpression() );
            managedRepos.add( repo );
        }

        return managedRepos;
    }

    public ManagedRepository getManagedRepository( String repositoryId )
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

    // FIXME duplicate of xml rpc
    // move this in a common place archiva commons remote service
    public Boolean deleteManagedRepository( String repoId )
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration repository = config.findManagedRepositoryById( repoId );

        if ( repository == null )
        {
            throw new Exception( "A repository with that id does not exist" );
        }

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            metadataRepository.removeRepository( repository.getId() );
            repositoryStatisticsManager.deleteStatistics( metadataRepository, repository.getId() );
            repositorySession.save();
        }
        finally
        {
            repositorySession.close();
        }
        config.removeManagedRepository( repository );

        try
        {
            saveConfiguration( config );
        }
        catch ( Exception e )
        {
            throw new Exception( "Error saving configuration for delete action" + e.getMessage() );
        }

        // TODO could be async ? as directory can be huge
        File dir = new File( repository.getLocation() );
        if ( !FileUtils.deleteQuietly( dir ) )
        {
            throw new IOException( "Cannot delete repository " + dir );
        }

        List<ProxyConnectorConfiguration> proxyConnectors = config.getProxyConnectors();
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), repository.getId() ) )
            {
                archivaConfiguration.getConfiguration().removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = archivaConfiguration.getConfiguration().getRepositoryToGroupMap();
        if ( repoToGroupMap != null )
        {
            if ( repoToGroupMap.containsKey( repository.getId() ) )
            {
                List<String> repoGroups = repoToGroupMap.get( repository.getId() );
                for ( String repoGroup : repoGroups )
                {
                    archivaConfiguration.getConfiguration().findRepositoryGroupById( repoGroup ).removeRepository(
                        repository.getId() );
                }
            }
        }

        return Boolean.TRUE;
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
        return
            addManagedRepository( managedRepository.getId(), managedRepository.getLayout(), managedRepository.getName(),
                                  managedRepository.getLocation(), managedRepository.isBlockRedeployments(),
                                  managedRepository.isReleases(), managedRepository.isSnapshots(),
                                  managedRepository.isStageRepoNeeded(), managedRepository.getCronExpression() )
                != null;
    }

    private ManagedRepositoryConfiguration addManagedRepository( String repoId, String layout, String name,
                                                                 String location, boolean blockRedeployments,
                                                                 boolean releasesIncluded, boolean snapshotsIncluded,
                                                                 boolean stageRepoNeeded, String cronExpression )
        throws Exception
    {

        Configuration config = archivaConfiguration.getConfiguration();

        CronExpressionValidator validator = new CronExpressionValidator();

        if ( config.getManagedRepositoriesAsMap().containsKey( repoId ) )
        {
            throw new Exception( "Unable to add new repository with id [" + repoId
                                     + "], that id already exists as a managed repository." );
        }
        else if ( config.getRemoteRepositoriesAsMap().containsKey( repoId ) )
        {
            throw new Exception( "Unable to add new repository with id [" + repoId
                                     + "], that id already exists as a remote repository." );
        }
        else if ( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
        {
            throw new Exception( "Unable to add new repository with id [" + repoId
                                     + "], that id already exists as a repository group." );
        }

        if ( !validator.validate( cronExpression ) )
        {
            throw new Exception( "Invalid cron expression." );
        }

        if ( !GenericValidator.matchRegexp( repoId, REPOSITORY_ID_VALID_EXPRESSION ) )
        {
            throw new Exception(
                "Invalid repository ID. Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        }

        if ( !GenericValidator.matchRegexp( name, REPOSITORY_NAME_VALID_EXPRESSION ) )
        {
            throw new Exception(
                "Invalid repository name. Repository Name must only contain alphanumeric characters, white-spaces(' '), "
                    + "forward-slashes(/), open-parenthesis('('), close-parenthesis(')'),  underscores(_), dots(.), and dashes(-)." );
        }

        String repoLocation = removeExpressions( location );

        if ( !GenericValidator.matchRegexp( repoLocation, REPOSITORY_LOCATION_VALID_EXPRESSION ) )
        {
            throw new Exception(
                "Invalid repository location. Directory must only contain alphanumeric characters, equals(=), question-marks(?), "
                    + "exclamation-points(!), ampersands(&amp;), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        }

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();

        repository.setId( repoId );
        repository.setBlockRedeployments( blockRedeployments );
        repository.setReleases( releasesIncluded );
        repository.setSnapshots( snapshotsIncluded );
        repository.setName( name );
        repository.setLocation( repoLocation );
        repository.setLayout( layout );
        repository.setRefreshCronExpression( cronExpression );

        addRepository( repository, config );

        if ( stageRepoNeeded )
        {
            ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
            addRepository( stagingRepository, config );
        }

        saveConfiguration( config );

        //MRM-1342 Repository statistics report doesn't appear to be working correctly
        //scan repository when adding of repository is successful
        try
        {
            executeRepositoryScanner( repoId );
            if ( stageRepoNeeded )
            {
                ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
                executeRepositoryScanner( stagingRepository.getId() );
            }
        }
        catch ( Exception e )
        {
            log.warn( new StringBuilder( "Unable to scan repository [" ).append( repoId ).append( "]: " ).append(
                e.getMessage() ).toString(), e );
        }

        return repository;
    }

    public Boolean updateManagedRepository( ManagedRepository repository )
        throws Exception
    {
        // Ensure that the fields are valid.
        Configuration configuration = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration toremove = configuration.findManagedRepositoryById( repository.getId() );

        if ( toremove != null )
        {
            configuration.removeManagedRepository( toremove );
        }
        // FIXME the case of the attached staging repository
        /*
        if ( stagingRepository != null )
        {
            removeRepository( stagingRepository.getId(), configuration );
        }*/

        // Save the repository configuration.
        String result;
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            addManagedRepository( repository.getId(), repository.getLayout(), repository.getName(), repository.getLocation(),
                                  repository.isBlockRedeployments(), repository.isReleases(), repository.isSnapshots(),
                                  repository.isStageRepoNeeded(), repository.getCronExpression() );

        // FIXME only location has changed from previous
        boolean resetStats = true;

        try
        {
            triggerAuditEvent( repository.getId(), null, AuditEvent.MODIFY_MANAGED_REPO );
            addRepositoryRoles( managedRepositoryConfiguration );

            // FIXME this staging part !!

            //update changes of the staging repo
            /*if ( stageNeeded )
            {

                stagingRepository = getStageRepoConfig( configuration );
                addRepository( stagingRepository, configuration );
                addRepositoryRoles( stagingRepository );

            }*/
            //delete staging repo when we dont need it
            /*
            if ( !stageNeeded )
            {
                stagingRepository = getStageRepoConfig( configuration );
                removeRepository( stagingRepository.getId(), configuration );
                removeContents( stagingRepository );
                removeRepositoryRoles( stagingRepository );
            }*/

            saveConfiguration( this.archivaConfiguration.getConfiguration() );
            if ( resetStats )
            {
                repositoryStatisticsManager.deleteStatistics( repositorySession.getRepository(), repository.getId() );
                repositorySession.save();
            }

            //MRM-1342 Repository statistics report doesn't appear to be working correctly
            //scan repository when modification of repository is successful
            // olamy :  IMHO we are fine to ignore issue with scheduling scanning
            // as here the repo has been updated
            scanRepository( repository.getId(), true );
            // FIXME staging !!
            /*
            if ( stageNeeded )
            {
                executeRepositoryScanner( stagingRepository.getId() );
            }*/

        }
        catch ( IOException e )
        {
            throw e;
        }
        catch ( RoleManagerException e )
        {
            throw e;
        }
        catch ( MetadataRepositoryException e )
        {
            throw e;
        }
        finally
        {
            repositorySession.close();
        }

        return true;
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
        User user = RedbackAuthenticationThreadLocal.get();
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

}


