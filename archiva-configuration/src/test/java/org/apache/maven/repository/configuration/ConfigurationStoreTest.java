package org.apache.maven.repository.configuration;

import org.codehaus.plexus.PlexusTestCase;
import org.easymock.MockControl;

import java.io.File;
import java.util.Properties;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Test the configuration store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @noinspection JavaDoc
 */
public class ConfigurationStoreTest
    extends PlexusTestCase
{
    public void testInvalidFile()
        throws Exception
    {
        ConfigurationStore configurationStore = (ConfigurationStore) lookup( ConfigurationStore.ROLE, "invalid-file" );

        Configuration configuration = configurationStore.getConfigurationFromStore();

        // check default configuration
        assertNotNull( "check configuration returned", configuration );
        assertEquals( "check configuration has default elements", "0 0 * * * ?",
                      configuration.getIndexerCronExpression() );
        assertNull( "check configuration has default elements", configuration.getIndexPath() );
        assertTrue( "check configuration has default elements", configuration.getRepositories().isEmpty() );
    }

    public void testCorruptFile()
        throws Exception
    {
        ConfigurationStore configurationStore = (ConfigurationStore) lookup( ConfigurationStore.ROLE, "corrupt-file" );

        try
        {
            configurationStore.getConfigurationFromStore();
            fail( "Configuration should not have succeeded" );
        }
        catch ( ConfigurationStoreException e )
        {
            // expected
            assertTrue( true );
        }
    }

    public void testGetConfiguration()
        throws Exception
    {
        ConfigurationStore configurationStore = (ConfigurationStore) lookup( ConfigurationStore.ROLE, "default" );

        Configuration configuration = configurationStore.getConfigurationFromStore();

        assertEquals( "check indexPath", ".index", configuration.getIndexPath() );
        assertEquals( "check localRepository", "local-repository", configuration.getLocalRepository() );

        assertEquals( "check managed repositories", 1, configuration.getRepositories().size() );
        RepositoryConfiguration repository =
            (RepositoryConfiguration) configuration.getRepositories().iterator().next();

        assertEquals( "check managed repositories", "managed-repository", repository.getDirectory() );
        assertEquals( "check managed repositories", "local", repository.getName() );
        assertEquals( "check managed repositories", "local", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isIndexed() );

        assertEquals( "check proxied repositories", 1, configuration.getProxiedRepositories().size() );
        ProxiedRepositoryConfiguration proxiedRepository =
            (ProxiedRepositoryConfiguration) configuration.getProxiedRepositories().iterator().next();

        assertEquals( "check proxied repositories", "local", proxiedRepository.getManagedRepository() );
        assertEquals( "check proxied repositories", "http://www.ibiblio.org/maven2/", proxiedRepository.getUrl() );
        assertEquals( "check proxied repositories", "ibiblio", proxiedRepository.getId() );
        assertEquals( "check proxied repositories", "Ibiblio", proxiedRepository.getName() );
        assertEquals( "check proxied repositories", 0, proxiedRepository.getSnapshotsInterval() );
        assertEquals( "check proxied repositories", 0, proxiedRepository.getReleasesInterval() );
        assertTrue( "check proxied repositories", proxiedRepository.isUseNetworkProxy() );

        assertEquals( "check synced repositories", 1, configuration.getSyncedRepositories().size() );
        SyncedRepositoryConfiguration syncedRepository =
            (SyncedRepositoryConfiguration) configuration.getSyncedRepositories().iterator().next();

        assertEquals( "check synced repositories", "local", syncedRepository.getManagedRepository() );
        assertEquals( "check synced repositories", "apache", syncedRepository.getId() );
        assertEquals( "check synced repositories", "ASF", syncedRepository.getName() );
        assertEquals( "check synced repositories", "0 0 * * * ?", syncedRepository.getCronExpression() );
        assertEquals( "check synced repositories", "rsync", syncedRepository.getMethod() );
        Properties properties = new Properties();
        properties.setProperty( "rsyncHost", "host" );
        properties.setProperty( "rsyncMethod", "ssh" );
        assertEquals( "check synced repositories", properties, syncedRepository.getProperties() );
    }

    public void testStoreConfiguration()
        throws Exception
    {
        ConfigurationStore configurationStore = (ConfigurationStore) lookup( ConfigurationStore.ROLE, "save-file" );

        Configuration configuration = new Configuration();
        configuration.setIndexPath( "index-path" );

        File file = getTestFile( "target/test/test-file.xml" );
        file.delete();
        assertFalse( file.exists() );

        configurationStore.storeConfiguration( configuration );

        assertTrue( "Check file exists", file.exists() );

        // read it back
        configuration = configurationStore.getConfigurationFromStore();
        assertEquals( "check value", "index-path", configuration.getIndexPath() );
    }

    /**
     * @noinspection JUnitTestMethodWithNoAssertions
     */
    public void testChangeListeners()
        throws Exception
    {
        ConfigurationStore configurationStore = (ConfigurationStore) lookup( ConfigurationStore.ROLE, "save-file" );

        MockControl control = MockControl.createControl( ConfigurationChangeListener.class );
        ConfigurationChangeListener mock = (ConfigurationChangeListener) control.getMock();
        configurationStore.addChangeListener( mock );

        Configuration configuration = new Configuration();
        mock.notifyOfConfigurationChange( configuration );
        control.replay();

        configurationStore.storeConfiguration( configuration );

        control.verify();
    }
}
