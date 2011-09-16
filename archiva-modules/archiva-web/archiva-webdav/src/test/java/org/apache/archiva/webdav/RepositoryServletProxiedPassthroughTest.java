package org.apache.archiva.webdav;

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

import java.io.File;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * RepositoryServlet Tests, Proxied, Get of resources that are not artifacts or metadata, with varying policy settings.
 * 
 * @version $Id: RepositoryServletProxiedReleasePolicyTest.java 661174 2008-05-29 01:49:41Z jdumay $
 */
public class RepositoryServletProxiedPassthroughTest
    extends AbstractRepositoryServletProxiedTestCase
{
    private static final String CONTENT_SHA1 = "2aab0a51c04c9023636852f3e63a68034ba10142";

    private static final String PATH_SHA1 = "org/apache/archiva/test/1.0/test-1.0.jar.sha1";

    private static final String CONTENT_ASC =
        "-----BEGIN PGP SIGNATURE-----\n" + "Version: GnuPG v1.4.8 (Darwin)\n" + "\n"
            + "iEYEABECAAYFAkiAIVgACgkQxbsDNW2stZZjyACeK3LW+ZDeawCyJj4XgvUaJkNh\n"
            + "qIEAoIUiijY4Iw82RWOT75Rt3yZuY6ZI\n" + "=WLkm\n" + "-----END PGP SIGNATURE-----\n";

    private static final String PATH_ASC = "org/apache/archiva/test/1.0/test-1.0.jar.asc";

    @Before
    public void setup()
        throws Exception
    {
        super.setUp();
    }

    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testGetProxiedManagedNewerSha1()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_MANAGED_CONTENTS, HAS_MANAGED_COPY, ( NEWER * OVER_ONE_DAY ), PATH_SHA1,
                                  CONTENT_SHA1 );
    }

    @Test
    public void testGetProxiedManagedOlderSha1()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_REMOTE_CONTENTS, HAS_MANAGED_COPY, ( OLDER * OVER_ONE_DAY ), PATH_SHA1,
                                  CONTENT_SHA1 );
    }

    @Test
    public void testGetProxiedNoManagedContentSha1()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_REMOTE_CONTENTS, NO_MANAGED_COPY, PATH_SHA1, CONTENT_SHA1 );
    }

    @Test
    public void testGetProxiedEqualSha1()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_MANAGED_CONTENTS, HAS_MANAGED_COPY, PATH_SHA1, CONTENT_SHA1 );
    }

    @Test
    public void testGetProxiedManagedNewerAsc()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_MANAGED_CONTENTS, HAS_MANAGED_COPY, ( NEWER * OVER_ONE_DAY ), PATH_ASC,
                                  CONTENT_ASC );
    }

    @Test
    public void testGetProxiedManagedOlderAsc()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_REMOTE_CONTENTS, HAS_MANAGED_COPY, ( OLDER * OVER_ONE_DAY ), PATH_ASC,
                                  CONTENT_ASC );
    }

    @Test
    public void testGetProxiedNoManagedContentAsc()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_REMOTE_CONTENTS, NO_MANAGED_COPY, PATH_ASC, CONTENT_ASC );
    }

    @Test
    public void testGetProxiedEqualAsc()
        throws Exception
    {
        assertGetProxiedResource( EXPECT_MANAGED_CONTENTS, HAS_MANAGED_COPY, PATH_ASC, CONTENT_ASC );
    }

    private void assertGetProxiedResource( int expectation, boolean hasManagedCopy, String path, String content )
        throws Exception
    {
        assertGetProxiedResource( expectation, hasManagedCopy, 0, path, content );
    }

    private void assertGetProxiedResource( int expectation, boolean hasManagedCopy, long deltaManagedToRemoteTimestamp,
                                           String path, String contents )
        throws Exception
    {
        // --- Setup
        setupCentralRemoteRepo();
        setupCleanInternalRepo();

        String expectedRemoteContents = contents;
        String expectedManagedContents = null;
        File remoteFile = populateRepo( remoteCentral, path, expectedRemoteContents );

        if ( hasManagedCopy )
        {
            expectedManagedContents = contents;
            File managedFile = populateRepo( repoRootInternal, path, expectedManagedContents );
            managedFile.setLastModified( remoteFile.lastModified() + deltaManagedToRemoteTimestamp );
        }

        setupConnector( REPOID_INTERNAL, remoteCentral );
        saveConfiguration();

        // --- Execution
        // process the response code later, not via an exception.
        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + path );
        WebResponse response = sc.getResponse( request );

        // --- Verification

        switch ( expectation )
        {
            case EXPECT_MANAGED_CONTENTS:
                assertResponseOK( response );
                assertTrue( "Invalid Test Case: Can't expect managed contents with "
                    + "test that doesn't have a managed copy in the first place.", hasManagedCopy );
                assertEquals( "Expected managed file contents", expectedManagedContents, response.getText() );
                break;
            case EXPECT_REMOTE_CONTENTS:
                assertResponseOK( response, path );
                assertEquals( "Expected remote file contents", expectedRemoteContents, response.getText() );
                break;
            case EXPECT_NOT_FOUND:
                assertResponseNotFound( response );
                assertManagedFileNotExists( repoRootInternal, path );
                break;
        }
    }
}
