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

import net.sf.ehcache.CacheManager;
import org.apache.archiva.configuration.*;
import org.apache.archiva.policies.*;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.maven.wagon.Wagon;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * AbstractProxyTestCase
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public abstract class AbstractProxyTestCase
{
    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    private ProxyRegistry proxyRegistry;

    @Inject
    RepositoryRegistry repositoryRegistry;

    protected static final String ID_PROXIED1 = "proxied1";

    protected static final String ID_PROXIED1_TARGET = "proxied1-target";

    protected static final String ID_PROXIED2 = "proxied2";

    protected static final String ID_PROXIED2_TARGET = "proxied2-target";

    protected static final String ID_DEFAULT_MANAGED = "default-managed-repository";

    protected static final String REPOPATH_PROXIED1 = "src/test/repositories/proxied1";

    protected static final String REPOPATH_PROXIED1_TARGET = "target/test-repository/proxied1";

    protected static final String REPOPATH_PROXIED2 = "src/test/repositories/proxied2";

    protected static final String REPOPATH_PROXIED2_TARGET = "target/test-repository/proxied2";

    protected static final String REPOPATH_DEFAULT_MANAGED = "src/test/repositories/managed";

    // protected static final String REPOPATH_DEFAULT_MANAGED_TARGET = "target/test-repository/managed";

    protected IMocksControl wagonMockControl;

    protected Wagon wagonMock;


    protected RepositoryProxyHandler proxyHandler;

    protected ManagedRepositoryContent managedDefaultRepository;

    protected Path managedDefaultDir;

    protected MockConfiguration config;

    protected Logger log = LoggerFactory.getLogger( getClass() );

    WagonDelegate delegate;

    @Before
    public void setUp()
        throws Exception
    {
        config =
            (MockConfiguration) applicationContext.getBean( "archivaConfiguration#mock", ArchivaConfiguration.class );

        config.getConfiguration().setManagedRepositories( new ArrayList<ManagedRepositoryConfiguration>() );
        config.getConfiguration().setRemoteRepositories( new ArrayList<RemoteRepositoryConfiguration>() );
        config.getConfiguration().setProxyConnectors( new ArrayList<ProxyConnectorConfiguration>() );
        ArchivaRuntimeConfiguration runtimeConfiguration = new ArchivaRuntimeConfiguration();
        List<String> checksumTypes = new ArrayList<>();
        checksumTypes.add("md5");
        checksumTypes.add("sha256");
        checksumTypes.add("sha1");
        checksumTypes.add("asc");
        runtimeConfiguration.setChecksumTypes(checksumTypes);
        config.getConfiguration().setArchivaRuntimeConfiguration(runtimeConfiguration);
        repositoryRegistry.setArchivaConfiguration( config );

        // Setup source repository (using default layout)
        String name = getClass().getSimpleName();
        Path repoPath = Paths.get("target/test-repository/managed/" + name);

        managedDefaultRepository =
            createRepository( ID_DEFAULT_MANAGED, "Default Managed Repository", repoPath.toString(), "default" );

        managedDefaultDir = repoPath.resolve(ID_DEFAULT_MANAGED) ;

        org.apache.archiva.repository.ManagedRepository repoConfig = repositoryRegistry.getManagedRepository(ID_DEFAULT_MANAGED);

        // Setup target (proxied to) repository.
        saveRemoteRepositoryConfig( ID_PROXIED1, "Proxied Repository 1",
            Paths.get( REPOPATH_PROXIED1 ).toUri().toURL().toExternalForm(), "default" );

        // Setup target (proxied to) repository.
        saveRemoteRepositoryConfig( ID_PROXIED2, "Proxied Repository 2",
            Paths.get( REPOPATH_PROXIED2 ).toUri().toURL().toExternalForm(), "default" );


        repositoryRegistry.reload();
        repositoryRegistry.putRepository(repoConfig);


        // Setup the proxy handler.
        //proxyHandler = applicationContext.getBean (RepositoryProxyHandler) lookup( RepositoryProxyHandler.class.getName() );

        proxyHandler = applicationContext.getBean( "repositoryProxyHandler#test", RepositoryProxyHandler.class );
        assertNotNull( proxyRegistry );
        assertTrue(proxyRegistry.getAllHandler( ).get( RepositoryType.MAVEN).contains( proxyHandler ));


        // Setup the wagon mock.
        wagonMockControl = EasyMock.createNiceControl();
        wagonMock = wagonMockControl.createMock( Wagon.class );

        delegate = (WagonDelegate) applicationContext.getBean( "wagon#http", Wagon.class );

        delegate.setDelegate( wagonMock );

        CacheManager.getInstance().clearAll();

        log.info( "\n.\\ {}() \\._________________________________________\n", name );
    }

    protected void assertChecksums( Path expectedFile, String expectedSha1Contents, String expectedMd5Contents )
        throws Exception
    {
        Path sha1File = expectedFile.toAbsolutePath().resolveSibling( expectedFile.getFileName().toString()+ ".sha1" );
        Path md5File = expectedFile.toAbsolutePath().resolveSibling( expectedFile.getFileName().toString() + ".md5" );

        if ( expectedSha1Contents == null )
        {
            assertFalse( "SHA1 File should NOT exist: " + sha1File.toAbsolutePath(), Files.exists(sha1File) );
        }
        else
        {
            assertTrue( "SHA1 File should exist: " + sha1File.toAbsolutePath(), Files.exists(sha1File) );
            String actualSha1Contents = readChecksumFile( sha1File );
            assertEquals( "SHA1 File contents: " + sha1File.toAbsolutePath(), expectedSha1Contents, actualSha1Contents );
        }

        if ( expectedMd5Contents == null )
        {
            assertFalse( "MD5 File should NOT exist: " + md5File.toAbsolutePath(), Files.exists(md5File) );
        }
        else
        {
            assertTrue( "MD5 File should exist: " + md5File.toAbsolutePath(), Files.exists(md5File) );
            String actualMd5Contents = readChecksumFile( md5File );
            assertEquals( "MD5 File contents: " + md5File.toAbsolutePath(), expectedMd5Contents, actualMd5Contents );
        }
    }

    protected void assertFileEquals( Path expectedFile, Path actualFile, Path sourceFile )
        throws Exception
    {
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Actual File should not be null.", actualFile );

        assertTrue( "Check actual file exists.", Files.exists(actualFile) );
        assertTrue("Check expected file exists", Files.exists(expectedFile));
        assertTrue( "Check file is the same.", Files.isSameFile( expectedFile,
            actualFile));
        String expectedContents =
            org.apache.commons.io.FileUtils.readFileToString( sourceFile.toFile(), Charset.defaultCharset() );
        String actualContents =
            org.apache.commons.io.FileUtils.readFileToString( actualFile.toFile(), Charset.defaultCharset() );
        assertEquals( "Check file contents.", expectedContents, actualContents );
    }

    protected void assertNotDownloaded(  StorageAsset downloadedFile )
    {
        assertNull( "Found file: " + downloadedFile + "; but was expecting a failure", downloadedFile );
    }

    @SuppressWarnings( "unchecked" )
    protected void assertNoTempFiles( Path expectedFile )
    {
        Path workingDir = expectedFile.getParent();
        if ( ( workingDir == null ) || !Files.isDirectory( workingDir) )
        {
            return;
        }

        Collection<Path> tmpFiles = null;
        try {
            tmpFiles = Files.list(workingDir).filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".tmp")).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Could not retrieve tmpFiles {}", workingDir);
        }
        if ( tmpFiles!=null && !tmpFiles.isEmpty() )
        {
            StringBuilder emsg = new StringBuilder();
            emsg.append( "Found Temp Files in dir: " ).append( workingDir.toString() );
            for ( Path tfile : tmpFiles )
            {
                emsg.append( "\n   " ).append( tfile.getFileName().toString());
            }
            fail( emsg.toString() );
        }
    }

    /**
     * A faster recursive copy that omits .svn directories.
     *
     * @param sourceDirectory the source directory to copy
     * @param destDirectory   the target location
     * @throws java.io.IOException if there is a copying problem
     * @todo get back into plexus-utils, share with converter module
     */
    protected void copyDirectoryStructure( Path sourceDirectory, Path destDirectory )
        throws IOException
    {
        if ( !Files.exists(sourceDirectory) )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.toAbsolutePath() + ")." );
        }

        Path[] files = Files.list(sourceDirectory).filter(path -> Files.isRegularFile(path)).toArray(Path[]::new);

        String sourcePath = sourceDirectory.toAbsolutePath().toString();

        for ( int i = 0; i < files.length; i++ )
        {
            Path file = files[i];

            String dest = file.toAbsolutePath().toString();

            dest = dest.substring( sourcePath.length() + 1 );

            Path destination = destDirectory.resolve( dest );

            if ( Files.isRegularFile(file) )
            {
                destination = destination.getParent();

                org.apache.commons.io.FileUtils.copyFile( file.toFile(), destination.resolve( file.getFileName() ).toFile(), false );
                // TODO: Change when there is a FileUtils.copyFileToDirectory(file, destination, boolean) option
                //FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( Files.isDirectory(file) )
            {
                if ( !".svn".equals( file.getFileName().toString() ) )
                {
                    if ( !Files.exists(destination))
                    {
                        Files.createDirectories(destination);
                    }

                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.toAbsolutePath() );
            }
        }
    }


    protected ManagedRepositoryContent createRepository( String id, String name, String path, String layout )
        throws Exception
    {
        ManagedRepository repo = BasicManagedRepository.newFilesystemInstance(id, name, Paths.get(path).resolve(id));
        repositoryRegistry.putRepository(repo);
        return repositoryRegistry.getManagedRepository(id).getContent();
    }

    /**
     * Read the first line from the checksum file, and return it (trimmed).
     */
    protected String readChecksumFile( Path checksumFile )
        throws Exception
    {
        FileReader freader = null;
        BufferedReader buf = null;

        try
        {
            freader = new FileReader( checksumFile.toFile() );
            buf = new BufferedReader( freader );
            return buf.readLine();
        }
        finally
        {
            if ( buf != null )
            {
                buf.close();
            }

            if ( freader != null )
            {
                freader.close();
            }
        }
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, boolean disabled )
    {
        saveConnector( sourceRepoId, targetRepoId, ChecksumPolicy.IGNORE, ReleasesPolicy.ALWAYS, SnapshotsPolicy.ALWAYS,
                       CachedFailuresPolicy.NO, disabled );
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, PolicyOption checksumPolicy, PolicyOption releasePolicy,
                                  PolicyOption snapshotPolicy, PolicyOption cacheFailuresPolicy, boolean disabled )
    {
        saveConnector( sourceRepoId, targetRepoId, checksumPolicy, releasePolicy, snapshotPolicy, cacheFailuresPolicy,
                       PropagateErrorsDownloadPolicy.QUEUE, disabled );
    }

    protected void saveConnector( String sourceRepoId, String targetRepoId, PolicyOption checksumPolicy, PolicyOption releasePolicy,
                                  PolicyOption snapshotPolicy, PolicyOption cacheFailuresPolicy, PolicyOption errorPolicy,
                                  boolean disabled )
    {
        saveConnector( sourceRepoId, targetRepoId, checksumPolicy, releasePolicy, snapshotPolicy, cacheFailuresPolicy,
                       errorPolicy, PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT, disabled );
    }

    protected void saveConnector(String sourceRepoId, String targetRepoId, PolicyOption checksumPolicy, PolicyOption releasePolicy,
                                 PolicyOption snapshotPolicy, PolicyOption cacheFailuresPolicy, PolicyOption errorPolicy,
                                 PolicyOption errorOnUpdatePolicy, boolean disabled )
    {
        ProxyConnectorConfiguration connectorConfig = new ProxyConnectorConfiguration();
        connectorConfig.setSourceRepoId( sourceRepoId );
        connectorConfig.setTargetRepoId( targetRepoId );
        connectorConfig.setProxyId(sourceRepoId);
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, checksumPolicy.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, releasePolicy.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, snapshotPolicy.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, cacheFailuresPolicy.getId());
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS, errorPolicy.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS_ON_UPDATE, errorOnUpdatePolicy.getId() );
        connectorConfig.setDisabled( disabled );

        int count = config.getConfiguration().getProxyConnectors().size();
        config.getConfiguration().addProxyConnector( connectorConfig );

        // Proper Triggering ...
        String prefix = "proxyConnectors.proxyConnector(" + count + ")";
        config.triggerChange( prefix + ".sourceRepoId", connectorConfig.getSourceRepoId() );
        config.triggerChange( prefix + ".targetRepoId", connectorConfig.getTargetRepoId() );
        config.triggerChange( prefix + ".proxyId", connectorConfig.getProxyId() );
        config.triggerChange( prefix + ".policies.releases", connectorConfig.getPolicy( "releases", "" ) );
        config.triggerChange( prefix + ".policies.checksum", connectorConfig.getPolicy( "checksum", "" ) );
        config.triggerChange( prefix + ".policies.snapshots", connectorConfig.getPolicy( "snapshots", "" ) );
        config.triggerChange( prefix + ".policies.cache-failures", connectorConfig.getPolicy( "cache-failures", "" ) );
        config.triggerChange( prefix + ".policies.propagate-errors",
                              connectorConfig.getPolicy( "propagate-errors", "" ) );
        config.triggerChange( prefix + ".policies.propagate-errors-on-update",
                              connectorConfig.getPolicy( "propagate-errors-on-update", "" ) );
    }

    protected void saveManagedRepositoryConfig( String id, String name, String path, String layout )
    {
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );
        repoConfig.setLayout( layout );

        repoConfig.setLocation( path );

        int count = config.getConfiguration().getManagedRepositories().size();
        config.getConfiguration().addManagedRepository( repoConfig );

        String prefix = "managedRepositories.managedRepository(" + count + ")";
        config.triggerChange( prefix + ".id", repoConfig.getId() );
        config.triggerChange( prefix + ".name", repoConfig.getName() );
        config.triggerChange( prefix + ".location", repoConfig.getLocation() );
        config.triggerChange( prefix + ".layout", repoConfig.getLayout() );
    }

    protected void saveRemoteRepositoryConfig( String id, String name, String url, String layout )
    {
        RemoteRepositoryConfiguration repoConfig = new RemoteRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );
        repoConfig.setLayout( layout );
        repoConfig.setUrl( url );

        int count = config.getConfiguration().getRemoteRepositories().size();
        config.getConfiguration().addRemoteRepository( repoConfig );

        String prefix = "remoteRepositories.remoteRepository(" + count + ")";
        config.triggerChange( prefix + ".id", repoConfig.getId() );
        config.triggerChange( prefix + ".name", repoConfig.getName() );
        config.triggerChange( prefix + ".url", repoConfig.getUrl() );
        config.triggerChange( prefix + ".layout", repoConfig.getLayout() );
        repositoryRegistry.reload();
    }

    protected Path saveTargetedRepositoryConfig( String id, String originalPath, String targetPath, String layout )
        throws IOException
    {
        Path repoLocation = Paths.get( targetPath );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( Paths.get(originalPath) , repoLocation );

        saveRemoteRepositoryConfig( id, "Target Repo-" + id, targetPath, layout );

        return repoLocation;
    }


    /**
     * Copy the specified resource directory from the src/test/repository/managed/ to
     * the testable directory under target/test-repository/managed/${testName}/
     *
     * @param resourcePath
     * @throws IOException
     */
    protected void setupTestableManagedRepository( String resourcePath )
        throws IOException
    {
        String resourceDir = resourcePath;

        if ( !resourcePath.endsWith( "/" ) )
        {
            int idx = resourcePath.lastIndexOf( '/' );
            resourceDir = resourcePath.substring( 0, idx );
        }

        Path sourceRepoDir = Paths.get( REPOPATH_DEFAULT_MANAGED );
        Path sourceDir = sourceRepoDir.resolve(resourceDir );

        Path destRepoDir = managedDefaultDir;
        Path destDir = destRepoDir.resolve(resourceDir );

        // Cleanout destination dirs.
        if ( Files.exists(destDir))
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( destDir );
        }

        // Make the destination dir.
        Files.createDirectories(destDir);

        // Test the source dir.
        if ( !Files.exists(sourceDir) )
        {
            // This is just a warning.
            log.error( "[WARN] Skipping setup of testable managed repository, source dir does not exist: {}", //
                       sourceDir );
        }
        else
        {

            // Test that the source is a dir.
            if ( !Files.isDirectory( sourceDir) )
            {
                fail( "Unable to setup testable managed repository, source is not a directory: " + sourceDir );
            }

            // Copy directory structure.
            copyDirectoryStructure( sourceDir, destDir );
        }
    }

    protected void setManagedNewerThanRemote( Path managedFile, Path remoteFile )
    {
        setManagedNewerThanRemote( managedFile, remoteFile, 55000 );
    }

    protected void setManagedNewerThanRemote( Path managedFile, Path remoteFile, long time )
    {
        assertTrue( "Managed File should exist: ", Files.exists(managedFile) );
        assertTrue( "Remote File should exist: ", Files.exists(remoteFile) );

        try
        {
            Files.setLastModifiedTime( managedFile,
                FileTime.from(Files.getLastModifiedTime( remoteFile ).toMillis() + time, TimeUnit.MILLISECONDS ));
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }

        try
        {
            assertTrue( Files.getLastModifiedTime( managedFile).compareTo( Files.getLastModifiedTime( remoteFile )) > 0);
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }
    }

    protected void setManagedOlderThanRemote( Path managedFile, Path remoteFile )
    {
        setManagedOlderThanRemote( managedFile, remoteFile, 55000 );
    }

    protected void setManagedOlderThanRemote( Path  managedFile, Path remoteFile, long time )
    {
        assertTrue( "Managed File should exist: ", Files.exists(managedFile) );
        assertTrue( "Remote File should exist: ", Files.exists(remoteFile) );

        try
        {
            Files.setLastModifiedTime( managedFile,
                FileTime.from(Files.getLastModifiedTime( remoteFile ).toMillis() - time, TimeUnit.MILLISECONDS ));
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }

        try
        {
            assertTrue( Files.getLastModifiedTime( managedFile ).compareTo(Files.getLastModifiedTime( remoteFile  )) < 0 );
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }

    }

    protected void assertNotModified( Path file, long expectedModificationTime )
    {
        try
        {
            assertEquals( "File <" + file.toAbsolutePath() + "> not have been modified.", expectedModificationTime,
                          Files.getLastModifiedTime( file ).toMillis());
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }
    }


    protected void assertNotExistsInManagedDefaultRepo( Path testFile )
        throws Exception
    {
        Path managedDefaultPath = managedDefaultDir;

        assertTrue( "Unit Test Failure: File <" + testFile
                        + "> should be have been defined within the managed default path of <" + managedDefaultPath
                        + ">", testFile.startsWith( managedDefaultPath ) );

        assertFalse( "File < " + testFile + "> should not exist in managed default repository.", Files.exists(testFile) );
    }

    protected static Date getFutureDate()
        throws ParseException
    {
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.YEAR, 1 );
        return cal.getTime();
    }

    protected static Date getPastDate()
        throws ParseException
    {
        return new SimpleDateFormat( "yyyy-MM-dd", Locale.US ).parse( "2000-01-01" );
    }
}
