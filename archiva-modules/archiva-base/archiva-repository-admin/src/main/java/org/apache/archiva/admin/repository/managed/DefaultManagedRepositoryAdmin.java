package org.apache.archiva.admin.repository.managed;
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
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
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
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.redback.components.scheduler.CronExpressionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * FIXME remove all generic Exception to have usefull ones
 * FIXME review the staging mechanism to have a per user session one
 *
 * @author Olivier Lamy
 */
@Service( "managedRepositoryAdmin#default" )
public class DefaultManagedRepositoryAdmin
    implements ManagedRepositoryAdmin
{
    public static final String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";

    public static final String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";

    public static final String REPOSITORY_LOCATION_VALID_EXPRESSION = "^[-a-zA-Z0-9._/~:?!&amp;=\\\\]+$";

    private Logger log = LoggerFactory.getLogger( getClass() );

    public static final String STAGE_REPO_ID_END = "-stage";

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    @Inject
    protected RoleManager roleManager;

    public List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {
        List<ManagedRepositoryConfiguration> managedRepoConfigs =
            archivaConfiguration.getConfiguration().getManagedRepositories();

        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>( managedRepoConfigs.size() );

        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            // TODO add staging repo information back too
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                       repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases(),
                                       repoConfig.isBlockRedeployments(), repoConfig.getRefreshCronExpression() );

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

    public Boolean addManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                         AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        triggerAuditEvent( managedRepository.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
        return
            addManagedRepository( managedRepository.getId(), managedRepository.getLayout(), managedRepository.getName(),
                                  managedRepository.getLocation(), managedRepository.isBlockRedeployments(),
                                  managedRepository.isReleases(), managedRepository.isSnapshots(), needStageRepo,
                                  managedRepository.getCronExpression(), auditInformation ) != null;

    }

    private ManagedRepositoryConfiguration addManagedRepository( String repoId, String layout, String name,
                                                                 String location, boolean blockRedeployments,
                                                                 boolean releasesIncluded, boolean snapshotsIncluded,
                                                                 boolean stageRepoNeeded, String cronExpression,
                                                                 AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        Configuration config = archivaConfiguration.getConfiguration();

        if ( config.getManagedRepositoriesAsMap().containsKey( repoId ) )
        {
            throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                                                    + "], that id already exists as a managed repository." );
        }
        else if ( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
        {
            throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                                                    + "], that id already exists as a repository group." );
        }

        // FIXME : olamy can be empty to avoid scheduled scan ?
        if ( StringUtils.isNotBlank( cronExpression ) )
        {
            CronExpressionValidator validator = new CronExpressionValidator();

            if ( !validator.validate( cronExpression ) )
            {
                throw new RepositoryAdminException( "Invalid cron expression." );
            }
        }
        else
        {
            throw new RepositoryAdminException( "Cron expression cannot be empty." );
        }

        if (StringUtils.isBlank( repoId  ))
        {
            throw new RepositoryAdminException( "Repository ID cannot be empty." );
        }

        if ( !GenericValidator.matchRegexp( repoId, REPOSITORY_ID_VALID_EXPRESSION ) )
        {
            throw new RepositoryAdminException(
                "Invalid repository ID. Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        }

        if ( StringUtils.isBlank( name ) )
        {
            throw new RepositoryAdminException( "repository name cannot be empty" );
        }

        if ( !GenericValidator.matchRegexp( name, REPOSITORY_NAME_VALID_EXPRESSION ) )
        {
            throw new RepositoryAdminException(
                "Invalid repository name. Repository Name must only contain alphanumeric characters, white-spaces(' '), "
                    + "forward-slashes(/), open-parenthesis('('), close-parenthesis(')'),  underscores(_), dots(.), and dashes(-)." );
        }

        String repoLocation = removeExpressions( location );

        if ( !GenericValidator.matchRegexp( repoLocation, REPOSITORY_LOCATION_VALID_EXPRESSION ) )
        {
            throw new RepositoryAdminException(
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
        try
        {
            addRepository( repository, config );
            addRepositoryRoles( repository );

            if ( stageRepoNeeded )
            {
                ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
                addRepository( stagingRepository, config );
                addRepositoryRoles( stagingRepository );
                triggerAuditEvent( stagingRepository.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
            }
        }
        catch ( RoleManagerException e )
        {
            throw new RepositoryAdminException( "failed to add repository roles " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( "failed to add repository " + e.getMessage(), e );
        }

        saveConfiguration( config );

        //MRM-1342 Repository statistics report doesn't appear to be working correctly
        //scan repository when adding of repository is successful
        try
        {
            scanRepository( repoId, true );
            // olamy no need of scanning staged repo
            /*
            if ( stageRepoNeeded )
            {
                ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
                scanRepository( stagingRepository.getId(), true );
            }*/
        }
        catch ( Exception e )
        {
            log.warn( new StringBuilder( "Unable to scan repository [" ).append( repoId ).append( "]: " ).append(
                e.getMessage() ).toString(), e );
        }

        return repository;
    }


    public Boolean deleteManagedRepository( String repositoryId, AuditInformation auditInformation,
                                            boolean deleteContent )
        throws RepositoryAdminException
    {
        Configuration config = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration repository = config.findManagedRepositoryById( repositoryId );

        if ( repository == null )
        {
            throw new RepositoryAdminException( "A repository with that id does not exist" );
        }

        triggerAuditEvent( repositoryId, null, AuditEvent.DELETE_MANAGED_REPO, auditInformation );

        deleteManagedRepository( repository, deleteContent, config, false );

        // stage repo exists ?
        ManagedRepositoryConfiguration stagingRepository =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repositoryId + STAGE_REPO_ID_END );
        if ( stagingRepository != null )
        {
            // do not trigger event when deleting the staged one
            //triggerAuditEvent( stagingRepository.getId(), null, AuditEvent.DELETE_MANAGED_REPO, auditInformation );
            deleteManagedRepository( stagingRepository, deleteContent, config, true );
        }

        try
        {
            saveConfiguration( config );
        }
        catch ( Exception e )
        {
            throw new RepositoryAdminException( "Error saving configuration for delete action" + e.getMessage() );
        }

        return Boolean.TRUE;
    }

    private Boolean deleteManagedRepository( ManagedRepositoryConfiguration repository, boolean deleteContent,
                                             Configuration config, boolean stagedOne )
        throws RepositoryAdminException
    {
        if ( !stagedOne )
        {
            RepositorySession repositorySession = repositorySessionFactory.createSession();
            try
            {
                MetadataRepository metadataRepository = repositorySession.getRepository();
                metadataRepository.removeRepository( repository.getId() );
                log.debug( "call repositoryStatisticsManager.deleteStatistics" );
                repositoryStatisticsManager.deleteStatistics( metadataRepository, repository.getId() );
                repositorySession.save();
            }
            catch ( MetadataRepositoryException e )
            {
                throw new RepositoryAdminException( e.getMessage(), e );
            }
            finally
            {
                repositorySession.close();
            }
        }
        config.removeManagedRepository( repository );

        if ( deleteContent )
        {
            // TODO could be async ? as directory can be huge
            File dir = new File( repository.getLocation() );
            if ( !FileUtils.deleteQuietly( dir ) )
            {
                throw new RepositoryAdminException( "Cannot delete repository " + dir );
            }
        }

        // olamy: copy list for reading as a unit test in webapp fail with ConcurrentModificationException
        List<ProxyConnectorConfiguration> proxyConnectors =
            new ArrayList<ProxyConnectorConfiguration>( config.getProxyConnectors() );
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), repository.getId() ) )
            {
                archivaConfiguration.getConfiguration().removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = config.getRepositoryToGroupMap();
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

        try
        {
            removeRepositoryRoles( repository );
        }
        catch ( RoleManagerException e )
        {
            throw new RepositoryAdminException(
                "fail to remove repository roles for repository " + repository.getId() + " : " + e.getMessage(), e );
        }
        return Boolean.TRUE;
    }


    public Boolean updateManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                            AuditInformation auditInformation, boolean resetStats )
        throws RepositoryAdminException
    {
        // Ensure that the fields are valid.
        Configuration configuration = archivaConfiguration.getConfiguration();

        log.debug( "updateManagedConfiguration repo {} needStage {} resetStats {} ",
                   Arrays.asList( managedRepository, needStageRepo, resetStats ).toArray() );

        ManagedRepositoryConfiguration toremove = configuration.findManagedRepositoryById( managedRepository.getId() );

        if ( toremove != null )
        {
            configuration.removeManagedRepository( toremove );
        }

        ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( toremove );

        // TODO remove content from old if path has changed !!!!!

        if ( stagingRepository != null )
        {
            configuration.removeManagedRepository( stagingRepository );
        }

        if ( toremove != null && stagingRepository != null )
        {
            saveConfiguration( configuration );
        }

        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            addManagedRepository( managedRepository.getId(), managedRepository.getLayout(), managedRepository.getName(),
                                  managedRepository.getLocation(), managedRepository.isBlockRedeployments(),
                                  managedRepository.isReleases(), managedRepository.isSnapshots(), needStageRepo,
                                  managedRepository.getCronExpression(), auditInformation );

        // Save the repository configuration.
        RepositorySession repositorySession = repositorySessionFactory.createSession();

        try
        {
            triggerAuditEvent( managedRepositoryConfiguration.getId(), null, AuditEvent.MODIFY_MANAGED_REPO,
                               auditInformation );

            saveConfiguration( this.archivaConfiguration.getConfiguration() );
            if ( resetStats )
            {
                log.debug( "call repositoryStatisticsManager.deleteStatistics" );
                repositoryStatisticsManager.deleteStatistics( repositorySession.getRepository(),
                                                              managedRepositoryConfiguration.getId() );
                repositorySession.save();
            }

        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }

        return true;
    }

    //--------------------------
    // utils methods
    //--------------------------

    protected void triggerAuditEvent( String repositoryId, String resource, String action,
                                      AuditInformation auditInformation )
    {
        User user = auditInformation == null ? null : auditInformation.getUser();
        AuditEvent event =
            new AuditEvent( repositoryId, user == null ? "null" : (String) user.getPrincipal(), resource, action );
        event.setRemoteIP( auditInformation == null ? "null" : auditInformation.getRemoteAddr() );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }

    }

    public String removeExpressions( String directory )
    {
        String value = StringUtils.replace( directory, "${appserver.base}",
                                            registry.getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
                                     registry.getString( "appserver.home", "${appserver.home}" ) );
        return value;
    }

    private void saveConfiguration( Configuration config )
        throws RepositoryAdminException
    {
        try
        {
            archivaConfiguration.save( config );
        }
        catch ( RegistryException e )
        {
            throw new RepositoryAdminException( "Error occurred in the registry.", e );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new RepositoryAdminException( "Error occurred while saving the configuration.", e );
        }
    }

    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws RepositoryAdminException, IOException
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
            throw new RepositoryAdminException(
                "Unable to add repository - no write access, can not create the root directory: " + file );
        }

        configuration.addManagedRepository( repository );
    }

    private ManagedRepositoryConfiguration getStageRepoConfig( ManagedRepositoryConfiguration repository )
    {
        ManagedRepositoryConfiguration stagingRepository = new ManagedRepositoryConfiguration();
        stagingRepository.setId( repository.getId() + STAGE_REPO_ID_END );
        stagingRepository.setLayout( repository.getLayout() );
        stagingRepository.setName( repository.getName() + STAGE_REPO_ID_END );
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

    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled", repositoryId );
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

    protected void removeRepositoryRoles( ManagedRepositoryConfiguration existingRepository )
        throws RoleManagerException
    {
        String repoId = existingRepository.getId();

        if ( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }

        if ( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        log.debug( "removed user roles associated with repository {}", repoId );
    }

    //--------------------------
    // setters/getters
    //--------------------------

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public RoleManager getRoleManager()
    {
        return roleManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }

    public RepositoryStatisticsManager getRepositoryStatisticsManager()
    {
        return repositoryStatisticsManager;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }

    public RepositorySessionFactory getRepositorySessionFactory()
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }

    public List<AuditListener> getAuditListeners()
    {
        return auditListeners;
    }

    public void setAuditListeners( List<AuditListener> auditListeners )
    {
        this.auditListeners = auditListeners;
    }

    public RepositoryArchivaTaskScheduler getRepositoryTaskScheduler()
    {
        return repositoryTaskScheduler;
    }

    public void setRepositoryTaskScheduler( RepositoryArchivaTaskScheduler repositoryTaskScheduler )
    {
        this.repositoryTaskScheduler = repositoryTaskScheduler;
    }

    public Registry getRegistry()
    {
        return registry;
    }

    public void setRegistry( Registry registry )
    {
        this.registry = registry;
    }
}
