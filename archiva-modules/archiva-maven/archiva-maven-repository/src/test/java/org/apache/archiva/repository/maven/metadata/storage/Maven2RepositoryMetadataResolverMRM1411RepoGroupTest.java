package org.apache.archiva.repository.maven.metadata.storage;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.filter.AllFilter;
import org.apache.archiva.filter.Filter;
import org.apache.archiva.metadata.repository.storage.ReadMetadataRequest;
import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.proxy.maven.WagonFactoryRequest;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.Wagon;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith ( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration ( { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class Maven2RepositoryMetadataResolverMRM1411RepoGroupTest
    extends TestCase
{
    private static final Filter<String> ALL = new AllFilter<String>();

    @Inject
    @Named ( "repositoryStorage#maven2")
    private Maven2RepositoryStorage storage;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_SNAP_REPO_ID = "tests";

    private static final String TEST_REPO_GROUP_ID = "testrg";

    private static final String TEST_REMOTE_REPO_ID = "central";

    private static final String ASF_SCM_CONN_BASE = "scm:svn:http://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_DEV_CONN_BASE = "scm:svn:https://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_VIEWVC_BASE = "http://svn.apache.org/viewvc/";

    private static final String TEST_SCM_CONN_BASE = "scm:svn:http://svn.example.com/repos/";

    private static final String TEST_SCM_DEV_CONN_BASE = "scm:svn:https://svn.example.com/repos/";

    private static final String TEST_SCM_URL_BASE = "http://svn.example.com/repos/";

    private static final String EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";

    private static final String EMPTY_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    @Inject
    @Named ( "archivaConfiguration#default" )
    private ArchivaConfiguration configuration;

    @Inject
    RepositoryRegistry repositoryRegistry;

    private WagonFactory wagonFactory;

    ManagedRepositoryConfiguration testRepo;

    ManagedRepositoryConfiguration testRepoS;

    Configuration c;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        c = new Configuration();

        testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( Paths.get( "target/test-repository" ).toAbsolutePath().toString() );
        testRepo.setReleases( true );
        testRepo.setSnapshots( false );
        c.addManagedRepository( testRepo );

        testRepoS = new ManagedRepositoryConfiguration();
        testRepoS.setId( TEST_SNAP_REPO_ID );
        testRepoS.setLocation( Paths.get( "target/test-repositorys" ).toAbsolutePath().toString() );
        testRepoS.setReleases( false );
        testRepoS.setSnapshots( true );
        c.addManagedRepository( testRepoS );

        RemoteRepositoryConfiguration testRemoteRepo = new RemoteRepositoryConfiguration();
        testRemoteRepo.setId( TEST_REMOTE_REPO_ID );
        testRemoteRepo.setLayout( "default" );
        testRemoteRepo.setName( "Central Repository" );
        testRemoteRepo.setUrl( "http://central.repo.com/maven2" );
        testRemoteRepo.setTimeout( 10 );
        c.addRemoteRepository( testRemoteRepo );

        ProxyConnectorConfiguration proxyConnector = new ProxyConnectorConfiguration();
        proxyConnector.setSourceRepoId( TEST_REPO_ID );
        proxyConnector.setTargetRepoId( TEST_REMOTE_REPO_ID );
        proxyConnector.setDisabled( false );
        c.addProxyConnector( proxyConnector );

        ProxyConnectorConfiguration proxyConnectors = new ProxyConnectorConfiguration();
        proxyConnectors.setSourceRepoId( TEST_SNAP_REPO_ID );
        proxyConnectors.setTargetRepoId( TEST_REMOTE_REPO_ID );
        proxyConnectors.setDisabled( false );
        c.addProxyConnector( proxyConnectors );

        List<String> repos = new ArrayList<>();
        repos.add( TEST_REPO_ID );
        repos.add( TEST_SNAP_REPO_ID );

        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( TEST_REPO_GROUP_ID );
        repoGroup.setRepositories( repos );
        c.addRepositoryGroup( repoGroup );

        configuration.save( c );
        repositoryRegistry.reload();

        assertFalse( c.getManagedRepositories().get( 0 ).isSnapshots() );
        assertTrue( c.getManagedRepositories().get( 0 ).isReleases() );

        assertTrue( c.getManagedRepositories().get( 1 ).isSnapshots() );
        assertFalse( c.getManagedRepositories().get( 1 ).isReleases() );

        wagonFactory = mock( WagonFactory.class );

        storage.setWagonFactory( wagonFactory );

        Wagon wagon = new MockWagon();
        when( wagonFactory.getWagon( new WagonFactoryRequest().protocol( "wagon#http" ) ) ).thenReturn( wagon );
    }

    // Tests for MRM-1411 - START
    @Test
    public void testGetProjectVersionMetadataWithParentSuccessful()
        throws Exception
    {
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-module-a",
                                    "target/test-repository/com/example/test/test-artifact-module-a" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-root",
                "target/test-repository/com/example/test/test-artifact-root" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-parent",
                "target/test-repository/com/example/test/test-artifact-parent" );

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "com.example.test" ).projectId(
                "test-artifact-module-a" ).projectVersion( "1.0" );
        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );

        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "jar", facet.getPackaging() );
        assertEquals( "http://maven.apache.org", metadata.getUrl() );
        assertEquals( "com.example.test", facet.getParent().getGroupId() );
        assertEquals( "test-artifact-root", facet.getParent().getArtifactId() );
        assertEquals( "1.0", facet.getParent().getVersion() );
        assertEquals( "test-artifact-module-a", facet.getArtifactId() );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertNull( metadata.getCiManagement() );
        assertNotNull( metadata.getDescription() );

        checkApacheLicense( metadata );

        assertEquals( "1.0", metadata.getId() );
        assertEquals( "Test Artifact :: Module A", metadata.getName() );
        String path = "test-artifact/trunk/test-artifact-module-a";
        assertEquals( TEST_SCM_CONN_BASE + path, metadata.getScm().getConnection() );
        assertEquals( TEST_SCM_DEV_CONN_BASE + path, metadata.getScm().getDeveloperConnection() );
        assertEquals( TEST_SCM_URL_BASE + path, metadata.getScm().getUrl() );

        List<Dependency> dependencies = metadata.getDependencies();
        assertEquals( 2, dependencies.size() );
        assertDependency( dependencies.get( 0 ), "commons-io", "commons-io", "1.4" );
        assertDependency( dependencies.get( 1 ), "junit", "junit", "3.8.1", "test" );

        List<String> paths = new ArrayList<>();
        paths.add( "target/test-repository/com/example/test/test-artifact-module-a" );
        paths.add( "target/test-repository/com/example/test/test-artifact-parent" );
        paths.add( "target/test-repository/com/example/test/test-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }

    @Test
    public void testGetProjectVersionMetadataWithParentNoRemoteReposConfigured()
        throws Exception
    {
        // remove configuration
        Configuration config = configuration.getConfiguration();
        RemoteRepositoryConfiguration remoteRepo = config.findRemoteRepositoryById( TEST_REMOTE_REPO_ID );
        config.removeRemoteRepository( remoteRepo );

        configuration.save( config );

        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-module-a",
                                    "target/test-repository/com/example/test/test-artifact-module-a" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-root",
                "target/test-repository/com/example/test/test-artifact-root" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-parent",
                "target/test-repository/com/example/test/test-artifact-parent" );

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "com.example.test" ).projectId(
                "test-artifact-module-a" ).projectVersion( "1.0" );
        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );
        assertEquals( "1.0", metadata.getId() );

        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertNotNull( facet );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertEquals( "test-artifact-module-a", facet.getArtifactId() );
        assertEquals( "jar", facet.getPackaging() );

        List<String> paths = new ArrayList<>();
        paths.add( "target/test-repository/com/example/test/test-artifact-module-a" );
        paths.add( "target/test-repository/com/example/test/test-artifact-parent" );
        paths.add( "target/test-repository/com/example/test/test-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }

    @Test
    public void testGetProjectVersionMetadataWithParentNotInAnyRemoteRepo()
        throws Exception
    {
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-module-a",
                                    "target/test-repository/com/example/test/test-artifact-module-a" );

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "com.example.test" ).projectId(
                "missing-parent" ).projectVersion( "1.1" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );

        assertEquals( "1.1", metadata.getId() );

        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertNotNull( facet );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertEquals( "missing-parent", facet.getArtifactId() );
        assertEquals( "jar", facet.getPackaging() );

        List<String> paths = new ArrayList<>();
        paths.add( "target/test-repository/com/example/test/test-artifact-module-a" );
        paths.add( "target/test-repository/com/example/test/test-artifact-parent" );
        paths.add( "target/test-repository/com/example/test/test-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }

    @Test
    public void testGetProjectVersionMetadataWithParentSnapshotVersion()
        throws Exception
    {
        copyTestArtifactWithParent( "target/test-classes/com/example/test/test-snapshot-artifact-module-a",
                                    "target/test-repositorys/com/example/test/test-snapshot-artifact-module-a" );
        copyTestArtifactWithParent( "target/test-classes/com/example/test/test-snapshot-artifact-root",
                                    "target/test-repositorys/com/example/test/test-snapshot-artifact-root" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-parent",
                "target/test-repositorys/com/example/test/test-artifact-parent" );

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest( TEST_SNAP_REPO_ID, "com.example.test", "test-snapshot-artifact-module-a",
                                     "1.1-SNAPSHOT" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );

        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "jar", facet.getPackaging() );
        assertEquals( "com.example.test", facet.getParent().getGroupId() );
        assertEquals( "test-snapshot-artifact-root", facet.getParent().getArtifactId() );
        assertEquals( "1.1-SNAPSHOT", facet.getParent().getVersion() );
        assertEquals( "test-snapshot-artifact-module-a", facet.getArtifactId() );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertNull( metadata.getCiManagement() );
        assertNotNull( metadata.getDescription() );

        checkApacheLicense( metadata );

        assertEquals( "1.1-SNAPSHOT", metadata.getId() );
        assertEquals( "Test Snapshot Artifact :: Module A", metadata.getName() );
        String path = "test-snapshot-artifact/trunk/test-snapshot-artifact-module-a";
        assertEquals( TEST_SCM_CONN_BASE + path, metadata.getScm().getConnection() );
        assertEquals( TEST_SCM_DEV_CONN_BASE + path, metadata.getScm().getDeveloperConnection() );
        assertEquals( TEST_SCM_URL_BASE + path, metadata.getScm().getUrl() );

        List<Dependency> dependencies = metadata.getDependencies();
        assertEquals( 2, dependencies.size() );
        assertDependency( dependencies.get( 0 ), "commons-io", "commons-io", "1.4" );
        assertDependency( dependencies.get( 1 ), "junit", "junit", "3.8.1", "test" );

        List<String> paths = new ArrayList<>();
        paths.add( "target/test-repositorys/com/example/test/test-snapshot-artifact-module-a" );
        paths.add( "target/test-repositorys/com/example/test/test-snapshot-artifact-root" );
        deleteTestArtifactWithParent( paths );
    }

    @Test
    public void testGetProjectVersionMetadataWithParentSnapshotVersionAndSnapNotAllowed()
        throws Exception
    {
        copyTestArtifactWithParent( "target/test-classes/com/example/test/test-snapshot-artifact-module-a",
                                    "target/test-repositorys/com/example/test/test-snapshot-artifact-module-a" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-parent",
                "target/test-repositorys/com/example/test/test-artifact-parent" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-snapshot-artifact-root",
                "target/test-repositorys/com/example/test/test-snapshot-artifact-root" );

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_SNAP_REPO_ID ).namespace( "com.example.test" ).projectId(
                "test-snapshot-artifact-module-a" ).projectVersion( "1.1-SNAPSHOT" );
        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );

        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "jar", facet.getPackaging() );
        assertEquals( "com.example.test", facet.getParent().getGroupId() );
        assertEquals( "test-snapshot-artifact-root", facet.getParent().getArtifactId() );
        assertEquals( "1.1-SNAPSHOT", facet.getParent().getVersion() );
        assertEquals( "test-snapshot-artifact-module-a", facet.getArtifactId() );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertNull( metadata.getCiManagement() );
        assertNotNull( metadata.getDescription() );

        checkApacheLicense( metadata );

        assertEquals( "1.1-SNAPSHOT", metadata.getId() );
        assertEquals( "Test Snapshot Artifact :: Module A", metadata.getName() );
        String path = "test-snapshot-artifact/trunk/test-snapshot-artifact-module-a";
        assertEquals( TEST_SCM_CONN_BASE + path, metadata.getScm().getConnection() );
        assertEquals( TEST_SCM_DEV_CONN_BASE + path, metadata.getScm().getDeveloperConnection() );
        assertEquals( TEST_SCM_URL_BASE + path, metadata.getScm().getUrl() );

        List<Dependency> dependencies = metadata.getDependencies();
        assertEquals( 2, dependencies.size() );
        assertDependency( dependencies.get( 0 ), "commons-io", "commons-io", "1.4" );
        assertDependency( dependencies.get( 1 ), "junit", "junit", "3.8.1", "test" );

        List<String> paths = new ArrayList<>();
        paths.add( "target/test-repositorys/com/example/test/test-snapshot-artifact-module-a" );
        paths.add( "target/test-repositorys/com/example/test/test-snapshot-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }

    @Test
    public void testGetProjectVersionMetadataWithParentSnapshotVersionAndSnapNotAllowed2()
        throws Exception
    {
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-module-b",
                                    "target/test-repository/com/example/test/test-artifact-module-b" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-artifact-parent",
                "target/test-repository/com/example/test/test-artifact-parent" );
        copyTestArtifactWithParent( "src/test/resources/com/example/test/test-snapshot-artifact-root",
                "target/test-repository/com/example/test/test-snapshot-artifact-root" );

        ReadMetadataRequest readMetadataRequest =
            new ReadMetadataRequest().repositoryId( TEST_REPO_ID ).namespace( "com.example.test" ).projectId(
                "test-artifact-module-b" ).projectVersion( "1.0" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( readMetadataRequest );

        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "jar", facet.getPackaging() );
        assertEquals( "com.example.test", facet.getParent().getGroupId() );
        assertEquals( "test-snapshot-artifact-root", facet.getParent().getArtifactId() );
        assertEquals( "1.1-SNAPSHOT", facet.getParent().getVersion() );
        assertEquals( "test-artifact-module-b", facet.getArtifactId() );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertNull( metadata.getCiManagement() );
        assertNotNull( metadata.getDescription() );

        checkApacheLicense( metadata );

        assertEquals( "1.0", metadata.getId() );
        assertEquals( "Test Artifact :: Module B", metadata.getName() );
        String path = "test-snapshot-artifact/trunk/test-artifact-module-b";
        assertEquals( TEST_SCM_CONN_BASE + path, metadata.getScm().getConnection() );
        assertEquals( TEST_SCM_DEV_CONN_BASE + path, metadata.getScm().getDeveloperConnection() );
        assertEquals( TEST_SCM_URL_BASE + path, metadata.getScm().getUrl() );

        List<Dependency> dependencies = metadata.getDependencies();
        assertEquals( 2, dependencies.size() );
        assertDependency( dependencies.get( 0 ), "commons-io", "commons-io", "1.4" );
        assertDependency( dependencies.get( 1 ), "junit", "junit", "3.8.1", "test" );

        List<String> paths = new ArrayList<>();
        paths.add( "target/test-repository/com/example/test/test-artifact-module-b" );
        paths.add( "target/test-repository/com/example/test/test-snapshot-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }
    // Tests for MRM-1411 - END

    private void assertDependency( Dependency dependency, String groupId, String artifactId, String version )
    {
        assertDependency( dependency, groupId, artifactId, version, "compile" );
    }

    private void assertDependency( Dependency dependency, String groupId, String artifactId, String version,
                                   String scope )
    {
        assertEquals( artifactId, dependency.getArtifactId() );
        assertEquals( "jar", dependency.getType() );
        assertEquals( version, dependency.getVersion() );
        assertEquals( groupId, dependency.getNamespace() );
        assertEquals( scope, dependency.getScope() );
        assertNull( dependency.getClassifier() );
        assertNull( dependency.getSystemPath() );
    }

    private void assertArtifact( ArtifactMetadata artifact, String id, int size, String sha1, String md5 )
    {
        assertEquals( id, artifact.getId() );
        assertEquals( md5, artifact.getMd5() );
        assertEquals( sha1, artifact.getSha1() );
        assertEquals( size, artifact.getSize() );
        assertEquals( "org.codehaus.plexus", artifact.getNamespace() );
        assertEquals( "plexus-spring", artifact.getProject() );
        assertEquals( "1.2", artifact.getVersion() );
        assertEquals( TEST_REPO_ID, artifact.getRepositoryId() );
    }

    private void assertMailingList( MailingList mailingList, String name, String archive, String post, String subscribe,
                                    String unsubscribe, List<String> otherArchives, boolean allowPost )
    {
        assertEquals( archive, mailingList.getMainArchiveUrl() );
        if ( allowPost )
        {
            assertEquals( post, mailingList.getPostAddress() );
        }
        else
        {
            assertNull( mailingList.getPostAddress() );
        }
        assertEquals( subscribe, mailingList.getSubscribeAddress() );
        assertEquals( unsubscribe, mailingList.getUnsubscribeAddress() );
        assertEquals( name, mailingList.getName() );
        assertEquals( otherArchives, mailingList.getOtherArchives() );
    }

    private void assertMailingList( String prefix, MailingList mailingList, String name, boolean allowPost,
                                    String nabbleUrl )
    {
        List<String> otherArchives = new ArrayList<>();
        otherArchives.add( "http://www.mail-archive.com/" + prefix + "@archiva.apache.org" );
        if ( nabbleUrl != null )
        {
            otherArchives.add( nabbleUrl );
        }
        otherArchives.add( "http://markmail.org/list/org.apache.archiva." + prefix );
        assertMailingList( mailingList, name, "http://mail-archives.apache.org/mod_mbox/archiva-" + prefix + "/",
                           prefix + "@archiva.apache.org", prefix + "-subscribe@archiva.apache.org",
                           prefix + "-unsubscribe@archiva.apache.org", otherArchives, allowPost );
    }

    private void checkApacheLicense( ProjectVersionMetadata metadata )
    {
        assertEquals( Arrays.asList( new License( "The Apache Software License, Version 2.0",
                                                  "http://www.apache.org/licenses/LICENSE-2.0.txt" ) ),
                      metadata.getLicenses() );
    }

    private void checkOrganizationApache( ProjectVersionMetadata metadata )
    {
        assertEquals( "The Apache Software Foundation", metadata.getOrganization().getName() );
        assertEquals( "http://www.apache.org/", metadata.getOrganization().getUrl() );
    }

    private void deleteTestArtifactWithParent( List<String> pathsToBeDeleted )
        throws IOException
    {
        for ( String path : pathsToBeDeleted )
        {
            Path dir = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), path );
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( dir );

            assertFalse( Files.exists(dir) );
        }
        Path dest = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/test-repository/com/example/test/test-artifact-module-a" );
        Path parentPom = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/test-repository/com/example/test/test-artifact-parent" );
        Path rootPom = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/test-repository/com/example/test/test-artifact-root" );

        org.apache.archiva.common.utils.FileUtils.deleteDirectory( dest );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( parentPom );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( rootPom );

        assertFalse( Files.exists(dest) );
        assertFalse( Files.exists(parentPom) );
        assertFalse( Files.exists(rootPom) );
    }

    private Path copyTestArtifactWithParent( String srcPath, String destPath )
        throws IOException
    {
        Path src = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), srcPath );
        Path dest = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), destPath );

        FileUtils.copyDirectory( src.toFile(), dest.toFile() );
        assertTrue( Files.exists(dest) );
        return dest;
    }

}
