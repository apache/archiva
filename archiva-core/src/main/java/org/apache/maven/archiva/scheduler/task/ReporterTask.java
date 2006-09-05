package org.apache.maven.archiva.scheduler.task;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.scheduler.TaskExecutionException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Map;

/**
 * Task for discovering problems in the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.archiva.scheduler.task.RepositoryTask" role-hint="reporter"
 */
public class ReporterTask
    extends AbstractLogEnabled
    implements RepositoryTask
{
    /**
     * Configuration store.
     *
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repoFactory;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    public void execute()
        throws TaskExecutionException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }

        execute( configuration );
    }

    private void execute( Configuration configuration )
        throws TaskExecutionException
    {
        long time = System.currentTimeMillis();
        getLogger().info( "Starting repository reporting process" );

        // TODO!

        time = System.currentTimeMillis() - time;
        getLogger().info( "Finished repository reporting process in " + time + "ms" );
    }

    public void executeNowIfNeeded()
        throws TaskExecutionException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }

        // TODO!
    }
}
