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

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.List;

/**
 * Test the configuration store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ArchivaConfigurationTest extends PlexusTestCase
{
    public void testDefaults() throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class, "test-defaults" );
        
        Configuration configuration = archivaConfiguration.getConfiguration();

        // check default configuration
        assertNotNull( "check configuration returned", configuration );
        assertTrue( "check configuration has default elements", configuration.getRepositories().isEmpty() );
    }

    public void testGetConfiguration() throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        FileTypes filetypes = (FileTypes) lookup( FileTypes.class.getName() );

        Configuration configuration = archivaConfiguration.getConfiguration();

        assertEquals( "check repositories", 4, configuration.getRepositories().size() );
        assertEquals( "check proxy connectors", 2, configuration.getProxyConnectors().size() );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        RepositoryScanningConfiguration repoScanning = configuration.getRepositoryScanning();
        assertNotNull( "check repository scanning", repoScanning );
        assertEquals( "check file types", 4, repoScanning.getFileTypes().size() );
        assertEquals( "check known consumers", 8, repoScanning.getKnownContentConsumers().size() );
        assertEquals( "check invalid consumers", 1, repoScanning.getInvalidContentConsumers().size() );

        List patterns = filetypes.getFileTypePatterns( "artifacts" );
        assertNotNull( "check 'artifacts' file type", patterns );
        assertEquals( "check 'artifacts' patterns", 13, patterns.size() );

        DatabaseScanningConfiguration dbScanning = configuration.getDatabaseScanning();
        assertNotNull( "check database scanning", dbScanning );
        assertEquals( "check unprocessed consumers", 6, dbScanning.getUnprocessedConsumers().size() );
        assertEquals( "check cleanup consumers", 3, dbScanning.getCleanupConsumers().size() );

        RepositoryConfiguration repository =
            (RepositoryConfiguration) configuration.getRepositories().iterator().next();

        assertEquals( "check managed repositories", "file://${appserver.home}/repositories/internal",
                      repository.getUrl() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isIndexed() );
    }

    public void testGetConfigurationSystemOverride() throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        System.setProperty( "org.apache.maven.archiva.localRepository", "system-repository" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        //        assertEquals( "check localRepository", "system-repository", configuration.getLocalRepository() );
        //        assertEquals( "check indexPath", ".index", configuration.getIndexPath() );
    }

    public void testStoreConfiguration() throws Exception
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
        //        configuration.setIndexPath( "index-path" );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", file.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        //        assertEquals( "check value", "index-path", configuration.getIndexPath() );

        // read it back
        archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-read-saved" );
        configuration = archivaConfiguration.getConfiguration();
        //        assertEquals( "check value", "index-path", configuration.getIndexPath() );
    }

    public void testStoreConfigurationUser() throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        // TODO: remove with commons-configuration 1.4
        userFile.getParentFile().mkdirs();
        org.codehaus.plexus.util.FileUtils.fileWrite( userFile.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        //        configuration.setIndexPath( "index-path" );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check file not created", baseFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        //        assertEquals( "check value", "index-path", configuration.getIndexPath() );
    }

    public void testStoreConfigurationFallback() throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        // TODO: remove with commons-configuration 1.4
        baseFile.getParentFile().mkdirs();
        org.codehaus.plexus.util.FileUtils.fileWrite( baseFile.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        //        configuration.setIndexPath( "index-path" );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", baseFile.exists() );
        assertFalse( "Check file not created", userFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        //        assertEquals( "check value", "index-path", configuration.getIndexPath() );
    }

    public void testRemoveProxiedRepositoryAndStoreConfiguration() throws Exception
    {
        // MRM-300

        File src = getTestFile( "src/test/conf/with-proxied-repos.xml" );
        File dest = getTestFile( "target/test/with-proxied-repos.xml" );
        FileUtils.copyFile( src, dest );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-remove-proxied-repo" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        //        configuration.getProxiedRepositories().remove( 0 );

        archivaConfiguration.save( configuration );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        //        assertEquals( 1, configuration.getProxiedRepositories().size() );

        release( archivaConfiguration );

        // read it back
        archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-read-back-remove-proxied-repo" );
        configuration = archivaConfiguration.getConfiguration();
        //        assertEquals( 1, configuration.getProxiedRepositories().size() );
    }
}
