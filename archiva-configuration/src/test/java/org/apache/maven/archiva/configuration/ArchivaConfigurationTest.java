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

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Properties;

/**
 * Test the configuration store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ArchivaConfigurationTest
    extends PlexusTestCase
{
    public void testDefaults()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-defaults" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        // check default configuration
        assertNotNull( "check configuration returned", configuration );
        assertEquals( "check configuration has default elements", "0 0,30 * * * ?",
                      configuration.getDataRefreshCronExpression() );
        assertNull( "check configuration has default elements", configuration.getIndexPath() );
        assertTrue( "check configuration has default elements", configuration.getRepositories().isEmpty() );
    }

    public void testGetConfiguration()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        Configuration configuration = archivaConfiguration.getConfiguration();

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

    public void testGetConfigurationSystemOverride()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        System.setProperty( "org.apache.maven.archiva.localRepository", "system-repository" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        assertEquals( "check localRepository", "system-repository", configuration.getLocalRepository() );
        assertEquals( "check indexPath", ".index", configuration.getIndexPath() );
    }

    public void testStoreConfiguration()
        throws Exception
    {
        File file = getTestFile( "target/test/test-file.xml" );
        file.delete();
        assertFalse( file.exists() );

        // TODO: remove with commons-configuration 1.4
        file.getParentFile().mkdirs();
        org.codehaus.plexus.util.FileUtils.fileWrite( file.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save" );

        Configuration configuration = new Configuration();
        configuration.setIndexPath( "index-path" );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", file.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertEquals( "check value", "index-path", configuration.getIndexPath() );

        // read it back
        archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-read-saved" );
        configuration = archivaConfiguration.getConfiguration();
        assertEquals( "check value", "index-path", configuration.getIndexPath() );
    }
}
