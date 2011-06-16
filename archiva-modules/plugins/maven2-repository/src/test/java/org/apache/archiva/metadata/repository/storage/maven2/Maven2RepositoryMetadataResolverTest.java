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

import junit.framework.TestCase;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.filter.AllFilter;
import org.apache.archiva.metadata.repository.filter.ExcludesFilter;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataInvalidException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataNotFoundException;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.common.utils.FileUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.wagon.Wagon;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath:/spring-context.xml"} )
public class Maven2RepositoryMetadataResolverTest
    extends TestCase
{
    private static final Filter<String> ALL = new AllFilter<String>();

    @Inject
    @Named(value = "repositoryStorage#maven2")
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

    @Inject
    private ArchivaConfiguration configuration;

    private WagonFactory wagonFactory;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        //ArchivaConfiguration configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( new File( "target/test-repository" ).getAbsolutePath() );
        c.addManagedRepository( testRepo );

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

        configuration.save( c );

        wagonFactory = mock( WagonFactory.class );

        storage.setWagonFactory( wagonFactory );

        Wagon wagon = new MockWagon();
        when( wagonFactory.getWagon( "wagon#http" ) ).thenReturn( wagon );
        //storage = (Maven2RepositoryStorage) lookup( RepositoryStorage.class, "maven2" );
    }

    @Test
    public void testGetProjectVersionMetadata()
        throws Exception
    {
        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( TEST_REPO_ID, "org.apache.archiva",
                                                                              "archiva-common", "1.2.1" );
        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "jar", facet.getPackaging() );
        assertEquals( "http://archiva.apache.org/ref/1.2.1/archiva-base/archiva-common", metadata.getUrl() );
        assertEquals( "org.apache.archiva", facet.getParent().getGroupId() );
        assertEquals( "archiva-base", facet.getParent().getArtifactId() );
        assertEquals( "1.2.1", facet.getParent().getVersion() );
        assertEquals( "archiva-common", facet.getArtifactId() );
        assertEquals( "org.apache.archiva", facet.getGroupId() );
        assertEquals( "continuum", metadata.getCiManagement().getSystem() );
        assertEquals( "http://vmbuild.apache.org/continuum", metadata.getCiManagement().getUrl() );
        assertNotNull( metadata.getDescription() );
        // TODO: this would be better
