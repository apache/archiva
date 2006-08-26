package org.apache.maven.archiva.configuration;

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

/**
 * A component for loading the configuration into the model.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo this is something that could possibly be generalised into Modello.
 */
public interface ConfigurationStore
{
    /**
     * The Plexus role for the component.
     */
    String ROLE = ConfigurationStore.class.getName();

    /**
     * Get the configuration from the store. A cached version may be used.
     *
     * @return the configuration
     * @throws ConfigurationStoreException if there is a problem loading the configuration
     */
    Configuration getConfigurationFromStore()
        throws ConfigurationStoreException;

    /**
     * Save the configuration to the store.
     *
     * @param configuration the configuration to store
     */
    void storeConfiguration( Configuration configuration )
        throws ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException;

    /**
     * Add a configuration change listener.
     *
     * @param listener the listener
     */
    void addChangeListener( ConfigurationChangeListener listener );
}
