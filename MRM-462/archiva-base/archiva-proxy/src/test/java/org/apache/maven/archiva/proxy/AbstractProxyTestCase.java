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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusTestCase;
import org.easymock.MockControl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * AbstractProxyTestCase
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractProxyTestCase
    extends PlexusTestCase
{
    protected static final String ID_LEGACY_PROXIED = "legacy-proxied";

    protected static final String ID_PROXIED1 = "proxied1";

    protected static final String ID_PROXIED1_TARGET = "proxied1-target";

    protected static final String ID_PROXIED2 = "proxied2";

    protected static final String ID_PROXIED2_TARGET = "proxied2-target";

    protected static final String ID_DEFAULT_MANAGED = "default-managed-repository";

    protected static final String ID_LEGACY_MANAGED = "legacy-managed-repository";

    protected static final String REPOPATH_PROXIED_LEGACY = "src/test/repositories/legacy-proxied";

    protected static final String REPOPATH_PROXIED1 = "src/test/repositories/proxied1";

    protected static final String REPOPATH_PROXIED1_TARGET = "target/test-repository/proxied1";

    protected static final String REPOPATH_PROXIED2 = "src/test/repositories/proxied2";

    protected static final String REPOPATH_PROXIED2_TARGET = "target/test-repository/proxied2";

    protected static final String REPOPATH_DEFAULT_MANAGED = "src/test/repositories/managed";

    protected static final String REPOPATH_DEFAULT_MANAGED_TARGET = "target/test-repository/managed";

    protected static final String REPOPATH_LEGACY_MANAGED = "src/test/repositories/legacy-managed";

    protected static final String REPOPATH_LEGACY_MANAGED_TARGET = "target/test-repository/legacy-managed";

    protected MockControl wagonMockControl;

    protected Wagon wagonMock;

    protected RepositoryProxyConnectors proxyHandler;

    protected ArchivaRepository managedDefaultRepository;

    protected File managedDefaultDir;

    protected ArchivaRepository managedLegacyRepository;

    protected File managedLegacyDir;

    protected BidirectionalRepositoryLayoutFactory layoutFactory;

    protected MockConfiguration config;

    protected void assertChecksums( File expectedFile, String expectedSha1Contents, String expectedMd5Contents )
        throws Exception
    {
        File sha1File = new File( expectedFile.getAbsolutePath() + ".sha1" );
        File md5File = new File( expectedFile.getAbsolutePath() + ".md5" );

        if ( expectedSha1Contents == null )
        {
            assertFalse( "SHA1 File should NOT exist: " + sha1File.getPath(), sha1File.exists() );
        }
        else
        {
            assertTrue( "SHA1 File should exist: " + sha1File.getPath(), sha1File.exists() );
            String actualSha1Contents = readChecksumFile( sha1File );
            assertEquals( "SHA1 File contents: " + sha1File.getPath(), expectedSha1Contents, actualSha1Contents );
        }

        if ( expectedMd5Contents == null )
        {
            assertFalse( "MD5 File should NOT exist: " + md5File.getPath(), md5File.exists() );
        }
        else
        {
            assertTrue( "MD5 File should exist: " + md5File.getPath(), md5File.exists() );
            String actualMd5Contents = readChecksumFile( md5File );
            assertEquals( "MD5 File contents: " + md5File.getPath(), expectedMd5Contents, actualMd5Contents );
        }
    }

    protected void assertFileEquals( File expectedFile, File actualFile, File sourceFile )
        throws Exception
    {
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Actual File should not be null.", actualFile );

        assertTrue( "Check file exists.", actualFile.exists() );
        assertEquals( "Check file path matches.", expectedFile.getAbsolutePath(), actualFile.getAbsolutePath() );

        String expectedContents = FileUtils.readFileToString( sourceFile, null );
        String actualContents = FileUtils.readFileToString( actualFile, null );
        assertEquals( "Check file contents.", expectedContents, actualContents );
    }

    protected void assertNotDownloaded( File downloadedFile )
    {
        assertNull( "Found file: " + downloadedFile + "; but was expecting a failure", downloadedFile );
    }

    protected void assertNoTempFiles( File expectedFile )
    {
        File workingDir = expectedFile.getParentFile();
        if ( ( workingDir == null ) || !workingDir.isDirectory() )
        {
            return;
        }

        Collection tmpFiles = FileUtils.listFiles( workingDir, new String[]{"tmp"}, false );
        if ( !tmpFiles.isEmpty() )
        {
            StringBuffer emsg = new StringBuffer();
            emsg.append( "Found Temp Files in dir: " ).append( workingDir.getPath() );
            Iterator it = tmpFiles.iterator();
            while ( it.hasNext() )
            {
                File tfile = (File) it.next();
                emsg.append( "\n   " ).append( tfile.getName() );
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
    protected void copyDirectoryStructure( File sourceDirectory, File destDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFile( file, new File( destination, file.getName() ), false );
                // TODO: Change when there is a FileUtils.copyFileToDirectory(file, destination, boolean) option
                //FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() )
            {
                if ( !".svn".equals( file.getName() ) )
                {
                    if ( !destination.exists() && !destination.mkdirs() )
                    {
                        throw new IOException(
                            "Could not create destination directory '" + destination.getAbsolutePath() + "'." );
                    }

                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
    }

    protected ArtifactReference createArtifactReference( String layoutType, String path )
        throws Exception
    {
        BidirectionalRepositoryLayout layout = layoutFactory.getLayout( layoutType );
        ArchivaArtifact artifact = layout.toArtifact( path );
        ArtifactReference ref = new ArtifactReference();
        ref.setGroupId( artifact.getGroupId() );
        ref.setArtifactId( artifact.getArtifactId() );
        ref.setVersion( artifact.getVersion() );
        ref.setClassifier( artifact.getClassifier() );
        ref.setType( artifact.getType() );
        return ref;
    }

    protected ArchivaRepository createManagedLegacyRepository()
    {
        return createRepository( "src/test/repositories/legacy-managed", "testManagedLegacyRepo",
                                 "Test Managed (Legacy) Repository", "legacy" );
    }

    protected ProjectReference createMetadataReference( String layoutType, String path )
        throws Exception
    {
        BidirectionalRepositoryLayout layout = layoutFactory.getLayout( layoutType );
        ProjectReference metadata = layout.toProjectReference( path );
        return metadata;
    }

    protected ArchivaRepository createProxiedLegacyRepository()
    {
        return createRepository( "src/test/repositories/legacy-proxied", "testProxiedLegacyRepo",
                                 "Test Proxied (Legacy) Repository", "legacy" );
    }

    protected ManagedRepositoryConfiguration createRepoConfig( ArchivaRepository repo )
    {
        return createRepoConfig( repo.getId(), repo.getName(), repo.getUrl().toString(), repo.getLayoutType() );
    }

    protected ManagedRepositoryConfiguration createRepoConfig( String id, String name, String path, String layout )
    {
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );

        repoConfig.setLocation( path );
        repoConfig.setLayout( layout );

        return repoConfig;
    }

    protected ArchivaRepository createRepository( String id, String name, String path, String layout )
    {
        ArchivaRepository repo = new ArchivaRepository( id, name, PathUtil.toUrl( path ) );
        repo.getModel().setLayoutName( layout );

        return repo;
    }

    protected UrlFailureCache lookupUrlFailureCache()
        throws Exception
    {
        UrlFailureCache failurlCache = (UrlFailureCache) lookup( UrlFailureCache.class.getName(), "default" );
        assertNotNull( "URL Failure Cache cannot be null.", failurlCache );
        return failurlCache;
    }

    /**
     * Read the first line from the checksum file, and return it (trimmed).
     */
    protected String readChecksumFile( File checksumFile )
        throws Exception
    {
        FileReader freader = null;
        BufferedReader buf = null;

        try
        {
            freader = new FileReader( checksumFile );
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

    protected void saveConnector( String sourceRepoId, String targetRepoId, String checksumPolicy, String releasePolicy,
                                  String snapshotPolicy, String cacheFailuresPolicy )
    {
        ProxyConnectorConfiguration connectorConfig = new ProxyConnectorConfiguration();
        connectorConfig.setSourceRepoId( sourceRepoId );
        connectorConfig.setTargetRepoId( targetRepoId );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, checksumPolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, releasePolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, snapshotPolicy );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, cacheFailuresPolicy );

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
    }

    protected void saveManagedRepositoryConfig( String id, String name, String path, String layout )
    {
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );
        repoConfig.setLayout( layout );

        repoConfig.setLocation( path );

        config.getConfiguration().addManagedRepository( repoConfig );

        config.triggerChange( "repository", "" );
    }

    protected void saveRemoteRepositoryConfig( String id, String name, String path, String layout )
    {
        RemoteRepositoryConfiguration repoConfig = new RemoteRepositoryConfiguration();

        repoConfig.setId( id );
        repoConfig.setName( name );
        repoConfig.setLayout( layout );

        repoConfig.setUrl( path );

        config.getConfiguration().addRemoteRepository( repoConfig );

        config.triggerChange( "repository", "" );
    }

    protected File saveTargetedRepositoryConfig( String id, String originalPath, String targetPath, String layout )
        throws IOException
    {
        File repoLocation = getTestFile( targetPath );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( getTestFile( originalPath ), repoLocation );

        saveRemoteRepositoryConfig( id, "Target Repo-" + id, targetPath, layout );

        return repoLocation;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        layoutFactory = (BidirectionalRepositoryLayoutFactory) lookup( BidirectionalRepositoryLayoutFactory.class
            .getName() );

        config = (MockConfiguration) lookup( ArchivaConfiguration.class.getName(), "mock" );

        // Setup source repository (using default layout)
        File repoLocation = getTestFile( REPOPATH_DEFAULT_MANAGED_TARGET );
        // faster only to delete this one before copying, the others are done case by case
        FileUtils.deleteDirectory( new File( repoLocation, "org/apache/maven/test/get-merged-metadata" ) );
        copyDirectoryStructure( getTestFile( REPOPATH_DEFAULT_MANAGED ), repoLocation );

        managedDefaultRepository = createRepository( ID_DEFAULT_MANAGED, "Default Managed Repository",
                                                     REPOPATH_DEFAULT_MANAGED_TARGET, "default" );

        managedDefaultDir = new File( managedDefaultRepository.getUrl().getPath() );

        ManagedRepositoryConfiguration repoConfig = createRepoConfig( managedDefaultRepository );

        config.getConfiguration().addManagedRepository( repoConfig );

        // Setup source repository (using legacy layout)
        repoLocation = getTestFile( REPOPATH_LEGACY_MANAGED_TARGET );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( getTestFile( REPOPATH_LEGACY_MANAGED ), repoLocation );

        managedLegacyRepository = createRepository( ID_LEGACY_MANAGED, "Legacy Managed Repository",
                                                    REPOPATH_LEGACY_MANAGED_TARGET, "legacy" );

        managedLegacyDir = new File( managedLegacyRepository.getUrl().getPath() );

        repoConfig = createRepoConfig( managedLegacyRepository );

        config.getConfiguration().addManagedRepository( repoConfig );

        // Setup target (proxied to) repository.
        saveRemoteRepositoryConfig( ID_PROXIED1, "Proxied Repository 1",
                                    new File( REPOPATH_PROXIED1 ).toURL().toExternalForm(), "default" );

        // Setup target (proxied to) repository.
        saveRemoteRepositoryConfig( ID_PROXIED2, "Proxied Repository 2",
                                    new File( REPOPATH_PROXIED2 ).toURL().toExternalForm(), "default" );

        // Setup target (proxied to) repository using legacy layout.
        saveRemoteRepositoryConfig( ID_LEGACY_PROXIED, "Proxied Legacy Repository",
                                    new File( REPOPATH_PROXIED_LEGACY ).toURL().toExternalForm(), "legacy" );

        // Setup the proxy handler.
        proxyHandler = (RepositoryProxyConnectors) lookup( RepositoryProxyConnectors.class.getName() );

        // Setup the wagon mock.
        wagonMockControl = MockControl.createNiceControl( Wagon.class );
        wagonMock = (Wagon) wagonMockControl.getMock();
        WagonDelegate delegate = (WagonDelegate) lookup( Wagon.ROLE, "test" );
        delegate.setDelegate( wagonMock );

        System.out.println( "\n.\\ " + getName() + "() \\._________________________________________\n" );
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