//        assertEquals(
//            "Archiva is an application for managing one or more remote repositories, including administration, artifact handling, browsing and searching.",
//            metadata.getDescription() );
        assertEquals( "1.2.1", metadata.getId() );
        assertEquals( "jira", metadata.getIssueManagement().getSystem() );
        assertEquals( "http://jira.codehaus.org/browse/MRM", metadata.getIssueManagement().getUrl() );
        checkApacheLicense( metadata );
        assertEquals( "Archiva Base :: Common", metadata.getName() );
        String path = "archiva/tags/archiva-1.2.1/archiva-modules/archiva-base/archiva-common";
        assertEquals( ASF_SCM_CONN_BASE + path, metadata.getScm().getConnection() );
        assertEquals( ASF_SCM_DEV_CONN_BASE + path, metadata.getScm().getDeveloperConnection() );
        assertEquals( ASF_SCM_VIEWVC_BASE + path, metadata.getScm().getUrl() );
        checkOrganizationApache( metadata );

        assertEquals( 4, metadata.getMailingLists().size() );
        assertMailingList( "users", metadata.getMailingLists().get( 0 ), "Archiva User List", true,
                           "http://www.nabble.com/archiva-users-f16426.html" );
        assertMailingList( "dev", metadata.getMailingLists().get( 1 ), "Archiva Developer List", true,
                           "http://www.nabble.com/archiva-dev-f16427.html" );
        assertMailingList( "commits", metadata.getMailingLists().get( 2 ), "Archiva Commits List", false, null );
        assertMailingList( "issues", metadata.getMailingLists().get( 3 ), "Archiva Issues List", false,
                           "http://www.nabble.com/Archiva---Issues-f29617.html" );

        List<Dependency> dependencies = metadata.getDependencies();
        assertEquals( 10, dependencies.size() );
        assertDependency( dependencies.get( 0 ), "commons-lang", "commons-lang", "2.2" );
        assertDependency( dependencies.get( 1 ), "commons-io", "commons-io", "1.4" );
        assertDependency( dependencies.get( 2 ), "org.slf4j", "slf4j-api", "1.5.0" );
        assertDependency( dependencies.get( 3 ), "org.codehaus.plexus", "plexus-component-api", "1.0-alpha-22" );
        assertDependency( dependencies.get( 4 ), "org.codehaus.plexus", "plexus-spring", "1.2", "test" );
        assertDependency( dependencies.get( 5 ), "xalan", "xalan", "2.7.0" );
        assertDependency( dependencies.get( 6 ), "dom4j", "dom4j", "1.6.1", "test" );
        assertDependency( dependencies.get( 7 ), "junit", "junit", "3.8.1", "test" );
        assertDependency( dependencies.get( 8 ), "easymock", "easymock", "1.2_Java1.3", "test" );
        assertDependency( dependencies.get( 9 ), "easymock", "easymockclassextension", "1.2", "test" );
    }

    @Test
    public void testGetArtifactMetadata()
        throws Exception
    {
        Collection<ArtifactMetadata> springArtifacts = storage.readArtifactsMetadata( TEST_REPO_ID,
                                                                                      "org.codehaus.plexus",
                                                                                      "plexus-spring", "1.2", ALL );
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>( springArtifacts );
        Collections.sort( artifacts, new Comparator<ArtifactMetadata>()
        {
            public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
            {
                return o1.getId().compareTo( o2.getId() );
            }
        } );

        assertEquals( 3, artifacts.size() );

        ArtifactMetadata artifactMetadata = artifacts.get( 0 );
        assertEquals( "plexus-spring-1.2-sources.jar", artifactMetadata.getId() );
        MavenArtifactFacet facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 0, facet.getBuildNumber() );
        assertNull( facet.getTimestamp() );
        assertEquals( "sources", facet.getClassifier() );
        assertEquals( "java-source", facet.getType() );

        artifactMetadata = artifacts.get( 1 );
        assertEquals( "plexus-spring-1.2.jar", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 0, facet.getBuildNumber() );
        assertNull( facet.getTimestamp() );
        assertNull( facet.getClassifier() );
        assertEquals( "jar", facet.getType() );

        artifactMetadata = artifacts.get( 2 );
        assertEquals( "plexus-spring-1.2.pom", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 0, facet.getBuildNumber() );
        assertNull( facet.getTimestamp() );
        assertNull( facet.getClassifier() );
        assertEquals( "pom", facet.getType() );
    }

    @Test
    public void testGetArtifactMetadataSnapshots()
        throws Exception
    {
        Collection<ArtifactMetadata> testArtifacts = storage.readArtifactsMetadata( TEST_REPO_ID, "com.example.test",
                                                                                    "test-artifact", "1.0-SNAPSHOT",
                                                                                    ALL );
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>( testArtifacts );
        Collections.sort( artifacts, new Comparator<ArtifactMetadata>()
        {
            public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
            {
                return o1.getId().compareTo( o2.getId() );
            }
        } );

        assertEquals( 6, artifacts.size() );

        ArtifactMetadata artifactMetadata = artifacts.get( 0 );
        assertEquals( "test-artifact-1.0-20100308.230825-1.jar", artifactMetadata.getId() );
        MavenArtifactFacet facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 1, facet.getBuildNumber() );
        assertEquals( "20100308.230825", facet.getTimestamp() );
        assertNull( facet.getClassifier() );
        assertEquals( "jar", facet.getType() );

        artifactMetadata = artifacts.get( 1 );
        assertEquals( "test-artifact-1.0-20100308.230825-1.pom", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 1, facet.getBuildNumber() );
        assertEquals( "20100308.230825", facet.getTimestamp() );
        assertNull( facet.getClassifier() );
        assertEquals( "pom", facet.getType() );

        artifactMetadata = artifacts.get( 2 );
        assertEquals( "test-artifact-1.0-20100310.014828-2-javadoc.jar", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 2, facet.getBuildNumber() );
        assertEquals( "20100310.014828", facet.getTimestamp() );
        assertEquals( "javadoc", facet.getClassifier() );
        assertEquals( "javadoc", facet.getType() );

        artifactMetadata = artifacts.get( 3 );
        assertEquals( "test-artifact-1.0-20100310.014828-2-sources.jar", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 2, facet.getBuildNumber() );
        assertEquals( "20100310.014828", facet.getTimestamp() );
        assertEquals( "sources", facet.getClassifier() );
        assertEquals( "java-source", facet.getType() );

        artifactMetadata = artifacts.get( 4 );
        assertEquals( "test-artifact-1.0-20100310.014828-2.jar", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 2, facet.getBuildNumber() );
        assertEquals( "20100310.014828", facet.getTimestamp() );
        assertNull( facet.getClassifier() );
        assertEquals( "jar", facet.getType() );

        artifactMetadata = artifacts.get( 5 );
        assertEquals( "test-artifact-1.0-20100310.014828-2.pom", artifactMetadata.getId() );
        facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );
        assertEquals( 2, facet.getBuildNumber() );
        assertEquals( "20100310.014828", facet.getTimestamp() );
        assertNull( facet.getClassifier() );
        assertEquals( "pom", facet.getType() );
    }

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
        assertEquals( groupId, dependency.getGroupId() );
        assertEquals( scope, dependency.getScope() );
        assertNull( dependency.getClassifier() );
        assertNull( dependency.getSystemPath() );
    }

    @Test
    public void testGetProjectVersionMetadataForTimestampedSnapshot()
        throws Exception
    {
        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( TEST_REPO_ID, "org.apache", "apache",
                                                                              "5-SNAPSHOT" );
        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "pom", facet.getPackaging() );
        assertEquals( "http://www.apache.org/", metadata.getUrl() );
        assertNull( facet.getParent() );
        assertEquals( "org.apache", facet.getGroupId() );
        assertEquals( "apache", facet.getArtifactId() );
        assertNull( metadata.getCiManagement() );
        assertNotNull( metadata.getDescription() );
        // TODO: this would be better
