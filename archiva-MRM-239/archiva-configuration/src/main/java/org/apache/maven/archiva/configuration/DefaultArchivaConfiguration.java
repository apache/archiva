package org.apache.maven.archiva.configuration;

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

import org.apache.maven.archiva.configuration.io.registry.ConfigurationRegistryReader;
import org.apache.maven.archiva.configuration.io.registry.ConfigurationRegistryWriter;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.registry.RegistryListener;

/**
 * Implementation of configuration holder that retrieves it from the registry.
 *
 * @plexus.component
 */
public class DefaultArchivaConfiguration
    implements ArchivaConfiguration, RegistryListener, Initializable
{
    /**
     * Plexus registry to read the configuration from.
     *
     * @plexus.requirement role-hint="commons-configuration"
     */
    private Registry registry;

    /**
     * The configuration that has been converted.
     */
    private Configuration configuration;

    private static final String KEY = "org.apache.maven.archiva";

    public synchronized Configuration getConfiguration()
    {
        if ( configuration == null )
        {
            // TODO: should this be the same as section? make sure unnamed sections still work (eg, sys properties)
            configuration = new ConfigurationRegistryReader().read( registry.getSubset( KEY ) );
        }
        return configuration;
    }

    public void save( Configuration configuration )
        throws RegistryException
    {
        Registry section = registry.getSection( KEY );
        new ConfigurationRegistryWriter().write( configuration, section );
        section.save();
    }

    public void addChangeListener( RegistryListener listener )
    {
        Registry section = registry.getSection( KEY );
        section.addChangeListener( listener );
    }

    public void initialize()
        throws InitializationException
    {
        registry.addChangeListener( this );
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // nothing to do here
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        configuration = null;
    }
}
