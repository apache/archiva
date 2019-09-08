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

import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.apache.archiva.policies.urlcache.UrlFailureCache;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.easymock.EasyMock;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * CacheFailuresTransferTest
 *
 *
 */
public class CacheFailuresTransferTest
    extends AbstractProxyTestCase
{
    // TODO: test some hard failures (eg TransferFailedException)
    // TODO: test the various combinations of fetchFrom* (note: need only test when caching is enabled)

    @Inject
    UrlFailureCache urlFailureCache;

    @Test
    public void testGetWithCacheFailuresOn()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        Path expectedFile = managedDefaultDir.resolve( path );
        setupTestableManagedRepository( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied1", "Bad Proxied 1", "http://bad.machine.com/repo/", "default" );
        saveRemoteRepositoryConfig( "badproxied2", "Bad Proxied 2", "http://bad.machine.com/anotherrepo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1", ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES, false );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2", ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES, false );

        wagonMock.get( EasyMock.eq( path ), EasyMock.anyObject( File.class ));

        EasyMock.expectLastCall().andThrow( new ResourceDoesNotExistException( "resource does not exist." ) ).times( 2 );


        wagonMockControl.replay();

        //noinspection UnusedAssignment
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        wagonMockControl.verify();

        // Second attempt to download same artifact use cache
        wagonMockControl.reset();
        wagonMockControl.replay();
        downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );
        wagonMockControl.verify();

        assertNotDownloaded( downloadedFile);
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetWithCacheFailuresOff()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        Path expectedFile = managedDefaultDir.resolve( path );
        setupTestableManagedRepository( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied1", "Bad Proxied 1", "http://bad.machine.com/repo/", "default" );
        saveRemoteRepositoryConfig( "badproxied2", "Bad Proxied 2", "http://bad.machine.com/anotherrepo/", "default" );


        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1", ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO, false );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2", ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO, false );

        wagonMock.get( EasyMock.eq( path ), EasyMock.anyObject( File.class ));
        EasyMock.expectLastCall().andThrow( new ResourceDoesNotExistException( "resource does not exist." ) ).times( 2 );

        wagonMockControl.replay();

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        wagonMockControl.verify();

        // Second attempt to download same artifact DOES NOT use cache
        wagonMockControl.reset();

        wagonMock.get( EasyMock.eq( path ), EasyMock.anyObject( File.class ));
        EasyMock.expectLastCall().andThrow( new ResourceDoesNotExistException( "resource does not exist." ) ).times( 2 );

        wagonMockControl.replay();

        downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        wagonMockControl.verify();

        assertNotDownloaded( downloadedFile);
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetWhenInBothProxiedButFirstCacheFailure()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );
        Path expectedFile = managedDefaultDir.resolve(path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        Files.deleteIfExists(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        String url = PathUtil.toUrl( REPOPATH_PROXIED1 + "/" + path );

        // Intentionally set failure on url in proxied1 (for test)
        UrlFailureCache failurlCache = lookupUrlFailureCache();
        failurlCache.cacheFailure( url );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES, false );
        saveConnector( ID_DEFAULT_MANAGED, "proxied2", ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES, false );

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        // Validate that file actually came from proxied2 (as intended).
        Path proxied2File = Paths.get( REPOPATH_PROXIED2, path );
        assertNotNull(downloadedFile);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxied2File );
        assertNoTempFiles( expectedFile );
    }

    protected UrlFailureCache lookupUrlFailureCache()
        throws Exception
    {
        assertNotNull( "URL Failure Cache cannot be null.", urlFailureCache );
        return urlFailureCache;
    }
}
