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

/**
 * RepositoryServlet Tests, Proxied, Get of Metadata, exists on local managed repository only. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServletProxiedMetadataLocalOnlyTest
    extends AbstractRepositoryServletProxiedMetadataTestCase
{
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
}
