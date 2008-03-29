package org.apache.maven.archiva.web.repository;

import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;

import java.util.List;

public class StubRepositoryContentConsumers
    extends RepositoryContentConsumers
{
    public List<KnownRepositoryContentConsumer> getSelectedKnownConsumers()
    {
        return getAvailableKnownConsumers();
    }

    public synchronized List<InvalidRepositoryContentConsumer> getSelectedInvalidConsumers()
    {
        return getAvailableInvalidConsumers();
    }
}
