package org.apache.maven.archiva.proxy;

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
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.registry.RegistryListener;
import org.easymock.MockControl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;

/**
 * MockConfiguration
 *
 * @version $Id$
 *          <p/>
 *          plexus.component role="org.apache.maven.archiva.configuration.ArchivaConfiguration"
 *          role-hint="mock"
 */
@Service( "archivaConfiguration#mock" )
public class MockConfiguration
    implements ArchivaConfiguration
{

    private Configuration configuration = new Configuration();

    private Set<RegistryListener> registryListeners = new HashSet<RegistryListener>();

    private Set<ConfigurationListener> configListeners = new HashSet<ConfigurationListener>();

    private MockControl registryControl;

    private Registry registryMock;

    public MockConfiguration()
    {
        registryControl = MockControl.createNiceControl( Registry.class );
        registryMock = (Registry) registryControl.getMock();
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

    public void addChangeListener( RegistryListener listener )
    {
        registryListeners.add( listener );
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public void save( Configuration configuration )
        throws RegistryException
    {
        /* do nothing */
    }

    public void triggerChange( String name, String value )
    {
        for ( RegistryListener listener : registryListeners )
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

    public void addListener( ConfigurationListener listener )
    {
        configListeners.add( listener );
    }

    public void removeListener( ConfigurationListener listener )
    {
        configListeners.remove( listener );
    }

    public boolean isDefaulted()
    {
        return false;
    }

    public void reload()
    {
        // no op
    }
}
