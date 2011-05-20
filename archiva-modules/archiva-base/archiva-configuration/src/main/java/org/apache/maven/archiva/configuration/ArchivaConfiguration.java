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

import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.registry.RegistryListener;

/**
 * Configuration holder for the model read from the registry.
 */
public interface ArchivaConfiguration
{
    String ROLE = ArchivaConfiguration.class.getName();

    /**
     * Get the configuration.
     *
     * @return the configuration
     */
    Configuration getConfiguration();

    /**
     * Save any updated configuration.
     *
     * @param configuration the configuration to save
     * @throws org.codehaus.plexus.registry.RegistryException
     *          if there is a problem saving the registry data
     * @throws IndeterminateConfigurationException
     *          if the configuration cannot be saved because it was read from two sources
     */
    void save( Configuration configuration )
        throws RegistryException, IndeterminateConfigurationException;
    
    /**
     * Determines if the configuration in use was as a result of a defaulted configuration.
     * 
     * @return true if the configuration was created from the default-archiva.xml as opposed
     *              to being loaded from the usual locations of ${user.home}/.m2/archiva.xml or
     *              ${appserver.base}/conf/archiva.xml
     */
    boolean isDefaulted();

    /**
     * Add a configuration listener to notify of changes to the configuration.
     *
     * @param listener the listener
     */
    void addListener( ConfigurationListener listener );
    
    /**
     * Remove a configuration listener to stop notifications of changes to the configuration.
     *
     * @param listener the listener
     */
    void removeListener( ConfigurationListener listener );
    
    /**
     * Add a registry listener to notify of events in plexus-registry.
     *
     * @param listener the listener
     * TODO: Remove in future.
     */
    void addChangeListener( RegistryListener listener );

    /**
     * reload configuration from file included registry
     * @since 1.4-M1
     */
    void reload();
}

