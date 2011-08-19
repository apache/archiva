package org.apache.archiva.rest.services;

import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.model.RemoteRepository;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service("repositoriesService#rest")
public class DefaultRepositoriesService
    implements RepositoriesService
{
    public List<ManagedRepository> getManagedRepositories()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<RemoteRepository> getRemoteRepositories()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
