package org.apache.maven.repository.configuration;

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
 * Component capable of noticing configuration changes and adjusting accordingly.
 * This is not a Plexus role.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ConfigurationChangeListener
{
    /**
     * Notify the object that there has been a configuration change.
     *
     * @param configuration the new configuration
     * @throws InvalidConfigurationException if there is a problem with the new configuration
     * @throws ConfigurationChangeException  if there is a problem changing the configuration, but the configuration is valid
     */
    void notifyOfConfigurationChange( Configuration configuration )
        throws InvalidConfigurationException, ConfigurationChangeException;
}
