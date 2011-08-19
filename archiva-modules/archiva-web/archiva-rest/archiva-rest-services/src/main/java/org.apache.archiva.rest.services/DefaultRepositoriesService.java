package org.apache.archiva.rest.services;

import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
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

    @Inject
    protected ArchivaConfiguration archivaConfiguration;

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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
