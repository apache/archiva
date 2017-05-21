package org.apache.archiva.metadata.repository.storage.maven2;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.filter.AllFilter;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.storage.ReadMetadataRequest;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageRuntimeException;
import org.apache.archiva.proxy.common.WagonFactory;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;


public class Maven2RepositoryMetadataResolverManagedReleaseTest
    extends Maven2RepositoryMetadataResolverTest
{
    private static final Filter<String> ALL = new AllFilter<String>();

    @Inject
    @Named ( "repositoryStorage#maven2")
    private Maven2RepositoryStorage storage;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_REMOTE_REPO_ID = "central";

    private static final String ASF_SCM_CONN_BASE = "scm:svn:http://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_DEV_CONN_BASE = "scm:svn:https://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_VIEWVC_BASE = "http://svn.apache.org/viewvc/";

    private static final String TEST_SCM_CONN_BASE = "scm:svn:http://svn.example.com/repos/";

    private static final String TEST_SCM_DEV_CONN_BASE = "scm:svn:https://svn.example.com/repos/";

    private static final String TEST_SCM_URL_BASE = "http://svn.example.com/repos/";

    private static final String EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";

    private static final String EMPTY_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    private WagonFactory wagonFactory;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        testRepo.setReleases( true );
        testRepo.setSnapshots( false );

        configuration.save( c );

        assertFalse( c.getManagedRepositories().get( 0 ).isSnapshots() );
        assertTrue( c.getManagedRepositories().get( 0 ).isReleases() );

    }

    @Test
    @Override
    public void testModelWithJdkProfileActivation()
        throws Exception
    {
        // skygo IMHO must fail because TEST_REPO_ID ( is snap ,no release) and we seek for a snapshot

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "org.apache.maven" ).projectId(
                "maven-archiver" ).projectVersion( "2.4.1" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );
    }

    @Test (expected = RepositoryStorageRuntimeException.class)
    @Override
    public void testGetProjectVersionMetadataForTimestampedSnapshotMissingMetadata()
        throws Exception
    {
        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "com.example.test" ).projectId(
                "missing-metadata" ).projectVersion( "1.0-SNAPSHOT" );
        storage.readProjectVersionMetadata( readMetadataRequest );
    }

    @Test (expected = RepositoryStorageRuntimeException.class)
    @Override
    public void testGetProjectVersionMetadataForTimestampedSnapshotMalformedMetadata()
        throws Exception
    {
        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "com.example.test" ).projectVersion(
                "malformed-metadata" ).projectVersion( "1.0-SNAPSHOT" );
        storage.readProjectVersionMetadata( readMetadataRequest );
    }

    @Test (expected = RepositoryStorageRuntimeException.class)
    @Override
    public void testGetProjectVersionMetadataForTimestampedSnapshot()
        throws Exception
    {
        super.testGetProjectVersionMetadataForTimestampedSnapshot();
    }


    @Test (expected = RepositoryStorageRuntimeException.class)
    @Override
    public void testGetProjectVersionMetadataForTimestampedSnapshotIncompleteMetadata()
        throws Exception
    {
        super.testGetProjectVersionMetadataForTimestampedSnapshotIncompleteMetadata();
    }

}
