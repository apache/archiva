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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.filter.ExcludesFilter;
import org.apache.archiva.metadata.repository.storage.StorageMetadataResolver;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class Maven2RepositoryMetadataResolverTest
    extends PlexusInSpringTestCase
{
    private Maven2RepositoryMetadataResolver resolver;

    private static final String TEST_REPO_ID = "test";

    private static final String ASF_SCM_CONN_BASE = "scm:svn:http://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_DEV_CONN_BASE = "scm:svn:https://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_VIEWVC_BASE = "http://svn.apache.org/viewvc/";

    private static final String EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";

    private static final String EMPTY_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    private MetadataRepository metadataRepository;

    public void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaConfiguration configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( getTestPath( "src/test/repositories/test" ) );
        c.addManagedRepository( testRepo );
        configuration.save( c );

        resolver = (Maven2RepositoryMetadataResolver) lookup( StorageMetadataResolver.class, "maven2" );
        metadataRepository = (MetadataRepository) lookup( MetadataRepository.class );
        metadataRepository.removeMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID );
    }

    public void testGetProjectVersionMetadata()
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "org.apache.archiva", "archiva-common", "1.2.1" );
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

    public void testGetProjectVersionMetadataForTimestampedSnapshot()
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "org.apache", "apache", "5-SNAPSHOT" );
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

    public void testGetProjectVersionMetadataForTimestampedSnapshotMissingMetadata()
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "missing-metadata", "1.0-SNAPSHOT" );
        assertNull( metadata );
    }

    public void testGetProjectVersionMetadataForTimestampedSnapshotMalformedMetadata()
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "malformed-metadata", "1.0-SNAPSHOT" );
        assertNull( metadata );
    }

    public void testGetProjectVersionMetadataForTimestampedSnapshotIncompleteMetadata()
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "incomplete-metadata", "1.0-SNAPSHOT" );
        assertNull( metadata );
    }

    public void testGetProjectVersionMetadataForInvalidPom()
    {
        assertTrue( metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID ).isEmpty() );

        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "invalid-pom", "1.0" );
        assertNull( metadata );

        assertFalse( metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID ).isEmpty() );
        RepositoryProblemFacet facet =
            (RepositoryProblemFacet) metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID,
                                                                          "com.example.test/invalid-pom/1.0" );
        assertEquals( "invalid-pom", facet.getProblem() );
    }

    public void testGetProjectVersionMetadataForMislocatedPom()
    {
        assertTrue( metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID ).isEmpty() );

        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "mislocated-pom", "1.0" );
        assertNull( metadata );

        assertFalse( metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID ).isEmpty() );
        RepositoryProblemFacet facet =
            (RepositoryProblemFacet) metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID,
                                                                          "com.example.test/mislocated-pom/1.0" );
        assertEquals( "mislocated-pom", facet.getProblem() );
    }

    public void testGetProjectVersionMetadataForMissingPom()
    {
        assertTrue( metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID ).isEmpty() );

        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "missing-pom", "1.0" );
        assertNull( metadata );

        assertFalse( metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID ).isEmpty() );
        RepositoryProblemFacet facet =
            (RepositoryProblemFacet) metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryProblemFacet.FACET_ID,
                                                                          "com.example.test/missing-pom/1.0" );
        assertEquals( "missing-pom", facet.getProblem() );

    }

    public void testGetRootNamespaces()
    {
        assertEquals( Arrays.asList( "com", "org" ), resolver.getRootNamespaces( TEST_REPO_ID ) );
    }

    public void testGetNamespaces()
    {
        assertEquals( Arrays.asList( "example" ), resolver.getNamespaces( TEST_REPO_ID, "com" ) );
        assertEquals( Arrays.asList( "test" ), resolver.getNamespaces( TEST_REPO_ID, "com.example" ) );
        assertEquals( Collections.<String>emptyList(), resolver.getNamespaces( TEST_REPO_ID, "com.example.test" ) );

        assertEquals( Arrays.asList( "apache", "codehaus" ), resolver.getNamespaces( TEST_REPO_ID, "org" ) );
        assertEquals( Arrays.asList( "archiva", "maven" ), resolver.getNamespaces( TEST_REPO_ID, "org.apache" ) );
        assertEquals( Collections.<String>emptyList(), resolver.getNamespaces( TEST_REPO_ID, "org.apache.archiva" ) );
        assertEquals( Arrays.asList( "plugins", "shared" ),
                      resolver.getNamespaces( TEST_REPO_ID, "org.apache.maven" ) );
        assertEquals( Collections.<String>emptyList(),
                      resolver.getNamespaces( TEST_REPO_ID, "org.apache.maven.plugins" ) );
        assertEquals( Collections.<String>emptyList(),
                      resolver.getNamespaces( TEST_REPO_ID, "org.apache.maven.shared" ) );

        assertEquals( Arrays.asList( "plexus" ), resolver.getNamespaces( TEST_REPO_ID, "org.codehaus" ) );
        assertEquals( Collections.<String>emptyList(), resolver.getNamespaces( TEST_REPO_ID, "org.codehaus.plexus" ) );
    }

    public void testGetProjects()
    {
        assertEquals( Collections.<String>emptyList(), resolver.getProjects( TEST_REPO_ID, "com" ) );
        assertEquals( Collections.<String>emptyList(), resolver.getProjects( TEST_REPO_ID, "com.example" ) );
        assertEquals( Arrays.asList( "incomplete-metadata", "invalid-pom", "malformed-metadata", "mislocated-pom",
                                     "missing-metadata" ), resolver.getProjects( TEST_REPO_ID, "com.example.test" ) );

        assertEquals( Collections.<String>emptyList(), resolver.getProjects( TEST_REPO_ID, "org" ) );
        assertEquals( Arrays.asList( "apache" ), resolver.getProjects( TEST_REPO_ID, "org.apache" ) );
        assertEquals( Arrays.asList( "archiva", "archiva-base", "archiva-common", "archiva-modules", "archiva-parent" ),
                      resolver.getProjects( TEST_REPO_ID, "org.apache.archiva" ) );
        assertEquals( Collections.<String>emptyList(), resolver.getProjects( TEST_REPO_ID, "org.apache.maven" ) );
        assertEquals( Collections.<String>emptyList(),
                      resolver.getProjects( TEST_REPO_ID, "org.apache.maven.plugins" ) );
        assertEquals( Arrays.asList( "maven-downloader" ),
                      resolver.getProjects( TEST_REPO_ID, "org.apache.maven.shared" ) );
    }

    public void testGetProjectVersions()
    {
        assertEquals( Arrays.asList( "1.0-SNAPSHOT" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "com.example.test", "incomplete-metadata" ) );
        assertEquals( Arrays.asList( "1.0-SNAPSHOT" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "com.example.test", "malformed-metadata" ) );
        assertEquals( Arrays.asList( "1.0-SNAPSHOT" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "com.example.test", "missing-metadata" ) );
        assertEquals( Arrays.asList( "1.0" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "com.example.test", "invalid-pom" ) );

        assertEquals( Arrays.asList( "4", "5-SNAPSHOT" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache", "apache" ) );

        assertEquals( Arrays.asList( "1.2.1" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "archiva" ) );
        assertEquals( Arrays.asList( "1.2.1" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "archiva-base" ) );
        assertEquals( Arrays.asList( "1.2.1" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "archiva-common" ) );
        assertEquals( Arrays.asList( "1.2.1" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "archiva-modules" ) );
        assertEquals( Arrays.asList( "3" ),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "archiva-parent" ) );

        assertEquals( Collections.<String>emptyList(),
                      resolver.getProjectVersions( TEST_REPO_ID, "org.apache.maven.shared", "maven-downloader" ) );
    }

    public void testGetArtifacts()
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>(
            resolver.getArtifacts( TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.2" ) );
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

    public void testGetArtifactsFiltered()
    {
        ExcludesFilter<String> filter =
            new ExcludesFilter<String>( Collections.singletonList( "plexus-spring-1.2.pom" ) );
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>(
            resolver.getArtifacts( TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.2", filter ) );
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

    public void testGetArtifactsTimestampedSnapshots()
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>(
            resolver.getArtifacts( TEST_REPO_ID, "com.example.test", "missing-metadata", "1.0-SNAPSHOT" ) );
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
}
