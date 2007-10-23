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

import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;

/**
 * CacheFailuresTransferTest
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CacheFailuresTransferTest
    extends AbstractProxyTestCase
{
    public void testGetWithCacheFailuresOn()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( managedDefaultDir.getAbsoluteFile(), path );
        setupTestableManagedRepository( path );
        
        assertNotExistsInManagedDefaultRepo( expectedFile );
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied1", "Bad Proxied 1", "test://bad.machine.com/repo/", "default" );
        saveRemoteRepositoryConfig( "badproxied2", "Bad Proxied 2", "test://bad.machine.com/repo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.CACHED );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.CACHED );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new ResourceDoesNotExistException( "resource does not exist." ), 2 );

        wagonMockControl.replay();

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        wagonMockControl.verify();
        
        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetWithCacheFailuresOff()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( managedDefaultDir.getAbsoluteFile(), path );
        
        setupTestableManagedRepository( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied1", "Bad Proxied 1", "test://bad.machine.com/repo/", "default" );
        saveRemoteRepositoryConfig( "badproxied2", "Bad Proxied 2", "test://bad.machine.com/repo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new ResourceDoesNotExistException( "resource does not exist." ), 2 );

        wagonMockControl.replay();

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );

        wagonMockControl.verify();
        assertNoTempFiles( expectedFile );
    }

    public void testGetWhenInBothProxiedButFirstCacheFailure()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        String url = PathUtil.toUrl( REPOPATH_PROXIED1 + "/" + path );

        // Intentionally set failure on url in proxied1 (for test)
        UrlFailureCache failurlCache = lookupUrlFailureCache();
        failurlCache.cacheFailure( url );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.CACHED );
        saveConnector( ID_DEFAULT_MANAGED, "proxied2", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.CACHED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        // Validate that file actually came from proxied2 (as intended).
        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }
}
