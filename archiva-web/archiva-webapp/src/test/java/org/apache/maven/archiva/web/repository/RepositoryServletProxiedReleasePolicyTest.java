package org.apache.maven.archiva.web.repository;

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

import org.apache.maven.archiva.policies.ReleasesPolicy;

import java.io.File;

/**
 * RepositoryServlet Tests, Proxied, Get of Release Artifacts, with varying policy settings. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServletProxiedReleasePolicyTest
    extends AbstractRepositoryServletProxiedTestCase
{
    public void testGetProxiedReleaseArtifactPolicyAlwaysManagedNewer()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, ReleasesPolicy.IGNORED, HAS_MANAGED_COPY,
                                                   ( NEWER * OVER_ONE_DAY ) );
    }

    public void testGetProxiedReleaseArtifactPolicyAlwaysManagedOlder()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.IGNORED, HAS_MANAGED_COPY,
                                                   ( OLDER * OVER_ONE_DAY ) );
    }

    public void testGetProxiedReleaseArtifactPolicyAlwaysNoManagedContent()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.IGNORED, NO_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyDailyFail()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, ReleasesPolicy.DAILY, HAS_MANAGED_COPY,
                                                   ( NEWER * ONE_MINUTE ) );
    }

    public void testGetProxiedReleaseArtifactPolicyDailyNoManagedContent()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.DAILY, NO_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyDailyPass()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.DAILY, HAS_MANAGED_COPY,
                                                   ( OLDER * OVER_ONE_DAY ) );
    }

    public void testGetProxiedReleaseArtifactPolicyDisabledFail()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, ReleasesPolicy.DISABLED, HAS_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyDisabledNoManagedContentFail()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_NOT_FOUND, ReleasesPolicy.DISABLED, NO_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyDisabledPass()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, ReleasesPolicy.DISABLED, HAS_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyHourlyFail()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, ReleasesPolicy.HOURLY, HAS_MANAGED_COPY,
                                                   ( NEWER * ONE_MINUTE ) );
    }

    public void testGetProxiedReleaseArtifactPolicyHourlyNoManagedContent()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.HOURLY, NO_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyHourlyPass()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.HOURLY, HAS_MANAGED_COPY,
                                                   ( OLDER * OVER_ONE_HOUR ) );
    }

    public void testGetProxiedReleaseArtifactPolicyOnceFail()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_MANAGED_CONTENTS, ReleasesPolicy.ONCE, HAS_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyOnceNoManagedContent()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.ONCE, NO_MANAGED_COPY );
    }

    public void testGetProxiedReleaseArtifactPolicyOncePass()
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( EXPECT_REMOTE_CONTENTS, ReleasesPolicy.ONCE, NO_MANAGED_COPY );
    }

    private void assertGetProxiedReleaseArtifactWithPolicy( int expectation, String releasePolicy,
                                                            boolean hasManagedCopy )
        throws Exception
    {
        assertGetProxiedReleaseArtifactWithPolicy( expectation, releasePolicy, hasManagedCopy, 0 );
    }

    private void assertGetProxiedReleaseArtifactWithPolicy( int expectation, String releasePolicy,
                                                            boolean hasManagedCopy, long deltaManagedToRemoteTimestamp )
        throws Exception
    {
        // --- Setup
        setupCentralRemoteRepo();
        setupCleanInternalRepo();

        String resourcePath = "org/apache/archiva/test/1.0/test-1.0.jar";
        String expectedRemoteContents = "archiva-test-1.0|jar-remote-contents";
        String expectedManagedContents = null;
        File remoteFile = populateRepo( remoteCentral, resourcePath, expectedRemoteContents );

        if ( hasManagedCopy )
        {
            expectedManagedContents = "archiva-test-1.0|jar-managed-contents";
            File managedFile = populateRepo( repoRootInternal, resourcePath, expectedManagedContents );
            managedFile.setLastModified( remoteFile.lastModified() + deltaManagedToRemoteTimestamp );
        }

        setupReleaseConnector( REPOID_INTERNAL, remoteCentral, releasePolicy );
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
