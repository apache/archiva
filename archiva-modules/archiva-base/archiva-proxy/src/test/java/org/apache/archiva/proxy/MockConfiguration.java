package org.apache.archiva.proxy;

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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;

/**
 * MockConfiguration
 *
 *
 */
@Service( "archivaConfiguration#mock" )
public class MockConfiguration
    implements ArchivaConfiguration
{

    private Configuration configuration = new Configuration();

    private Set<RegistryListener> registryListeners = new HashSet<RegistryListener>();

    private Set<ConfigurationListener> configListeners = new HashSet<ConfigurationListener>();

    private IMocksControl registryControl;

    private Registry registryMock;

    public MockConfiguration()
    {
        registryControl = EasyMock.createNiceControl( );
        registryMock = registryControl.createMock( Registry.class );
    }

    @PostConstruct
    public void initialize()
        throws Exception
    {

        configuration.setRepositoryScanning( new RepositoryScanningConfiguration()
        {
            @Override
            public List<FileType> getFileTypes()
            {
                FileType fileType = new FileType();
                fileType.setId( FileTypes.ARTIFACTS );
                fileType.setPatterns( Collections.singletonList( "**/*" ) );
                return Collections.singletonList( fileType );
            }
        } );
    }

    @Override
    public void addChangeListener( org.apache.archiva.redback.components.registry.RegistryListener listener )
    {
        registryListeners.add( listener );
    }

    @Override
    public void removeChangeListener( RegistryListener listener )
    {
        registryListeners.remove( listener );
    }

    @Override
    public Configuration getConfiguration()
    {
        return configuration;
    }

    @Override
    public void save( Configuration configuration )
        throws RegistryException
    {
        /* do nothing */
    }

    public void triggerChange( String name, String value )
    {
        for ( org.apache.archiva.redback.components.registry.RegistryListener listener : registryListeners )
        {
            try
            {
                listener.afterConfigurationChange( registryMock, name, value );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addListener( ConfigurationListener listener )
    {
        configListeners.add( listener );
    }

    @Override
    public void removeListener( ConfigurationListener listener )
    {
        configListeners.remove( listener );
    }

    @Override
    public boolean isDefaulted()
    {
        return false;
    }

    @Override
    public void reload()
    {
        // no op
    }
}
