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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import org.apache.archiva.policies.ReleasesPolicy;
import org.junit.Test;

/**
 * RepositoryServlet Tests, Proxied, Get of Release Artifacts, with varying policy settings.
 *
 * @version $Id: RepositoryServletProxiedReleasePolicyTest.java 590908 2007-11-01 06:21:26Z joakime $
 */
public class RepositoryServletProxiedRelocatedTest
    extends AbstractRepositoryServletProxiedTestCase
{

    @Test
    public void testGetProxiedReleaseArtifactPolicyOncePass()
        throws Exception
    {
        // --- Setup
        setupCentralRemoteRepo();
        setupCleanInternalRepo();

        String resourcePath = "org/apache/archiva/test/1.0/test-1.0.jar";
        String expectedRemoteContents = "archiva-test-1.0|jar-remote-contents";
        populateRepo( remoteCentral, resourcePath, expectedRemoteContents );

        resourcePath = "archiva/test/1.0/test-1.0.pom";
        String pom = "<project>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>archiva</groupId>" +
                "<artifactId>test</artifactId>" +
                "<version>1.0</version>" +
                "<distributionManagement>" +
                "<relocation>" +
                "<groupId>org.apache.archiva</groupId>" +
                "</relocation>" +
                "</distributionManagement>" +
                "</project>";
        populateRepo( remoteCentral, resourcePath, pom );

        resourcePath = "archiva/jars/test-1.0.jar";

        setupReleaseConnector( REPOID_INTERNAL, remoteCentral, ReleasesPolicy.ONCE );
        saveConfiguration();

        // --- Execution
        // process the response code later, not via an exception.
        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + resourcePath );
        WebResponse response = sc.getResponse( request );

        // --- Verification
        assertResponseOK( response );
        assertEquals( "Expected remote file contents", expectedRemoteContents, response.getText() );
    }
}
