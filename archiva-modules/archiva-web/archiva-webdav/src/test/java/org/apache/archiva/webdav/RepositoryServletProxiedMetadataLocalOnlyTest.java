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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * RepositoryServlet Tests, Proxied, Get of Metadata, exists on local managed repository only. 
 *
 *
 */
public class RepositoryServletProxiedMetadataLocalOnlyTest
    extends AbstractRepositoryServletProxiedMetadataTestCase
{

    @Before
    public void setup()
        throws Exception
    {
        super.setUp();
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testGetProxiedSnapshotVersionMetadataLocalOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();

        String path = "org/apache/archiva/archivatest-maven-plugin/4.0-alpha-1-SNAPSHOT/maven-metadata.xml";
        String expectedMetadata = createVersionMetadata( "org.apache.archiva", "archivatest-maven-plugin",
                                                         "4.0-alpha-1-SNAPSHOT" );

        populateRepo( repoRootInternal, path, expectedMetadata );

        setupConnector( REPOID_INTERNAL, remoteSnapshots );
        setupConnector( REPOID_INTERNAL, remotePrivateSnapshots );

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedVersionMetadataLocalOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();

        String path = "org/apache/archiva/archivatest-maven-plugin/4.0-alpha-2/maven-metadata.xml";
        String expectedMetadata = createVersionMetadata( "org.apache.archiva", "archivatest-maven-plugin",
                                                         "4.0-alpha-2" );

        populateRepo( repoRootInternal, path, expectedMetadata );

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedProjectMetadataLocalOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();

        String path = "org/apache/archiva/archivatest-maven-plugin/maven-metadata.xml";
        String version = "1.0-alpha-4";
        String release = "1.0-alpha-4";
        String expectedMetadata = createProjectMetadata( "org.apache.archiva", "archivatest-maven-plugin", version,
                                                         release, new String[] { "1.0-alpha-4" } );

        populateRepo( repoRootInternal, path, expectedMetadata );

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedGroupMetadataLocalOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();

        String path = "org/apache/archiva/maven-metadata.xml";
        String expectedMetadata = createGroupMetadata( "org.apache.archiva", new String[] { "archivatest-maven-plugin" } );

        populateRepo( repoRootInternal, path, expectedMetadata );

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }
}
