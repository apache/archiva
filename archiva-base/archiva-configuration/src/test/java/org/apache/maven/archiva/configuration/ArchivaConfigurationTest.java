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
import org.codehaus.plexus.registry.RegistryException;
import org.custommonkey.xmlunit.XMLAssert;
import org.easymock.MockControl;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Test the configuration store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ArchivaConfigurationTest
    extends PlexusTestCase
{
    public void testGetConfigurationFromRegistryWithASingleNamedConfigurationResource()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository =
            (ManagedRepositoryConfiguration) configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );
    }

    public void testGetConfigurationFromDefaults()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-defaults" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 0, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository =
            (ManagedRepositoryConfiguration) configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/data/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );
    }

    /**
     * Ensures that the provided configuration matches the details present in the archiva-default.xml file.
     */
    private void assertConfiguration( Configuration configuration )
        throws Exception
    {
        FileTypes filetypes = (FileTypes) lookup( FileTypes.class.getName() );

        assertEquals( "check repositories", 2, configuration.getManagedRepositories().size() );
        assertEquals( "check repositories", 2, configuration.getRemoteRepositories().size() );
        assertEquals( "check proxy connectors", 2, configuration.getProxyConnectors().size() );

        RepositoryScanningConfiguration repoScanning = configuration.getRepositoryScanning();
        assertNotNull( "check repository scanning", repoScanning );
        assertEquals( "check file types", 4, repoScanning.getFileTypes().size() );
        assertEquals( "check known consumers", 9, repoScanning.getKnownContentConsumers().size() );
        assertEquals( "check invalid consumers", 1, repoScanning.getInvalidContentConsumers().size() );

        List<String> patterns = filetypes.getFileTypePatterns( "artifacts" );
        assertNotNull( "check 'artifacts' file type", patterns );
        assertEquals( "check 'artifacts' patterns", 13, patterns.size() );

        DatabaseScanningConfiguration dbScanning = configuration.getDatabaseScanning();
        assertNotNull( "check database scanning", dbScanning );
        assertEquals( "check unprocessed consumers", 6, dbScanning.getUnprocessedConsumers().size() );
        assertEquals( "check cleanup consumers", 3, dbScanning.getCleanupConsumers().size() );

        WebappConfiguration webapp = configuration.getWebapp();
        assertNotNull( "check webapp", webapp );

        UserInterfaceOptions ui = webapp.getUi();
        assertNotNull( "check webapp ui", ui );
        assertTrue( "check showFindArtifacts", ui.isShowFindArtifacts() );
        assertTrue( "check appletFindEnabled", ui.isAppletFindEnabled() );
    }

    public void testGetConfigurationFromRegistryWithTwoConfigurationResources()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration-both" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        // from base
        assertEquals( "check repositories", 2, configuration.getManagedRepositories().size() );
        assertEquals( "check repositories", 2, configuration.getRemoteRepositories().size() );
        // from user
        assertEquals( "check proxy connectors", 2, configuration.getProxyConnectors().size() );

        WebappConfiguration webapp = configuration.getWebapp();
        assertNotNull( "check webapp", webapp );

        UserInterfaceOptions ui = webapp.getUi();
        assertNotNull( "check webapp ui", ui );
        // from base
        assertFalse( "check showFindArtifacts", ui.isShowFindArtifacts() );
        // from user
        assertFalse( "check appletFindEnabled", ui.isAppletFindEnabled() );
    }

    public void testGetConfigurationSystemOverride()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        System.setProperty( "org.apache.maven.archiva.webapp.ui.appletFindEnabled", "false" );

        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();

            assertFalse( "check boolean", configuration.getWebapp().getUi().isAppletFindEnabled() );
        }
        finally
        {
            System.getProperties().remove( "org.apache.maven.archiva.webapp.ui.appletFindEnabled" );
        }
    }

    public void testStoreConfiguration()
        throws Exception
    {
        File file = getTestFile( "target/test/test-file.xml" );
        file.delete();
        assertFalse( file.exists() );

        // TODO: remove with commons-configuration 1.4
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile( file, "<configuration/>", null );

        DefaultArchivaConfiguration archivaConfiguration =
            (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save" );

        Configuration configuration = new Configuration();
        configuration.setVersion( "1" );
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        // add a change listener
        MockControl control = createConfigurationListenerMockControl();
        ConfigurationListener listener = (ConfigurationListener) control.getMock();
        archivaConfiguration.addListener( listener );

        listener.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED ) );
        control.setVoidCallable();

        control.replay();

        archivaConfiguration.save( configuration );

        control.verify();

        assertTrue( "Check file exists", file.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        // read it back
        archivaConfiguration =
            (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-read-saved" );
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    private static MockControl createConfigurationListenerMockControl()
    {
        return MockControl.createControl( ConfigurationListener.class );
    }

    public void testStoreConfigurationUser()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        userFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( userFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check file not created", baseFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testStoreConfigurationLoadedFromDefaults()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        // add a change listener
        MockControl control = createConfigurationListenerMockControl();
        ConfigurationListener listener = (ConfigurationListener) control.getMock();
        archivaConfiguration.addListener( listener );

        listener.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED ) );
        // once from default creation, and again from manual call to save
        control.setVoidCallable( 2 );

        control.replay();

        archivaConfiguration.save( configuration );

        control.verify();

        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check file not created", baseFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testDefaultUserConfigFilename()
        throws Exception
    {
        DefaultArchivaConfiguration archivaConfiguration =
            (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class.getName() );

        assertEquals( System.getProperty( "user.home" ) + "/.m2/archiva.xml",
                      archivaConfiguration.getUserConfigFilename() );
        assertEquals( System.getProperty( "appserver.base", "${appserver.base}" ) + "/conf/archiva.xml",
                      archivaConfiguration.getAltConfigFilename() );
    }

    public void testStoreConfigurationFallback()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( baseFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", baseFile.exists() );
        assertFalse( "Check file not created", userFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testStoreConfigurationFailsWhenReadFromBothLocationsNoLists()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( baseFile, "<configuration/>", null );

        userFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( userFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertTrue( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", baseFile.exists() );
        assertEquals( "Check base file is unchanged", "<configuration/>",
                      FileUtils.readFileToString( baseFile, null ) );
        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check base file is changed",
                     "<configuration/>".equals( FileUtils.readFileToString( userFile, null ) ) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testStoreConfigurationFailsWhenReadFromBothLocationsUserHasLists()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        userFile.getParentFile().mkdirs();
        FileUtils.copyFile( getTestFile( "src/test/conf/conf-user.xml" ), userFile );

        baseFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( baseFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertTrue( "check value", configuration.getWebapp().getUi().isShowFindArtifacts() );

        configuration.getWebapp().getUi().setShowFindArtifacts( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", baseFile.exists() );
        assertEquals( "Check base file is unchanged", "<configuration/>",
                      FileUtils.readFileToString( baseFile, null ) );
        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check base file is changed",
                     "<configuration/>".equals( FileUtils.readFileToString( userFile, null ) ) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isShowFindArtifacts() );
    }

    public void testStoreConfigurationFailsWhenReadFromBothLocationsAppserverHasLists()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.copyFile( getTestFile( "src/test/conf/conf-base.xml" ), baseFile );

        userFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( userFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertTrue( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        try
        {
            archivaConfiguration.save( configuration );
            fail( "Configuration saving should not succeed if it was loaded from two locations" );
        }
        catch ( IndeterminateConfigurationException e )
        {
            // check it was reverted
            configuration = archivaConfiguration.getConfiguration();
            assertTrue( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
        }
    }

    public void testLoadConfigurationFromInvalidBothLocationsOnDisk()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-not-allowed-to-write-to-both" );
        Configuration config = archivaConfiguration.getConfiguration();

        try
        {
            archivaConfiguration.save( config );
            fail( "Should have thrown a RegistryException because the configuration can't be saved." );
        }
        catch ( RegistryException e )
        {
            /* expected exception */
        }
    }

    public void testLoadConfigurationFromInvalidUserLocationOnDisk()
        throws Exception
    {
        File testConfDir = getTestFile( "target/test-appserver-base/conf/" );
        testConfDir.mkdirs();

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-not-allowed-to-write-to-user" );
        Configuration config = archivaConfiguration.getConfiguration();
        archivaConfiguration.save( config );
        // No Exception == test passes. 
        // Expected Path is: Should not have thrown an exception.
    }

    public void testConfigurationUpgradeFrom09()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-upgrade-09" );

        // we just use the defaults when upgrading from 0.9 at this point.
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 0, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository =
            (ManagedRepositoryConfiguration) configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/data/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );
    }

    public void testAutoDetectV1()
        throws Exception
    {
        // Setup the autodetect-v1.xml file in the target directory (so we can save/load it)
        File userFile = getTestFile( "target/test-autodetect-v1/archiva-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        userFile.getParentFile().mkdirs();
        FileUtils.copyFile( getTestFile( "src/test/conf/autodetect-v1.xml" ), userFile );

        // Load the original (unconverted) archiva.xml
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-autodetect-v1" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository =
            (ManagedRepositoryConfiguration) configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );

        // Test that only 1 set of repositories exist.
        assertEquals( "check managed repositories size.", 2, configuration.getManagedRepositories().size() );
        assertEquals( "check remote repositories size.", 2, configuration.getRemoteRepositories().size() );
        assertEquals( "check v1 repositories size.", 0, configuration.getRepositories().size() );

        // Save the file.
        archivaConfiguration.save( configuration );

        // Release existing
        release( archivaConfiguration );

        // Reload.
        archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-autodetect-v1" );
        configuration = archivaConfiguration.getConfiguration();

        // Test that only 1 set of repositories exist.
        assertEquals( "check managed repositories size.", 2, configuration.getManagedRepositories().size() );
        assertEquals( "check managed repositories size.", 2, configuration.getManagedRepositoriesAsMap().size() );
        assertEquals( "check remote repositories size.", 2, configuration.getRemoteRepositories().size() );
        assertEquals( "check remote repositories size.", 2, configuration.getRemoteRepositoriesAsMap().size() );
        assertEquals( "check v1 repositories size.", 0, configuration.getRepositories().size() );

        String actualXML = FileUtils.readFileToString( userFile, null );
        XMLAssert.assertXpathNotExists( "//configuration/repositories/repository", actualXML );
        XMLAssert.assertXpathNotExists( "//configuration/repositories", actualXML );
    }

    public void testArchivaV1()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-archiva-v1" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        assertEquals( "check managed repositories", 2, configuration.getManagedRepositories().size() );
        assertEquals( "check v1 repositories size.", 0, configuration.getRepositories().size() );

        Map<String, ManagedRepositoryConfiguration> map = configuration.getManagedRepositoriesAsMap();

        ManagedRepositoryConfiguration repository = map.get( "internal" );
        assertEquals( "check managed repositories", "${appserver.base}/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );
        assertFalse( "check managed repositories", repository.isSnapshots() );

        repository = map.get( "snapshots" );
        assertEquals( "check managed repositories", "${appserver.base}/repositories/snapshots",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Snapshot Repository", repository.getName() );
        assertEquals( "check managed repositories", "snapshots", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertFalse( "check managed repositories", repository.isScanned() );
        assertTrue( "check managed repositories", repository.isSnapshots() );
    }

    public void testCronExpressionsWithComma()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.copyFile( getTestFile( "src/test/conf/escape-cron-expressions.xml" ), baseFile );

        userFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( userFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-cron-expressions" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration repository =
            (ManagedRepositoryConfiguration) configuration.getManagedRepositories().get( 0 );

        assertEquals( "check cron expression", "0 0,30 * * ?", repository.getRefreshCronExpression().trim() );

        configuration.getDatabaseScanning().setCronExpression( "0 0,15 0 * * ?" );

        archivaConfiguration.save( configuration );

        configuration = archivaConfiguration.getConfiguration();

        assertEquals( "check cron expression", "0 0,15 0 * * ?",
                      configuration.getDatabaseScanning().getCronExpression() );

        // test for the escape character '\' showing up on repositories.jsp
        repository.setRefreshCronExpression( "0 0,20 0 * * ?" );

        archivaConfiguration.save( configuration );

        repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( "snapshots" );

        assertEquals( "check cron expression", "0 0,20 0 * * ?", repository.getRefreshCronExpression() );
    }

    public void testRemoveLastElements()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.copyFile( getTestFile( "src/test/conf/conf-single-list-elements.xml" ), baseFile );

        userFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( userFile, "<configuration/>", null );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-remove-central" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        RemoteRepositoryConfiguration repository = configuration.getRemoteRepositoriesAsMap().get( "central" );
        assertNotNull( repository );
        configuration.removeRemoteRepository( repository );
        assertTrue( configuration.getRemoteRepositories().isEmpty() );

        ManagedRepositoryConfiguration managedRepository =
            configuration.getManagedRepositoriesAsMap().get( "snapshots" );
        assertNotNull( managedRepository );
        configuration.removeManagedRepository( managedRepository );
        assertTrue( configuration.getManagedRepositories().isEmpty() );

        ProxyConnectorConfiguration proxyConnector =
            (ProxyConnectorConfiguration) configuration.getProxyConnectors().get( 0 );
        assertNotNull( proxyConnector );
        configuration.removeProxyConnector( proxyConnector );
        assertTrue( configuration.getProxyConnectors().isEmpty() );

        NetworkProxyConfiguration networkProxy = configuration.getNetworkProxiesAsMap().get( "proxy" );
        assertNotNull( networkProxy );
        configuration.removeNetworkProxy( networkProxy );
        assertTrue( configuration.getNetworkProxies().isEmpty() );

        LegacyArtifactPath path = (LegacyArtifactPath) configuration.getLegacyArtifactPaths().get( 0 );
        assertNotNull( path );
        configuration.removeLegacyArtifactPath( path );
        assertTrue( configuration.getLegacyArtifactPaths().isEmpty() );

        RepositoryScanningConfiguration scanning = configuration.getRepositoryScanning();
        String consumer = (String) scanning.getKnownContentConsumers().get( 0 );
        assertNotNull( consumer );
        scanning.removeKnownContentConsumer( consumer );
        assertTrue( scanning.getKnownContentConsumers().isEmpty() );
        consumer = (String) scanning.getInvalidContentConsumers().get( 0 );
        assertNotNull( consumer );
        scanning.removeInvalidContentConsumer( consumer );
        assertTrue( scanning.getInvalidContentConsumers().isEmpty() );

        DatabaseScanningConfiguration databaseScanning = configuration.getDatabaseScanning();
        consumer = (String) databaseScanning.getCleanupConsumers().get( 0 );
        assertNotNull( consumer );
        databaseScanning.removeCleanupConsumer( consumer );
        assertTrue( databaseScanning.getCleanupConsumers().isEmpty() );
        consumer = (String) databaseScanning.getUnprocessedConsumers().get( 0 );
        assertNotNull( consumer );
        databaseScanning.removeUnprocessedConsumer( consumer );
        assertTrue( databaseScanning.getUnprocessedConsumers().isEmpty() );

        archivaConfiguration.save( configuration );

        archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-read-saved" );
        configuration = archivaConfiguration.getConfiguration();
        assertNull( configuration.getRemoteRepositoriesAsMap().get( "central" ) );
        assertNull( configuration.getManagedRepositoriesAsMap().get( "snapshots" ) );
        assertTrue( configuration.getProxyConnectors().isEmpty() );
        assertNull( configuration.getNetworkProxiesAsMap().get( "proxy" ) );
        assertTrue( configuration.getLegacyArtifactPaths().isEmpty() );
        scanning = configuration.getRepositoryScanning();
        assertTrue( scanning.getKnownContentConsumers().isEmpty() );
        assertTrue( scanning.getInvalidContentConsumers().isEmpty() );
        databaseScanning = configuration.getDatabaseScanning();
        assertTrue( databaseScanning.getCleanupConsumers().isEmpty() );
        assertTrue( databaseScanning.getUnprocessedConsumers().isEmpty() );
    }

    /**
     * [MRM-582] Remote Repositories with empty <username> and <password> fields shouldn't be created in configuration.
     */
    public void testGetConfigurationFixEmptyRemoteRepoUsernamePassword()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = (ArchivaConfiguration) lookup(
                                                                                   ArchivaConfiguration.class.getName(),
                                                                                   "test-configuration" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check remote repositories", 2, configuration.getRemoteRepositories().size() );

        RemoteRepositoryConfiguration repository = (RemoteRepositoryConfiguration) configuration
            .getRemoteRepositoriesAsMap().get( "maven2-repository.dev.java.net" );

        assertEquals( "remote repository.url", "https://maven2-repository.dev.java.net/nonav/repository", repository.getUrl() );
        assertEquals( "remote repository.name", "Java.net Repository for Maven 2", repository.getName() );
        assertEquals( "remote repository.id", "maven2-repository.dev.java.net", repository.getId() );
        assertEquals( "remote repository.layout", "default", repository.getLayout() );
        assertNull( "remote repository.username == null", repository.getUsername() );
        assertNull( "remote repository.password == null", repository.getPassword() );
    }
}
