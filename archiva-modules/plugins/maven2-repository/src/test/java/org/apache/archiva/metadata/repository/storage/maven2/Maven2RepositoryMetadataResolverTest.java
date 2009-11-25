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

import java.util.Arrays;

import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.MetadataResolverException;
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

        resolver = (Maven2RepositoryMetadataResolver) lookup( MetadataResolver.class, "maven2" );
    }

    public void testGetProjectVersionMetadata()
        throws MetadataResolverException
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
    }

    public void testGetProjectVersionMetadataForTimestampedSnapshot()
        throws MetadataResolverException
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
    }

    public void testGetProjectVersionMetadataForTimestampedSnapshotMissingMetadata()
        throws MetadataResolverException
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "missing-metadata", "1.0-SNAPSHOT" );
        assertNull( metadata );
    }

    public void testGetProjectVersionMetadataForTimestampedSnapshotMalformedMetadata()
        throws MetadataResolverException
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "malformed-metadata", "1.0-SNAPSHOT" );
        assertNull( metadata );
    }

    public void testGetProjectVersionMetadataForTimestampedSnapshotIncompleteMetadata()
        throws MetadataResolverException
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "incomplete-metadata", "1.0-SNAPSHOT" );
        assertNull( metadata );
    }

    public void testGetProjectVersionMetadataForInvalidPom()
    {
        try
        {
            ProjectVersionMetadata metadata =
                resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "invalid-pom", "1.0" );

            fail( "Expected failure, but received metadata: " + metadata );
        }
        catch ( MetadataResolverException e )
        {
            assertTrue( true );
        }
    }

    public void testGetProjectVersionMetadataForMissingPom()
        throws MetadataResolverException
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "com.example.test", "missing-pom", "1.0" );
        assertNull( metadata );

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
