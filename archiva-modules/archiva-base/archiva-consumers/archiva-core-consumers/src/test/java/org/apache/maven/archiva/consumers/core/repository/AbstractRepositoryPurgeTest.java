package org.apache.maven.archiva.consumers.core.repository;

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

import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;

/**
 */
public abstract class AbstractRepositoryPurgeTest
    extends PlexusInSpringTestCase
{
    public static final String TEST_REPO_ID = "test-repo";

    public static final String TEST_REPO_NAME = "Test Repository";

    public static final int TEST_RETENTION_COUNT = 2;

    public static final int TEST_DAYS_OLDER = 30;

    public static final String PATH_TO_BY_DAYS_OLD_ARTIFACT = "org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar";

    public static final String PATH_TO_BY_DAYS_OLD_METADATA_DRIVEN_ARTIFACT = "org/codehaus/plexus/plexus-utils/1.4.3-SNAPSHOT/plexus-utils-1.4.3-20070113.163208-4.jar";

    public static final String PATH_TO_BY_RETENTION_COUNT_ARTIFACT = "org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar";

    public static final String PATH_TO_BY_RETENTION_COUNT_POM = "org/codehaus/castor/castor-anttasks/1.1.2-SNAPSHOT/castor-anttasks-1.1.2-20070506.163513-2.pom";

    public static final String PATH_TO_TEST_ORDER_OF_DELETION = "org/apache/maven/plugins/maven-assembly-plugin/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar";

    protected static final String RELEASES_TEST_REPO_ID = "releases-test-repo-one";

    protected static final String RELEASES_TEST_REPO_NAME = "Releases Test Repo One";

    private ManagedRepositoryConfiguration config;

    private ManagedRepositoryContent repo;

    protected RepositoryPurge repoPurge;

    protected MockControl listenerControl;

    protected RepositoryListener listener;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        listenerControl = MockControl.createControl( RepositoryListener.class );

        listener = (RepositoryListener) listenerControl.getMock();
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        config = null;
        repo = null;
    }

    public ManagedRepositoryConfiguration getRepoConfiguration( String repoId, String repoName )
    {
        config = new ManagedRepositoryConfiguration();
        config.setId( repoId );
        config.setName( repoName );
        config.setDaysOlder( TEST_DAYS_OLDER );
        config.setLocation( getTestFile( "target/test-" + getName() + "/" + repoId ).getAbsolutePath() );
        config.setReleases( true );
        config.setSnapshots( true );
        config.setDeleteReleasedSnapshots( true );
        config.setRetentionCount( TEST_RETENTION_COUNT );
        
        return config;
    }

    public ManagedRepositoryContent getRepository()
        throws Exception
    {
        if ( repo == null )
        {
            repo = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, "default" );            
            repo.setRepository( getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ) );
        }

        return repo;
    }

    protected void assertDeleted( String path )
    {
        assertFalse( "File should have been deleted: " + path, new File( path ).exists() );
    }

    protected void assertExists( String path )
    {
        assertTrue( "File should exist: " + path, new File( path ).exists() );
    }
    
    protected File getTestRepoRoot()
    {
        return getTestFile( "target/test-" + getName() + "/" + TEST_REPO_ID );
    }

    protected String prepareTestRepos()
        throws IOException
    {
        File testDir = getTestRepoRoot();
        FileUtils.deleteDirectory( testDir );
        FileUtils.copyDirectory( getTestFile( "target/test-classes/" + TEST_REPO_ID ), testDir );
        
        File releasesTestDir = getTestFile( "target/test-" + getName() + "/" + RELEASES_TEST_REPO_ID );
        FileUtils.deleteDirectory( releasesTestDir );
        FileUtils.copyDirectory( getTestFile( "target/test-classes/" + RELEASES_TEST_REPO_ID ), releasesTestDir );
        
        return testDir.getAbsolutePath();
    }

    protected ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        return new ArchivaArtifact( groupId, artifactId, version, null, type, TEST_REPO_ID );
    }
}
