package org.apache.archiva.configuration;

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

import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.springframework.stereotype.Service;

@Service("archivaConfiguration#mocked")
public class StubConfiguration
    implements ArchivaConfiguration
{
    private Configuration configuration;

    @Override
    public Configuration getConfiguration()
    {
        return configuration;
    }

    @Override
    public void save( Configuration configuration )
        throws RegistryException, IndeterminateConfigurationException
    {
        this.configuration = configuration;
    }

    @Override
    public boolean isDefaulted()
    {
        return false;
    }

    @Override
    public void addListener( ConfigurationListener listener )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener( ConfigurationListener listener )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChangeListener( RegistryListener listener )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChangeListener( RegistryListener listener )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reload()
    {
        // no op
    }
}
