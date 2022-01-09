package org.apache.archiva.repository.base;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.provider.ConfigurationListener;
import org.apache.archiva.configuration.provider.IndeterminateConfigurationException;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is just a simple wrapper to access the archiva configuration used by the registry and associated classes
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("configurationHandler#default")
public class ConfigurationHandler
{
    public static final String REGISTRY_EVENT_TAG = "repositoryRegistry";

    private ArchivaConfiguration archivaConfiguration;

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock( );

    public ConfigurationHandler( ArchivaConfiguration archivaConfiguration ) {
        this.archivaConfiguration = archivaConfiguration;
    }

    public void addListener( ConfigurationListener listener ) {
        this.archivaConfiguration.addListener( listener );
    }

    public ArchivaConfiguration getArchivaConfiguration( )
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Configuration getBaseConfiguration() {
        return archivaConfiguration.getConfiguration( );
    }

    public void save(Configuration configuration, String eventTag) throws IndeterminateConfigurationException, RegistryException
    {
        archivaConfiguration.save( configuration, eventTag);
    }

    public void save(Configuration configuration) throws IndeterminateConfigurationException, RegistryException
    {
        archivaConfiguration.save( configuration, "" );
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }
}
