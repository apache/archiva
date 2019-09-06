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


import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.policies.PolicyOption;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

/**
 * RepositoryServlet Tests, Proxied, Get of Timestamped Snapshot Artifacts, with varying policy settings. 
 *
 *
 */
public class RepositoryServletProxiedPluginSnapshotPolicyTest
    extends AbstractRepositoryServletProxiedTestCase
{

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

    private void assertGetProxiedSnapshotsArtifactWithPolicy( int expectation, PolicyOption snapshotsPolicy,
                                                              boolean hasManagedCopy )
        throws Exception
    {
        assertGetProxiedSnapshotsArtifactWithPolicy( expectation, snapshotsPolicy, hasManagedCopy, 0 );
    }

    private void assertGetProxiedSnapshotsArtifactWithPolicy( int expectation, PolicyOption snapshotsPolicy,
                                                              boolean hasManagedCopy, long deltaManagedToRemoteTimestamp )
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupCleanInternalRepo();
        saveConfiguration();

        String resourcePath = "org/apache/archiva/archivatest-maven-plugin/4.0-alpha-1-SNAPSHOT/archivatest-maven-plugin-4.0-alpha-1-20070822.033400-42.jar";
        String expectedRemoteContents = "archivatest-maven-plugin-4.0-alpha-1-20070822.033400-42|jar-remote-contents";
        String expectedManagedContents = null;
        Path remoteFile = populateRepo( remoteSnapshots, resourcePath, expectedRemoteContents );

        if ( hasManagedCopy )
        {
            expectedManagedContents = "archivatest-maven-plugin-4.0-alpha-1-20070822.033400-42|jar-managed-contents";
            Path managedFile = populateRepo( repoRootInternal, resourcePath, expectedManagedContents );
            Files.setLastModifiedTime( managedFile, FileTime.fromMillis( Files.getLastModifiedTime( remoteFile).toMillis() + deltaManagedToRemoteTimestamp ));
        }

        archivaConfiguration.getConfiguration().setProxyConnectors( new ArrayList<ProxyConnectorConfiguration>( 0 ) );

        setupSnapshotConnector( REPOID_INTERNAL, remoteSnapshots, snapshotsPolicy );
        saveConfiguration();

        // --- Execution
        // process the response code later, not via an exception.
        //HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + resourcePath );
        WebResponse response = getServletUnitClient().getResponse( request );

        // --- Verification

        switch ( expectation )
        {
            case EXPECT_MANAGED_CONTENTS:
                assertResponseOK( response );
                assertTrue( "Invalid Test Case: Can't expect managed contents with "
                    + "test that doesn't have a managed copy in the first place.", hasManagedCopy );
                assertEquals( "Expected managed file contents", expectedManagedContents, response.getContentAsString() );
                break;
            case EXPECT_REMOTE_CONTENTS:
                assertResponseOK( response );
                assertEquals( "Expected remote file contents", expectedRemoteContents, response.getContentAsString() );
                break;
            case EXPECT_NOT_FOUND:
                assertResponseNotFound( response );
                assertManagedFileNotExists( repoRootInternal, resourcePath );
                break;
        }
    }
}
