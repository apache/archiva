package org.apache.archiva.rest.services;

import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
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

    @Inject
    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    public List<ManagedRepository> getManagedRepositories()
    {
        List<ManagedRepositoryConfiguration> managedRepoConfigs =
            archivaConfiguration.getConfiguration().getManagedRepositories();

        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>( managedRepoConfigs.size() );

        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            // TODO fix resolution of repo url!
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), "URL", repoConfig.getLayout(),
                                       repoConfig.isSnapshots(), repoConfig.isReleases() );
            managedRepos.add( repo );
        }

        return managedRepos;
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
        }
        return true;
    }
}