//        assertEquals(
//            "The Apache Software Foundation provides support for the Apache community of open-source software projects. " +
//                "The Apache projects are characterized by a collaborative, consensus based development process, an open " +
//                "and pragmatic software license, and a desire to create high quality software that leads the way in its " +
//                "field. We consider ourselves not simply a group of projects sharing a server, but rather a community of " +
//                "developers and users.", metadata.getDescription() );
        assertEquals( "5-SNAPSHOT", metadata.getId() );
        assertNull( metadata.getIssueManagement() );
        checkApacheLicense( metadata );
        assertEquals( "The Apache Software Foundation", metadata.getName() );
        String path = "maven/pom/trunk/asf";
        assertEquals( ASF_SCM_CONN_BASE + path, metadata.getScm().getConnection() );
        assertEquals( ASF_SCM_DEV_CONN_BASE + path, metadata.getScm().getDeveloperConnection() );
        assertEquals( ASF_SCM_VIEWVC_BASE + path, metadata.getScm().getUrl() );
        checkOrganizationApache( metadata );
        assertEquals( 1, metadata.getMailingLists().size() );
        assertMailingList( metadata.getMailingLists().get( 0 ), "Apache Announce List",
                           "http://mail-archives.apache.org/mod_mbox/www-announce/", "announce@apache.org",
                           "announce-subscribe@apache.org", "announce-unsubscribe@apache.org",
                           Collections.<String>emptyList(), true );
        assertEquals( Collections.<Dependency>emptyList(), metadata.getDependencies() );
    }

    @Test
    public void testGetProjectVersionMetadataForTimestampedSnapshotMissingMetadata()
        throws Exception
    {
        try
        {
            storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "missing-metadata", "1.0-SNAPSHOT" );
            fail( "Should not be found" );
        }
        catch ( RepositoryStorageMetadataNotFoundException e )
        {
            assertEquals( "missing-pom", e.getId() );
        }
    }

    @Test
    public void testGetProjectVersionMetadataForTimestampedSnapshotMalformedMetadata()
        throws Exception
    {
        try
        {
            storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "malformed-metadata",
                                                "1.0-SNAPSHOT" );
            fail( "Should not be found" );
        }
        catch ( RepositoryStorageMetadataNotFoundException e )
        {
            assertEquals( "missing-pom", e.getId() );
        }
    }

    @Test
    public void testGetProjectVersionMetadataForTimestampedSnapshotIncompleteMetadata()
        throws Exception
    {
        try
        {
            storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "incomplete-metadata",
                                                "1.0-SNAPSHOT" );
            fail( "Should not be found" );
        }
        catch ( RepositoryStorageMetadataNotFoundException e )
        {
            assertEquals( "missing-pom", e.getId() );
        }
    }

    @Test
    public void testGetProjectVersionMetadataForInvalidPom()
        throws Exception
    {
        try
        {
            storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "invalid-pom", "1.0" );
            fail( "Should have received an exception due to invalid POM" );
        }
        catch ( RepositoryStorageMetadataInvalidException e )
        {
            assertEquals( "invalid-pom", e.getId() );
        }
    }

    @Test
    public void testGetProjectVersionMetadataForMislocatedPom()
        throws Exception
    {
        try
        {
            storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "mislocated-pom", "1.0" );
            fail( "Should have received an exception due to mislocated POM" );
        }
        catch ( RepositoryStorageMetadataInvalidException e )
        {
            assertEquals( "mislocated-pom", e.getId() );
        }
    }

    @Test
    public void testGetProjectVersionMetadataForMissingPom()
        throws Exception
    {
        try
        {
            storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "missing-pom", "1.0" );
            fail( "Should not be found" );
        }
        catch ( RepositoryStorageMetadataNotFoundException e )
        {
            assertEquals( "missing-pom", e.getId() );
        }
    }

    // Tests for MRM-1411 - START
    @Test
    public void testGetProjectVersionMetadataWithParentSuccessful()
        throws Exception
    {
        copyTestArtifactWithParent( "target/test-classes/com/example/test/test-artifact-module-a",
                                    "target/test-repository/com/example/test/test-artifact-module-a" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test",
                                                                               "test-artifact-module-a", "1.0" );

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

        List<String> paths = new ArrayList<String>();
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

        copyTestArtifactWithParent( "target/test-classes/com/example/test/test-artifact-module-a",
                                    "target/test-repository/com/example/test/test-artifact-module-a" );

        ProjectVersionMetadata metadata =  storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test",
                                                                "test-artifact-module-a", "1.0" );
        assertEquals( "1.0", metadata.getId() );

        MavenProjectFacet facet = ( MavenProjectFacet ) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertNotNull( facet );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertEquals( "test-artifact-module-a", facet.getArtifactId() );
        assertEquals( "jar", facet.getPackaging() );

        List<String> paths = new ArrayList<String>();
        paths.add( "target/test-repository/com/example/test/test-artifact-module-a" );
        paths.add( "target/test-repository/com/example/test/test-artifact-parent" );
        paths.add( "target/test-repository/com/example/test/test-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }

    @Test
    public void testGetProjectVersionMetadataWithParentNotInAnyRemoteRepo()
         throws Exception
    {
        copyTestArtifactWithParent( "target/test-classes/com/example/test/test-artifact-module-a",
                                    "target/test-repository/com/example/test/test-artifact-module-a" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "missing-parent", "1.1" );

        assertEquals( "1.1", metadata.getId() );

        MavenProjectFacet facet = ( MavenProjectFacet ) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertNotNull( facet );
        assertEquals( "com.example.test", facet.getGroupId() );
        assertEquals( "missing-parent", facet.getArtifactId() );
        assertEquals( "jar", facet.getPackaging() );

        List<String> paths = new ArrayList<String>();
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
                                    "target/test-repository/com/example/test/test-snapshot-artifact-module-a" );

        ProjectVersionMetadata metadata = storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test",
                                                                               "test-snapshot-artifact-module-a", "1.1-SNAPSHOT" );

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

        List<String> paths = new ArrayList<String>();
        paths.add( "target/test-repository/com/example/test/test-snapshot-artifact-module-a" );
        paths.add( "target/test-repository/com/example/test/test-snapshot-artifact-root" );

        deleteTestArtifactWithParent( paths );
    }

    // Tests for MRM-1411 - END

    @Test
    public void testGetRootNamespaces()
    {
        assertEquals( Arrays.asList( "com", "org" ), storage.listRootNamespaces( TEST_REPO_ID, ALL ) );
    }

    @Test
    public void testGetNamespaces()
    {
        assertEquals( Arrays.asList( "example" ), storage.listNamespaces( TEST_REPO_ID, "com", ALL ) );
        assertEquals( Arrays.asList( "test" ), storage.listNamespaces( TEST_REPO_ID, "com.example", ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listNamespaces( TEST_REPO_ID, "com.example.test",
                                                                               ALL ) );

        assertEquals( Arrays.asList( "apache", "codehaus" ), storage.listNamespaces( TEST_REPO_ID, "org", ALL ) );
        assertEquals( Arrays.asList( "archiva", "maven" ), storage.listNamespaces( TEST_REPO_ID, "org.apache", ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listNamespaces( TEST_REPO_ID, "org.apache.archiva",
                                                                               ALL ) );
        assertEquals( Arrays.asList( "plugins", "shared" ), storage.listNamespaces( TEST_REPO_ID, "org.apache.maven",
                                                                                    ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listNamespaces( TEST_REPO_ID, "org.apache.maven.plugins",
                                                                               ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listNamespaces( TEST_REPO_ID, "org.apache.maven.shared",
                                                                               ALL ) );

        assertEquals( Arrays.asList( "plexus" ), storage.listNamespaces( TEST_REPO_ID, "org.codehaus", ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listNamespaces( TEST_REPO_ID, "org.codehaus.plexus",
                                                                               ALL ) );
    }

    @Test
    public void testGetProjects()
    {
        assertEquals( Collections.<String>emptyList(), storage.listProjects( TEST_REPO_ID, "com", ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listProjects( TEST_REPO_ID, "com.example", ALL ) );
        assertEquals( Arrays.asList( "incomplete-metadata", "invalid-pom", "malformed-metadata", "mislocated-pom",
                                     "missing-metadata", "missing-parent", "test-artifact" ), storage.listProjects( TEST_REPO_ID,
                                                                                                  "com.example.test",
                                                                                                  ALL ) );

        assertEquals( Collections.<String>emptyList(), storage.listProjects( TEST_REPO_ID, "org", ALL ) );
        assertEquals( Arrays.asList( "apache" ), storage.listProjects( TEST_REPO_ID, "org.apache", ALL ) );
        assertEquals( Arrays.asList( "archiva", "archiva-base", "archiva-common", "archiva-modules", "archiva-parent" ),
                      storage.listProjects( TEST_REPO_ID, "org.apache.archiva", ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listProjects( TEST_REPO_ID, "org.apache.maven", ALL ) );
        assertEquals( Collections.<String>emptyList(), storage.listProjects( TEST_REPO_ID, "org.apache.maven.plugins",
                                                                             ALL ) );
        assertEquals( Arrays.asList( "maven-downloader" ), storage.listProjects( TEST_REPO_ID,
                                                                                 "org.apache.maven.shared", ALL ) );
    }

    @Test
    public void testGetProjectVersions()
    {
        assertEquals( Arrays.asList( "1.0-SNAPSHOT" ), storage.listProjectVersions( TEST_REPO_ID, "com.example.test",
                                                                                    "incomplete-metadata", ALL ) );
        assertEquals( Arrays.asList( "1.0-SNAPSHOT" ), storage.listProjectVersions( TEST_REPO_ID, "com.example.test",
                                                                                    "malformed-metadata", ALL ) );
        assertEquals( Arrays.asList( "1.0-SNAPSHOT" ), storage.listProjectVersions( TEST_REPO_ID, "com.example.test",
                                                                                    "missing-metadata", ALL ) );
        assertEquals( Arrays.asList( "1.0" ), storage.listProjectVersions( TEST_REPO_ID, "com.example.test",
                                                                           "invalid-pom", ALL ) );

        assertEquals( Arrays.asList( "4", "5-SNAPSHOT" ), storage.listProjectVersions( TEST_REPO_ID, "org.apache",
                                                                                       "apache", ALL ) );

        assertEquals( Arrays.asList( "1.2.1", "1.2.2" ), storage.listProjectVersions( TEST_REPO_ID,
                                                                                      "org.apache.archiva", "archiva",
                                                                                      ALL ) );
        assertEquals( Arrays.asList( "1.2.1" ), storage.listProjectVersions( TEST_REPO_ID, "org.apache.archiva",
                                                                             "archiva-base", ALL ) );
        assertEquals( Arrays.asList( "1.2.1" ), storage.listProjectVersions( TEST_REPO_ID, "org.apache.archiva",
                                                                             "archiva-common", ALL ) );
        assertEquals( Arrays.asList( "1.2.1" ), storage.listProjectVersions( TEST_REPO_ID, "org.apache.archiva",
                                                                             "archiva-modules", ALL ) );
        assertEquals( Arrays.asList( "3" ), storage.listProjectVersions( TEST_REPO_ID, "org.apache.archiva",
                                                                         "archiva-parent", ALL ) );

        assertEquals( Collections.<String>emptyList(), storage.listProjectVersions( TEST_REPO_ID,
                                                                                    "org.apache.maven.shared",
                                                                                    "maven-downloader", ALL ) );
    }

    @Test
    public void testGetArtifacts()
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>( storage.readArtifactsMetadata( TEST_REPO_ID,
                                                                                                           "org.codehaus.plexus",
                                                                                                           "plexus-spring",
                                                                                                           "1.2",
                                                                                                           ALL ) );
        assertEquals( 3, artifacts.size() );
        Collections.sort( artifacts, new Comparator<ArtifactMetadata>()
        {
            public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
            {
                return o1.getId().compareTo( o2.getId() );
            }
        } );

        assertArtifact( artifacts.get( 0 ), "plexus-spring-1.2-sources.jar", 0, EMPTY_SHA1, EMPTY_MD5 );
        assertArtifact( artifacts.get( 1 ), "plexus-spring-1.2.jar", 0, EMPTY_SHA1, EMPTY_MD5 );
        assertArtifact( artifacts.get( 2 ), "plexus-spring-1.2.pom", 7407, "96b14cf880e384b2d15e8193c57b65c5420ca4c5",
                        "f83aa25f016212a551a4b2249985effc" );
    }

    @Test
    public void testGetArtifactsFiltered()
    {
        ExcludesFilter<String> filter = new ExcludesFilter<String>( Collections.singletonList(
            "plexus-spring-1.2.pom" ) );
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>( storage.readArtifactsMetadata( TEST_REPO_ID,
                                                                                                           "org.codehaus.plexus",
                                                                                                           "plexus-spring",
                                                                                                           "1.2",
                                                                                                           filter ) );
        assertEquals( 2, artifacts.size() );
        Collections.sort( artifacts, new Comparator<ArtifactMetadata>()
        {
            public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
            {
                return o1.getId().compareTo( o2.getId() );
            }
        } );

        assertArtifact( artifacts.get( 0 ), "plexus-spring-1.2-sources.jar", 0, EMPTY_SHA1, EMPTY_MD5 );
        assertArtifact( artifacts.get( 1 ), "plexus-spring-1.2.jar", 0, EMPTY_SHA1, EMPTY_MD5 );
    }

    @Test
    public void testGetArtifactsTimestampedSnapshots()
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>( storage.readArtifactsMetadata( TEST_REPO_ID,
                                                                                                           "com.example.test",
                                                                                                           "missing-metadata",
                                                                                                           "1.0-SNAPSHOT",
                                                                                                           ALL ) );
        assertEquals( 1, artifacts.size() );

        ArtifactMetadata artifact = artifacts.get( 0 );
        assertEquals( "missing-metadata-1.0-20091101.112233-1.pom", artifact.getId() );
        assertEquals( "com.example.test", artifact.getNamespace() );
        assertEquals( "missing-metadata", artifact.getProject() );
        assertEquals( "1.0-20091101.112233-1", artifact.getVersion() );
        assertEquals( TEST_REPO_ID, artifact.getRepositoryId() );
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
        List<String> otherArchives = new ArrayList<String>();
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
        for( String path : pathsToBeDeleted )
        {
            File dir =  new File( FileUtil.getBasedir(), path );
            FileUtils.deleteDirectory( dir );

            assertFalse( dir.exists() );
        }
        File dest = new File( FileUtil.getBasedir(), "target/test-repository/com/example/test/test-artifact-module-a" );
        File parentPom = new File( FileUtil.getBasedir(), "target/test-repository/com/example/test/test-artifact-parent" );
        File rootPom = new File( FileUtil.getBasedir(), "target/test-repository/com/example/test/test-artifact-root" );

        FileUtils.deleteDirectory( dest );
        FileUtils.deleteDirectory( parentPom );
        FileUtils.deleteDirectory( rootPom );

        assertFalse( dest.exists() );
        assertFalse( parentPom.exists() );
        assertFalse( rootPom.exists() );
    }

    private File copyTestArtifactWithParent( String srcPath, String destPath )
         throws IOException
    {
        File src = new File( FileUtil.getBasedir(), srcPath );
        File dest = new File( FileUtil.getBasedir(), destPath );

        FileUtils.copyDirectory( src, dest );
        assertTrue( dest.exists() );
        return dest;
    }

}
