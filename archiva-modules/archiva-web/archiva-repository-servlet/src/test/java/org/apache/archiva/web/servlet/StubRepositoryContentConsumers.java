package org.apache.archiva.web.servlet;

import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;

import java.util.List;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;

public class StubRepositoryContentConsumers
    extends RepositoryContentConsumers
{
    public StubRepositoryContentConsumers(ArchivaConfiguration configuration)
    {
        super(configuration);
    }

    @Override
    public List<KnownRepositoryContentConsumer> getSelectedKnownConsumers()
    {
        return getAvailableKnownConsumers();
    }

    @Override
    public synchronized List<InvalidRepositoryContentConsumer> getSelectedInvalidConsumers()
    {
        return getAvailableInvalidConsumers();
    }
}
