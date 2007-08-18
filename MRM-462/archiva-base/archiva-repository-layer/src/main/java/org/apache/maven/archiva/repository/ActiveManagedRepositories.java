package org.apache.maven.archiva.repository;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.configuration.functors.LocalRepositoryPredicate;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A component that provides a real-time listing of the active managed repositories within archiva.
 * This object is internally consistent and will return maintain a consistent list of managed repositories internally.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.ActiveManagedRepositories"
 */
public class ActiveManagedRepositories
    implements RegistryListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private List allManagedRepositories = new ArrayList();

    /**
     * Get the {@link List} of {@link RepositoryConfiguration} objects representing managed repositories.
     * 
     * @return the {@link List} of {@link RepositoryConfiguration} objects.
     */
    public List getAllManagedRepositories()
    {
        synchronized ( allManagedRepositories )
        {
            return Collections.unmodifiableList( allManagedRepositories );
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            update();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* nothing to do here */
    }

    public void initialize()
        throws InitializationException
    {
        update();
        archivaConfiguration.addChangeListener( this );
    }

    private void update()
    {
        synchronized ( allManagedRepositories )
        {
            allManagedRepositories.clear();

            List configRepos = archivaConfiguration.getConfiguration().getRepositories();
            CollectionUtils.filter( configRepos, LocalRepositoryPredicate.getInstance() );
        }
    }
}
