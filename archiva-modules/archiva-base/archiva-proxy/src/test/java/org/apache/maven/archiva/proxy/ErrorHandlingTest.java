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

import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.PropagateErrorsDownloadPolicy;
import org.apache.maven.archiva.policies.PropagateErrorsOnUpdateDownloadPolicy;
import org.apache.maven.archiva.policies.ProxyDownloadException;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.junit.Test;

import java.io.File;

/**
 * ErrorHandlingTest
 *
 * @version $Id$
 */
public class ErrorHandlingTest
    extends AbstractProxyTestCase
{
    private static final String PATH_IN_BOTH_REMOTES_NOT_LOCAL =
        "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";

    private static final String PATH_IN_BOTH_REMOTES_AND_LOCAL =
        "org/apache/maven/test/get-on-multiple-repos/1.0/get-on-multiple-repos-1.0.pom";

    private static final String ID_MOCKED_PROXIED1 = "badproxied1";

    private static final String NAME_MOCKED_PROXIED1 = "Bad Proxied 1";

    private static final String ID_MOCKED_PROXIED2 = "badproxied2";

    private static final String NAME_MOCKED_PROXIED2 = "Bad Proxied 2";

    @Test
    public void testPropagateErrorImmediatelyWithErrorThenSuccess()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED1 );
    }

    @Test
    public void testPropagateErrorImmediatelyWithNotFoundThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.STOP );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED2 );
    }

    @Test
    public void testPropagateErrorImmediatelyWithSuccessThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.STOP );

        confirmSuccess( path, expectedFile, REPOPATH_PROXIED1 );
    }

    @Test
    public void testPropagateErrorImmediatelyWithNotFoundThenSuccess()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false  );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        confirmSuccess( path, expectedFile, REPOPATH_PROXIED2 );
    }

    @Test
    public void testPropagateErrorAtEndWithErrorThenSuccess()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false  );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED1 );
    }

    @Test
    public void testPropagateErrorAtEndWithSuccessThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false  );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE );

        confirmSuccess( path, expectedFile, REPOPATH_PROXIED1 );
    }

    @Test
    public void testPropagateErrorAtEndWithNotFoundThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED2 );
    }

    @Test
    public void testPropagateErrorAtEndWithErrorThenNotFound()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE );

        simulateGetError( path, expectedFile, createTransferException() );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED1 );
    }

    @Test
    public void testPropagateErrorAtEndWithErrorThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE );

        simulateGetError( path, expectedFile, createTransferException() );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmFailures( path, new String[]{ID_MOCKED_PROXIED1, ID_MOCKED_PROXIED2} );
    }

    @Test
    public void testPropagateErrorAtEndWithNotFoundThenSuccess()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false  );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        confirmSuccess( path, expectedFile, REPOPATH_PROXIED2 );
    }

    @Test
    public void testIgnoreErrorWithErrorThenSuccess()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false  );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSuccess( path, expectedFile, REPOPATH_PROXIED2 );
    }

    @Test
    public void testIgnoreErrorWithSuccessThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false  );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE );

        confirmSuccess( path, expectedFile, REPOPATH_PROXIED1 );
    }

    @Test
    public void testIgnoreErrorWithNotFoundThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
    }

    @Test
    public void testIgnoreErrorWithErrorThenNotFound()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE );

        simulateGetError( path, expectedFile, createTransferException() );

        simulateGetError( path, expectedFile, createResourceNotFoundException() );

        confirmNotDownloadedNoError( path );
    }

    @Test
    public void testIgnoreErrorWithErrorThenError()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE );

        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE );

        simulateGetError( path, expectedFile, createTransferException() );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
    }

    @Test
    public void testPropagateOnUpdateAlwaysArtifactNotPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED1 );
    }

    @Test
    public void testPropagateOnUpdateAlwaysArtifactPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_AND_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFilePresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );

        simulateGetIfNewerError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED1 );
    }

    @Test
    public void testPropagateOnUpdateAlwaysQueueArtifactNotPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );

        simulateGetError( path, expectedFile, createTransferException() );
        simulateGetError( path, expectedFile, createTransferException() );

        confirmFailures( path, new String[] { ID_MOCKED_PROXIED1, ID_MOCKED_PROXIED2 } );
    }

    @Test
    public void testPropagateOnUpdateAlwaysQueueArtifactPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_AND_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFilePresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );

        simulateGetIfNewerError( path, expectedFile, createTransferException() );
        simulateGetIfNewerError( path, expectedFile, createTransferException() );

        confirmFailures( path, new String[] { ID_MOCKED_PROXIED1, ID_MOCKED_PROXIED2 } );
    }

    @Test
    public void testPropagateOnUpdateAlwaysIgnoreArtifactNotPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );

        simulateGetError( path, expectedFile, createTransferException() );
        simulateGetError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
    }

    @Test
    public void testPropagateOnUpdateAlwaysIgnoreArtifactPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_AND_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFilePresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.ALWAYS );

        simulateGetIfNewerError( path, expectedFile, createTransferException() );
        simulateGetIfNewerError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
        assertTrue( expectedFile.exists() );
    }

    @Test
    public void testPropagateOnUpdateNotPresentArtifactNotPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        simulateGetError( path, expectedFile, createTransferException() );

        confirmSingleFailure( path, ID_MOCKED_PROXIED1 );
    }

    @Test
    public void testPropagateOnUpdateNotPresentArtifactPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_AND_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFilePresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.STOP,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        simulateGetIfNewerError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
        assertTrue( expectedFile.exists() );
    }

    @Test
    public void testPropagateOnUpdateNotPresentQueueArtifactNotPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        simulateGetError( path, expectedFile, createTransferException() );
        simulateGetError( path, expectedFile, createTransferException() );

        confirmFailures( path, new String[] { ID_MOCKED_PROXIED1, ID_MOCKED_PROXIED2 } );
    }

    @Test
    public void testPropagateOnUpdateNotPresentQueueArtifactPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_AND_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFilePresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.QUEUE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        simulateGetIfNewerError( path, expectedFile, createTransferException() );
        simulateGetIfNewerError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
        assertTrue( expectedFile.exists() );
    }

    @Test
    public void testPropagateOnUpdateNotPresentIgnoreArtifactNotPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_NOT_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFileNotPresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        simulateGetError( path, expectedFile, createTransferException() );
        simulateGetError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
    }

    @Test
    public void testPropagateOnUpdateNotPresentIgnoreArtifactPresent()
        throws Exception
    {
        String path = PATH_IN_BOTH_REMOTES_AND_LOCAL;
        File expectedFile = setupRepositoriesWithLocalFilePresent( path );

        createMockedProxyConnector( ID_MOCKED_PROXIED1, NAME_MOCKED_PROXIED1, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );
        createMockedProxyConnector( ID_MOCKED_PROXIED2, NAME_MOCKED_PROXIED2, PropagateErrorsDownloadPolicy.IGNORE,
                                    PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        simulateGetIfNewerError( path, expectedFile, createTransferException() );
        simulateGetIfNewerError( path, expectedFile, createTransferException() );

        confirmNotDownloadedNoError( path );
        assertTrue( expectedFile.exists() );
    }

    // ------------------------------------------
    // HELPER METHODS
    // ------------------------------------------

    private void createMockedProxyConnector( String id, String name, String errorPolicy )
    {
        saveRemoteRepositoryConfig( id, name, "test://bad.machine.com/repo/", "default" );
        saveConnector( ID_DEFAULT_MANAGED, id, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS, SnapshotsPolicy.ALWAYS,
                       CachedFailuresPolicy.NO, errorPolicy, false );
    }

    private void createMockedProxyConnector( String id, String name, String errorPolicy, String errorOnUpdatePolicy )
    {
        saveRemoteRepositoryConfig( id, name, "test://bad.machine.com/repo/", "default" );
        saveConnector( ID_DEFAULT_MANAGED, id, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS, SnapshotsPolicy.ALWAYS,
                       CachedFailuresPolicy.NO, errorPolicy, errorOnUpdatePolicy, false );
    }

    private File setupRepositoriesWithLocalFileNotPresent( String path )
        throws Exception
    {
        setupTestableManagedRepository( path );

        File file = new File( managedDefaultDir, path );

        assertNotExistsInManagedDefaultRepo( file );

        return file;
    }

    private File setupRepositoriesWithLocalFilePresent( String path )
        throws Exception
    {
        setupTestableManagedRepository( path );

        File file = new File( managedDefaultDir, path );

        assertTrue( file.exists() );

        return file;
    }

    private void simulateGetError( String path, File expectedFile, Exception throwable )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        wagonMock.get( path, createExpectedTempFile( expectedFile ) );
        wagonMockControl.setMatcher(customWagonGetMatcher);
        wagonMockControl.setThrowable( throwable, 1 );
    }

    private void simulateGetIfNewerError( String path, File expectedFile, TransferFailedException exception )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        wagonMock.getIfNewer( path, createExpectedTempFile( expectedFile ), expectedFile.lastModified() );
        wagonMockControl.setMatcher(customWagonGetIfNewerMatcher);
        wagonMockControl.setThrowable( exception, 1 );
    }

    private File createExpectedTempFile( File expectedFile )
    {
        return new File( managedDefaultDir, expectedFile.getName() + ".tmp" ).getAbsoluteFile();
    }

    private void confirmSingleFailure( String path, String id )
        throws LayoutException
    {
        confirmFailures( path, new String[]{id} );
    }

    private void confirmFailures( String path, String[] ids )
        throws LayoutException
    {
        wagonMockControl.replay();

        // Attempt the proxy fetch.
        File downloadedFile = null;
        try
        {
            downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository,
                                                            managedDefaultRepository.toArtifactReference( path ) );
            fail( "Proxy should not have succeeded" );
        }
        catch ( ProxyDownloadException e )
        {
            assertEquals( ids.length, e.getFailures().size() );
            for ( String id : ids )
            {
                assertTrue( e.getFailures().keySet().contains( id ) );
            }
        }

        wagonMockControl.verify();

        assertNotDownloaded( downloadedFile );
    }

    private void confirmSuccess( String path, File expectedFile, String basedir )
        throws Exception
    {
        File downloadedFile = performDownload( path );

        File proxied1File = new File( basedir, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
    }

    private void confirmNotDownloadedNoError( String path )
        throws Exception
    {
        File downloadedFile = performDownload( path );

        assertNotDownloaded( downloadedFile );
    }

    private File performDownload( String path )
        throws ProxyDownloadException, LayoutException
    {
        wagonMockControl.replay();

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository,
                                                             managedDefaultRepository.toArtifactReference( path ) );

        wagonMockControl.verify();
        return downloadedFile;
    }

    private static TransferFailedException createTransferException()
    {
        return new TransferFailedException( "test download exception" );
    }

    private static ResourceDoesNotExistException createResourceNotFoundException()
    {
        return new ResourceDoesNotExistException( "test download not found" );
    }
}
