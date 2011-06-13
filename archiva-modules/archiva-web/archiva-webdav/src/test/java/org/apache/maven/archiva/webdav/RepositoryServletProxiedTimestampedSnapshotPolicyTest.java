package org.apache.maven.archiva.webdav;

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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * RepositoryServlet Tests, Proxied, Get of Timestamped Snapshot Artifacts, with varying policy settings. 
 *
 * @version $Id$
 */
public class RepositoryServletProxiedTimestampedSnapshotPolicyTest
    extends AbstractRepositoryServletProxiedTestCase
{

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
    public void testGetProxiedSnapshotsArtifactPolicyAlwaysManagedNewer()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, SnapshotsPolicy.ALWAYS,
                                                     HAS_MANAGED_COPY, ( NEWER * OVER_ONE_DAY ) );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyAlwaysManagedOlder()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.ALWAYS, HAS_MANAGED_COPY,
                                                     ( OLDER * OVER_ONE_DAY ) );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyAlwaysNoManagedContent()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.ALWAYS, NO_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyDailyFail()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, SnapshotsPolicy.DAILY, HAS_MANAGED_COPY,
                                                     ( NEWER * ONE_MINUTE ) );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyDailyNoManagedContent()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.DAILY, NO_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyDailyPass()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.DAILY, HAS_MANAGED_COPY,
                                                     ( OLDER * OVER_ONE_DAY ) );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyRejectFail()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, SnapshotsPolicy.NEVER,
                                                     HAS_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyRejectNoManagedContentFail()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_NOT_FOUND, SnapshotsPolicy.NEVER, NO_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyRejectPass()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, SnapshotsPolicy.NEVER,
                                                     HAS_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyHourlyFail()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, SnapshotsPolicy.HOURLY, HAS_MANAGED_COPY,
                                                     ( NEWER * ONE_MINUTE ) );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyHourlyNoManagedContent()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.HOURLY, NO_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyHourlyPass()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.HOURLY, HAS_MANAGED_COPY,
                                                     ( OLDER * OVER_ONE_HOUR ) );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyOnceFail()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, SnapshotsPolicy.ONCE, HAS_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyOnceNoManagedContent()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.ONCE, NO_MANAGED_COPY );
    }

    @Test
    public void testGetProxiedSnapshotsArtifactPolicyOncePass()
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, SnapshotsPolicy.ONCE, NO_MANAGED_COPY );
    }

    private void assertGetProxiedSnapshotsArtifactWithPolicy( int expectation, String snapshotsPolicy,
                                                              boolean hasManagedCopy )
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( expectation, snapshotsPolicy, hasManagedCopy, 0 );
    }

    private void assertGetProxiedSnapshotsArtifactWithPolicy( int expectation, String snapshotsPolicy,
                                                              boolean hasManagedCopy, long deltaManagedToRemoteTimestamp )
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupCleanInternalRepo();

        String resourcePath = "org/apache/archiva/test/3.0-SNAPSHOT/test-3.0-20070822.033400-42.jar";
        String expectedRemoteContents = "archiva-test-3.0-20070822.033400-42|jar-remote-contents";
        String expectedManagedContents = null;
        File remoteFile = populateRepo( remoteSnapshots, resourcePath, expectedRemoteContents );

        if ( hasManagedCopy )
        {
            expectedManagedContents = "archiva-test-3.0-20070822.033400-42|jar-managed-contents";
            File managedFile = populateRepo( repoRootInternal, resourcePath, expectedManagedContents );
            managedFile.setLastModified( remoteFile.lastModified() + deltaManagedToRemoteTimestamp );
        }

        setupSnapshotConnector( REPOID_INTERNAL, remoteSnapshots, snapshotsPolicy );
        saveConfiguration();

        // --- Execution
        // process the response code later, not via an exception.
        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + resourcePath );
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
                assertResponseOK( response );
                assertEquals( "Expected remote file contents", expectedRemoteContents, response.getText() );
                break;
            case EXPECT_NOT_FOUND:
                assertResponseNotFound( response );
                assertManagedFileNotExists( repoRootInternal, resourcePath );
                break;
        }
    }
}
