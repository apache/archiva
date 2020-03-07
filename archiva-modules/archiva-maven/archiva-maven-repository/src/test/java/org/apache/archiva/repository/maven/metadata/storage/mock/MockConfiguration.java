package org.apache.archiva.repository.maven.metadata.storage.mock;

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
import org.apache.archiva.configuration.ArchivaRuntimeConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.easymock.IMocksControl;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.easymock.EasyMock.createNiceControl;

/**
 * MockConfiguration 
 *
 *
 */
@Service("archivaConfiguration#mock")
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
        registryControl = createNiceControl();
        registryMock = registryControl.createMock( Registry.class );
        configuration.setArchivaRuntimeConfiguration(new ArchivaRuntimeConfiguration());
        configuration.getArchivaRuntimeConfiguration().addChecksumType("sha1");
        configuration.getArchivaRuntimeConfiguration().addChecksumType("sha256");
        configuration.getArchivaRuntimeConfiguration().addChecksumType("md5");
        RepositoryScanningConfiguration rpsc = new RepositoryScanningConfiguration( );
        FileType ft = new FileType( );
        ft.setId( "artifacts" );
        ArrayList<String> plist = new ArrayList<>( );
        plist.add( "**/*.jar" );
        plist.add( "**/*.pom" );
        plist.add( "**/*.war" );
        ft.setPatterns( plist );
        rpsc.addFileType( ft  );
        ArrayList<FileType> ftList = new ArrayList<>( );
        ftList.add( ft );
        rpsc.setFileTypes( ftList );
        configuration.setRepositoryScanning( rpsc );
    }

    @Override
    public void addChangeListener( RegistryListener listener )
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
        for(RegistryListener listener: registryListeners)
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
        configListeners.add(listener);
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

    @Override
    public Locale getDefaultLocale( )
    {
        return Locale.getDefault();
    }

    @Override
    public List<Locale.LanguageRange> getLanguagePriorities( )
    {
        return Locale.LanguageRange.parse( "en,fr,de" );
    }

    @Override
    public Path getAppServerBaseDir() {
        if (System.getProperties().containsKey("appserver.base")) {
            return Paths.get(System.getProperty("appserver.base"));
        } else {
            return Paths.get("");
        }
    }


    @Override
    public Path getRepositoryBaseDir() {
        return getDataDirectory().resolve("repositories");
    }

    @Override
    public Path getRemoteRepositoryBaseDir() {
        return getDataDirectory().resolve("remotes");
    }

    @Override
    public Path getRepositoryGroupBaseDir() {
        return getDataDirectory().resolve("groups");
    }

    @Override
    public Path getDataDirectory() {
        if (configuration!=null && StringUtils.isNotEmpty(configuration.getArchivaRuntimeConfiguration().getDataDirectory())) {
            return Paths.get(configuration.getArchivaRuntimeConfiguration().getDataDirectory());
        } else {
            return getAppServerBaseDir().resolve("data");
        }
    }




}
