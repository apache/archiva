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
 * RepositoryServlet Tests, Proxied, Get of Metadata, exists on remote repository only.
 *
 *
 */
public class RepositoryServletProxiedMetadataRemoteOnlyTest
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
    public void testGetProxiedSnapshotVersionMetadataRemoteOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();
        saveConfiguration();

        String path = "org/apache/archiva/archivatest-maven-plugin/4.0-alpha-1-SNAPSHOT/maven-metadata.xml";
        String version = "4.0-alpha-1-SNAPSHOT";
        String timestamp = "20040305.112233";
        String buildNumber = "2";
        String lastUpdated = "20040305112233";
        String expectedMetadata =
            createVersionMetadata( "org.apache.archiva", "archivatest-maven-plugin", version, timestamp, buildNumber,
                                   lastUpdated );

        populateRepo( remoteSnapshots, path, expectedMetadata );

        setupConnector( REPOID_INTERNAL, remoteSnapshots );
        setupConnector( REPOID_INTERNAL, remotePrivateSnapshots );
        saveConfiguration();

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedPluginSnapshotVersionMetadataRemoteOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();
        saveConfiguration();

        String path = "org/apache/maven/plugins/maven-assembly-plugin/2.2-beta-2-SNAPSHOT/maven-metadata.xml";
        String version = "2.2-beta-2-SNAPSHOT";
        String timestamp = "20071017.162810";
        String buildNumber = "20";
        String lastUpdated = "20071017162810";
        String expectedMetadata =
            createVersionMetadata( "org.apache.maven.plugins", "maven-assembly-plugin", version, timestamp, buildNumber,
                                   lastUpdated );

        populateRepo( remoteSnapshots, path, expectedMetadata );

        setupConnector( REPOID_INTERNAL, remoteSnapshots );
        setupConnector( REPOID_INTERNAL, remotePrivateSnapshots );
        saveConfiguration();

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedVersionMetadataRemoteOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();
        saveConfiguration();

        String path = "org/apache/archiva/archivatest-maven-plugin/4.0-alpha-2/maven-metadata.xml";
        String expectedMetadata =
            createVersionMetadata( "org.apache.archiva", "archivatest-maven-plugin", "4.0-alpha-2" );

        populateRepo( remoteSnapshots, path, expectedMetadata );

        setupConnector( REPOID_INTERNAL, remoteSnapshots );
        setupConnector( REPOID_INTERNAL, remotePrivateSnapshots );
        saveConfiguration();

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedProjectMetadataRemoteOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();
        saveConfiguration();

        String path = "org/apache/archiva/archivatest-maven-plugin/maven-metadata.xml";
        String latest = "1.0-alpha-4";
        String release = "1.0-alpha-4";
        String expectedMetadata =
            createProjectMetadata( "org.apache.archiva", "archivatest-maven-plugin", latest, release,
                                   new String[]{ "1.0-alpha-4" } );

        populateRepo( remoteSnapshots, path, expectedMetadata );

        setupConnector( REPOID_INTERNAL, remoteSnapshots );
        setupConnector( REPOID_INTERNAL, remotePrivateSnapshots );
        saveConfiguration();

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }

    @Test
    public void testGetProxiedGroupMetadataRemoteOnly()
        throws Exception
    {
        // --- Setup
        setupSnapshotsRemoteRepo();
        setupPrivateSnapshotsRemoteRepo();
        setupCleanInternalRepo();
        saveConfiguration();

        String path = "org/apache/archiva/maven-metadata.xml";
        String expectedMetadata =
            createGroupMetadata( "org.apache.archiva", new String[]{ "archivatest-maven-plugin" } );

        populateRepo( remoteSnapshots, path, expectedMetadata );

        setupConnector( REPOID_INTERNAL, remoteSnapshots );
        setupConnector( REPOID_INTERNAL, remotePrivateSnapshots );
        saveConfiguration();

        // --- Execution
        String actualMetadata = requestMetadataOK( path );

        // --- Verification
        assertExpectedMetadata( expectedMetadata, actualMetadata );
    }
}
