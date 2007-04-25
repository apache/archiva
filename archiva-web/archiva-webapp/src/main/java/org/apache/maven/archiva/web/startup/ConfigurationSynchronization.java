package org.apache.maven.archiva.web.startup;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.Iterator;
import java.util.List;

/**
 * ConfigurationSynchronization 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.web.startup.ConfigurationSynchronization"
 *                   role-hint="default"
 */
public class ConfigurationSynchronization
    extends AbstractLogEnabled
    implements RegistryListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            synchConfiguration();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void synchConfiguration()
    {
        List repos = archivaConfiguration.getConfiguration().getRepositories();
        Iterator it = repos.iterator();
        while ( it.hasNext() )
        {
            RepositoryConfiguration repoConfig = (RepositoryConfiguration) it.next();
            try
            {
                try
                {
                    ArchivaRepository repository = dao.getRepositoryDAO().getRepository( repoConfig.getId() );
                    // Found repository.  Update it.

                    repository.getModel().setName( repoConfig.getName() );
                    repository.getModel().setUrl( repoConfig.getUrl() );
                    repository.getModel().setLayoutName( repoConfig.getLayout() );
                    repository.getModel().setCreationSource( "configuration" );
                    repository.getModel().setReleasePolicy( repoConfig.isReleases() );
                    repository.getModel().setSnapshotPolicy( repoConfig.isSnapshots() );

                    dao.getRepositoryDAO().saveRepository( repository );
                }
                catch ( ObjectNotFoundException e )
                {
                    // Add the repository to the database.
                    getLogger().info( "Adding repository configuration to DB: " + repoConfig );
                    ArchivaRepository drepo = ArchivaConfigurationAdaptor.toArchivaRepository( repoConfig );
                    drepo.getModel().setCreationSource( "configuration" );
                    dao.getRepositoryDAO().saveRepository( drepo );
                }
            }
            catch ( ArchivaDatabaseException e )
            {
                // Log error.
                getLogger().error( "Unable to add configured repositories to the database: " + e.getMessage(), e );
            }
        }
    }

    public void initialize()
        throws InitializationException
    {
        Banner.display( getLogger(), ArchivaVersion.determineVersion( this.getClass().getClassLoader() ) );
        synchConfiguration();
    }
}
