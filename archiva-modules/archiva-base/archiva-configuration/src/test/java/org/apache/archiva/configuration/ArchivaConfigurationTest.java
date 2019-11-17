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

import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Test the configuration store.
 */
@RunWith(ArchivaSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ArchivaConfigurationTest
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    FileTypes filetypes;

    public static Path getTestFile( String path )
    {
        return Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), path );
    }

    @SuppressWarnings( "unchecked" )
    protected <T> T lookup( Class<T> clazz, String hint )
    {
        return (T) applicationContext.getBean( "archivaConfiguration#" + hint, ArchivaConfiguration.class );
    }

    @Test
    public void testGetConfigurationFromDefaults()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-defaults" );
        Configuration configuration = archivaConfiguration.getConfiguration();

        assertConfiguration( configuration, 2, 1, 1 );
        assertEquals( "check network proxies", 0, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository = configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );
    }

    @Test
    public void testGetConfigurationFromRegistryWithASingleNamedConfigurationResource()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-configuration" );
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration, 2, 2, 2 );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository = configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );
    }

    /**
     * Ensures that the provided configuration matches the details present in the archiva-default.xml file.
     */
    private void assertConfiguration( Configuration configuration, int managedExpected, int remoteExpected,
                                      int proxyConnectorExpected )
        throws Exception
    {

        assertEquals( "check managed repositories: " + configuration.getManagedRepositories(), managedExpected,
                      configuration.getManagedRepositories().size() );
        assertEquals( "check remote repositories: " + configuration.getRemoteRepositories(), remoteExpected,
                      configuration.getRemoteRepositories().size() );
        assertEquals( "check proxy connectors:" + configuration.getProxyConnectors(), proxyConnectorExpected,
                      configuration.getProxyConnectors().size() );

        RepositoryScanningConfiguration repoScanning = configuration.getRepositoryScanning();
        assertNotNull( "check repository scanning", repoScanning );
        assertEquals( "check file types", 4, repoScanning.getFileTypes().size() );
        assertEquals( "check known consumers", 9, repoScanning.getKnownContentConsumers().size() );
        assertEquals( "check invalid consumers", 1, repoScanning.getInvalidContentConsumers().size() );

        List<String> patterns = filetypes.getFileTypePatterns( "artifacts" );
        assertNotNull( "check 'artifacts' file type", patterns );
        assertEquals( "check 'artifacts' patterns", 13, patterns.size() );

        WebappConfiguration webapp = configuration.getWebapp();
        assertNotNull( "check webapp", webapp );

        UserInterfaceOptions ui = webapp.getUi();
        assertNotNull( "check webapp ui", ui );
        assertTrue( "check showFindArtifacts", ui.isShowFindArtifacts() );
        assertTrue( "check appletFindEnabled", ui.isAppletFindEnabled() );
    }

    @Test
    public void testGetConfigurationFromRegistryWithTwoConfigurationResources()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-configuration-both" );

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

    @Test
    public void testGetConfigurationSystemOverride()
        throws Exception
    {

        System.setProperty( "org.apache.archiva.webapp.ui.appletFindEnabled", "false" );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-configuration" );

        archivaConfiguration.reload();

        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();

            assertFalse( "check boolean", configuration.getWebapp().getUi().isAppletFindEnabled() );
        }
        finally
        {
            System.getProperties().remove( "org.apache.archiva.webapp.ui.appletFindEnabled" );
            archivaConfiguration.reload();
            Configuration configuration = archivaConfiguration.getConfiguration();
            assertTrue( "check boolean", configuration.getWebapp().getUi().isAppletFindEnabled() );
        }
    }

    @Test
    public void testStoreConfiguration()
        throws Exception
    {
        Path file = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(file);
        assertFalse( Files.exists(file) );

        // TODO: remove with commons-configuration 1.4
        //file.getParentFile().mkdirs();
        //FileUtils.writeStringToFile( file, "<configuration/>", null );

        DefaultArchivaConfiguration archivaConfiguration =
            (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class, "test-save" );

        archivaConfiguration.reload();

        Configuration configuration = new Configuration();
        configuration.setVersion( "1" );
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        // add a change listener
        ConfigurationListener listener = createMock( ConfigurationListener.class );
        archivaConfiguration.addListener( listener );

        listener.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED ) );

        replay( listener );

        archivaConfiguration.save( configuration );

        verify( listener );

        assertTrue( "Check file exists", Files.exists(file) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        // read it back
        archivaConfiguration = (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class, "test-read-saved" );

        archivaConfiguration.reload();
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    private static ConfigurationListener createConfigurationListenerMockControl()
    {
        return createMock( ConfigurationListener.class );// MockControl.createControl( ConfigurationListener.class );
    }

    @Test
    public void testStoreConfigurationUser()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists( baseFile );
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists( userFile );
        assertFalse( Files.exists(userFile) );

        Files.createDirectories(userFile.getParent());
        FileUtils.writeStringToFile( userFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", Files.exists(userFile) );
        assertFalse( "Check file not created", Files.exists(baseFile) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    @Test
    public void testStoreConfigurationLoadedFromDefaults()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.delete(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-save-user-defaults" );

        archivaConfiguration.reload();

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        // add a change listener
        ConfigurationListener listener = createConfigurationListenerMockControl();
        archivaConfiguration.addListener( listener );

        listener.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED ) );

        replay( listener );

        archivaConfiguration.save( configuration );

        verify( listener );

        assertTrue( "Check file exists", Files.exists(userFile) );
        assertFalse( "Check file not created", Files.exists(baseFile) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    @Test
    public void testDefaultUserConfigFilename()
        throws Exception
    {
        DefaultArchivaConfiguration archivaConfiguration =
            (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class, "default" );
        String expectedFile = System.getProperty( "user.home" ) + "/.m2/archiva.xml";
        String systemFile = System.getProperty(ArchivaConfiguration.USER_CONFIG_PROPERTY);
        if (StringUtils.isNotEmpty( systemFile )) {
            expectedFile = systemFile;
        } else
        {
            String envFile = System.getenv( ArchivaConfiguration.USER_CONFIG_ENVVAR );
            if ( StringUtils.isNotEmpty( envFile ) )
                expectedFile = envFile;
        }

        archivaConfiguration.reload();

        assertEquals( expectedFile,
                      archivaConfiguration.getUserConfigFilename() );
        assertEquals( System.getProperty( "appserver.base", "${appserver.base}" ) + "/conf/archiva.xml",
                      archivaConfiguration.getAltConfigFilename() );
    }

    @Test
    public void testStoreConfigurationFallback()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        Files.createDirectories( baseFile.getParent());
        FileUtils.writeStringToFile( baseFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class, "test-save-user-fallback" );

        archivaConfiguration.reload();

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", Files.exists(baseFile) );
        assertFalse( "Check file not created", Files.exists(userFile) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    @Test
    public void testStoreConfigurationFailsWhenReadFromBothLocationsNoLists()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        Files.createDirectories( baseFile.getParent() );
        FileUtils.writeStringToFile( baseFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        Files.createDirectories( userFile.getParent());
        FileUtils.writeStringToFile( userFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-save-user" );

        archivaConfiguration.reload();

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertTrue( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", Files.exists(baseFile) );
        assertEquals( "Check base file is unchanged", "<configuration/>",
                      FileUtils.readFileToString( baseFile.toFile(), Charset.forName( "UTF-8" ) ) );
        assertTrue( "Check file exists", Files.exists(userFile) );
        assertFalse( "Check base file is changed",
                     "<configuration/>".equals( FileUtils.readFileToString( userFile.toFile(), Charset.forName( "UTF-8" ) ) ) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    @Test
    public void testStoreConfigurationFailsWhenReadFromBothLocationsUserHasLists()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        Files.createDirectories( userFile.getParent() );
        FileUtils.copyFile( getTestFile( "src/test/conf/conf-user.xml" ).toFile(), userFile.toFile() );

        Files.createDirectories(baseFile.getParent());
        FileUtils.writeStringToFile( baseFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-save-user" );

        archivaConfiguration.reload();

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertTrue( "check value", configuration.getWebapp().getUi().isShowFindArtifacts() );

        configuration.getWebapp().getUi().setShowFindArtifacts( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", Files.exists(baseFile) );
        assertEquals( "Check base file is unchanged", "<configuration/>",
                      FileUtils.readFileToString( baseFile.toFile(), Charset.forName( "UTF-8" ) ) );
        assertTrue( "Check file exists", Files.exists(userFile) );
        assertFalse( "Check base file is changed",
                     "<configuration/>".equals( FileUtils.readFileToString( userFile.toFile(), Charset.forName( "UTF-8" ) ) ) );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isShowFindArtifacts() );
    }

    @Test
    public void testStoreConfigurationFailsWhenReadFromBothLocationsAppserverHasLists()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        Files.createDirectories(baseFile.getParent());
        FileUtils.copyFile( getTestFile( "src/test/conf/conf-base.xml" ).toFile(), baseFile.toFile() );

        Files.createDirectories(userFile.getParent());
        FileUtils.writeStringToFile( userFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-save-user" );

        archivaConfiguration.reload();

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

    @Test
    public void testLoadConfigurationFromInvalidBothLocationsOnDisk()
        throws Exception
    {
        String propFile = System.getProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY );
        System.setProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY, "/../../..//*intentionally:invalid*/.m2/archiva-user.xml" );
        ArchivaConfiguration archivaConfiguration =
            lookup( ArchivaConfiguration.class, "test-not-allowed-to-write-to-both" );
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
        if (propFile!=null)
        {
            System.setProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY, propFile );
        }
    }

    @Test
    public void testLoadConfigurationFromInvalidUserLocationOnDisk()
        throws Exception
    {
        Path testConfDir = getTestFile( "target/test-appserver-base/conf/" );
        Files.createDirectories( testConfDir );

        ArchivaConfiguration archivaConfiguration =
            lookup( ArchivaConfiguration.class, "test-not-allowed-to-write-to-user" );
        Configuration config = archivaConfiguration.getConfiguration();
        archivaConfiguration.save( config );
        // No Exception == test passes.
        // Expected Path is: Should not have thrown an exception.
    }


    @Test
    public void testConfigurationUpgradeFrom13()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-upgrade-1.3" );

        // we just use the defaults when upgrading from 1.3 at this point.
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration, 2, 2, 2 );
        assertEquals( "check network proxies", 0, configuration.getNetworkProxies().size() );

        ManagedRepositoryConfiguration repository = configuration.getManagedRepositories().get( 0 );

        assertEquals( "check managed repositories", "${appserver.base}/data/repositories/internal",
                      repository.getLocation() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isScanned() );

        log.info( "knowContentConsumers {}", configuration.getRepositoryScanning().getKnownContentConsumers() );

        assertFalse(
            configuration.getRepositoryScanning().getKnownContentConsumers().contains( "update-db-artifact" ) );
        assertFalse( configuration.getRepositoryScanning().getKnownContentConsumers().contains(
            "update-db-repository-metadata" ) );

        assertTrue(
            configuration.getRepositoryScanning().getKnownContentConsumers().contains( "create-archiva-metadata" ) );

        assertTrue(
            configuration.getRepositoryScanning().getKnownContentConsumers().contains( "duplicate-artifacts" ) );
    }


    @Test
    public void testCronExpressionsWithComma()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        Files.createDirectories(baseFile.getParent());
        FileUtils.copyFile( getTestFile( "src/test/conf/escape-cron-expressions.xml" ).toFile(), baseFile.toFile() );

        Files.createDirectories(userFile.getParent());
        FileUtils.writeStringToFile( userFile.toFile(), "<configuration/>", Charset.defaultCharset() );

        final ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-cron-expressions" );

        archivaConfiguration.reload();

        Configuration configuration = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration repository = configuration.getManagedRepositories().get( 0 );

        assertEquals( "check cron expression", "0 0,30 * * * ?", repository.getRefreshCronExpression().trim() );

        // add a test listener to confirm it doesn't see the escaped format. We don't need to test the number of calls,
        // etc. as it's done in other tests
        archivaConfiguration.addListener( new ConfigurationListener()
        {
            @Override
            public void configurationEvent( ConfigurationEvent event )
            {
                assertEquals( ConfigurationEvent.SAVED, event.getType() );

            }
        } );

        archivaConfiguration.save( configuration );

        configuration = archivaConfiguration.getConfiguration();

        // test for the escape character '\' showing up on repositories.jsp
        repository.setRefreshCronExpression( "0 0,20 0 * * ?" );

        archivaConfiguration.save( configuration );

        repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( "snapshots" );

        assertEquals( "check cron expression", "0 0,20 0 * * ?", repository.getRefreshCronExpression() );
    }

    @Test
    public void testRemoveLastElements()
        throws Exception
    {
        Path baseFile = getTestFile( "target/test/test-file.xml" );
        Files.deleteIfExists(baseFile);
        assertFalse( Files.exists(baseFile) );

        Path userFile = getTestFile( "target/test/test-file-user.xml" );
        Files.deleteIfExists(userFile);
        assertFalse( Files.exists(userFile) );

        Files.createDirectories( baseFile.getParent() );
        FileUtils.copyFile( getTestFile( "src/test/conf/conf-single-list-elements.xml" ).toFile(), baseFile.toFile() );

        Files.createDirectories( userFile.getParent());
        FileUtils.writeStringToFile( userFile.toFile(), "<configuration/>", Charset.forName( "UTF-8" ) );

        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-remove-central" );

        archivaConfiguration.reload();

        Configuration configuration = archivaConfiguration.getConfiguration();

        RepositoryGroupConfiguration repositoryGroup = configuration.getRepositoryGroups().get( 0 );
        assertNotNull( repositoryGroup );
        configuration.removeRepositoryGroup( repositoryGroup );
        assertTrue( configuration.getRepositoryGroups().isEmpty() );

        RemoteRepositoryConfiguration repository = configuration.getRemoteRepositoriesAsMap().get( "central" );
        assertNotNull( repository );
        configuration.removeRemoteRepository( repository );
        assertTrue( configuration.getRemoteRepositories().isEmpty() );

        ManagedRepositoryConfiguration managedRepository =
            configuration.getManagedRepositoriesAsMap().get( "snapshots" );
        assertNotNull( managedRepository );
        configuration.removeManagedRepository( managedRepository );
        assertTrue( configuration.getManagedRepositories().isEmpty() );

        ProxyConnectorConfiguration proxyConnector = configuration.getProxyConnectors().get( 0 );
        assertNotNull( proxyConnector );
        configuration.removeProxyConnector( proxyConnector );
        assertTrue( configuration.getProxyConnectors().isEmpty() );

        NetworkProxyConfiguration networkProxy = configuration.getNetworkProxiesAsMap().get( "proxy" );
        assertNotNull( networkProxy );
        configuration.removeNetworkProxy( networkProxy );
        assertTrue( configuration.getNetworkProxies().isEmpty() );

        LegacyArtifactPath path = configuration.getLegacyArtifactPaths().get( 0 );
        assertNotNull( path );
        configuration.removeLegacyArtifactPath( path );
        assertTrue( configuration.getLegacyArtifactPaths().isEmpty() );

        RepositoryScanningConfiguration scanning = configuration.getRepositoryScanning();
        String consumer = scanning.getKnownContentConsumers().get( 0 );
        assertNotNull( consumer );
        scanning.removeKnownContentConsumer( consumer );
        // default values
        assertFalse( scanning.getKnownContentConsumers().isEmpty() );
        consumer = scanning.getInvalidContentConsumers().get( 0 );
        assertNotNull( consumer );
        scanning.removeInvalidContentConsumer( consumer );
        assertTrue( scanning.getInvalidContentConsumers().isEmpty() );

        archivaConfiguration.save( configuration );

        archivaConfiguration = lookup( ArchivaConfiguration.class, "test-read-saved" );
        configuration = archivaConfiguration.getConfiguration();
        assertNull( configuration.getRemoteRepositoriesAsMap().get( "central" ) );
        assertTrue( configuration.getRepositoryGroups().isEmpty() );
        assertNull( configuration.getManagedRepositoriesAsMap().get( "snapshots" ) );
        assertTrue( configuration.getProxyConnectors().isEmpty() );
        assertNull( configuration.getNetworkProxiesAsMap().get( "proxy" ) );
        assertTrue( configuration.getLegacyArtifactPaths().isEmpty() );
        scanning = configuration.getRepositoryScanning();
        assertFalse( scanning.getKnownContentConsumers().isEmpty() );
        assertTrue( scanning.getInvalidContentConsumers().isEmpty() );
    }

    /**
     * [MRM-582] Remote Repositories with empty <username> and <password> fields shouldn't be created in configuration.
     */
    @Test
    public void testGetConfigurationFixEmptyRemoteRepoUsernamePassword()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = lookup( ArchivaConfiguration.class, "test-configuration" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration, 2, 2, 2 );
        assertEquals( "check remote repositories", 2, configuration.getRemoteRepositories().size() );

        RemoteRepositoryConfiguration repository =
            configuration.getRemoteRepositoriesAsMap().get( "maven2-repository.dev.java.net" );

        assertEquals( "remote repository.url", "https://maven2-repository.dev.java.net/nonav/repository",
                      repository.getUrl() );
        assertEquals( "remote repository.name", "Java.net Repository for Maven 2", repository.getName() );
        assertEquals( "remote repository.id", "maven2-repository.dev.java.net", repository.getId() );
        assertEquals( "remote repository.layout", "default", repository.getLayout() );
        assertNull( "remote repository.username == null", repository.getUsername() );
        assertNull( "remote repository.password == null", repository.getPassword() );
    }
}
