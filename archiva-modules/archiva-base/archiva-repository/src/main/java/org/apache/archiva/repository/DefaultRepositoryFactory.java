package org.apache.archiva.repository;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.archiva.repository.api.Repository;
import org.apache.archiva.repository.api.RepositoryFactory;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

public class DefaultRepositoryFactory implements RepositoryFactory
{
    private final ArchivaConfiguration archivaConfiguration;

    public DefaultRepositoryFactory(ArchivaConfiguration archivaConfiguration)
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Map<String, Repository> getRepositories()
    {
        final HashMap<String, Repository> repositories = new HashMap<String, Repository>();
        for (ManagedRepositoryConfiguration configuration : archivaConfiguration.getConfiguration().getManagedRepositories())
        {
            repositories.put(configuration.getId(), new DefaultRepository(configuration.getId(), configuration.getName(), new File(configuration.getLocation())));
        }
        return repositories;
    }
}
