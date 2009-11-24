package org.apache.maven.archiva.web.action;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.opensymphony.xwork2.Action;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.memory.MemoryMetadataRepository;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectParent;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.security.UserRepositoriesStub;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class ShowArtifactActionTest
    extends PlexusInSpringTestCase
{
    private static final String ACTION_HINT = "showArtifactAction";

    private static final String TEST_GROUP_ID = "groupId";

    private static final String TEST_ARTIFACT_ID = "artifactId";

    private static final String TEST_VERSION = "version";

    private static final String TEST_PACKAGING = "packaging";

    private static final String TEST_ISSUE_URL = "http://jira.codehaus.org/browse/MRM";

    private static final String TEST_ISSUE_SYSTEM = "jira";

    private static final String TEST_CI_SYSTEM = "continuum";

    private static final String TEST_CI_URL = "http://vmbuild.apache.org/";

    private static final String TEST_URL = "url";

    private static final String TEST_NAME = "name";

    private static final String TEST_DESCRIPTION = "description";

    private static final String TEST_PARENT_GROUP_ID = "parentGroupId";

    private static final String TEST_PARENT_ARTIFACT_ID = "parentArtifactId";

    private static final String TEST_PARENT_VERSION = "parentVersion";

    private static final String TEST_ORGANIZATION_NAME = "organizationName";

    private static final String TEST_ORGANIZATION_URL = "organizationUrl";

    private static final String TEST_LICENSE_URL = "licenseUrl";

    private static final String TEST_LICENSE_NAME = "licenseName";

    private static final String TEST_LICENSE_URL_2 = "licenseUrl_2";

    private static final String TEST_LICENSE_NAME_2 = "licenseName_2";

    private static final String TEST_REPO = "test-repo";

    private static final String TEST_SCM_CONNECTION = "scmConnection";

    private static final String TEST_SCM_DEV_CONNECTION = "scmDevConnection";

    private static final String TEST_SCM_URL = "scmUrl";

    private static final String TEST_SNAPSHOT_VERSION = "1.0-SNAPSHOT";

    private static final String TEST_TS_SNAPSHOT_VERSION = "1.0-20091120.111111-1";

    private static final List<String> ALL_TEST_SNAPSHOT_VERSIONS =
        Arrays.asList( TEST_TS_SNAPSHOT_VERSION, "1.0-20091120.222222-2", "1.0-20091123.333333-3" );

    private static final String OTHER_TEST_REPO = "first-repo";

    private ShowArtifactAction action;

    private MemoryMetadataRepository metadataRepository;

    public void testInstantiation()
    {
        assertFalse( action == lookup( Action.class, ACTION_HINT ) );
    }

    public void testGetArtifactUniqueRelease()
    {
        metadataRepository.setProjectBuild( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getSnapshotVersions().isEmpty() );
    }

    public void testGetArtifactUniqueSnapshot()
    {
        metadataRepository.setProjectBuild( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_SNAPSHOT_VERSION ) );
        metadataRepository.setArtifactVersions( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION,
                                                ALL_TEST_SNAPSHOT_VERSIONS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_SNAPSHOT_VERSION );

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertEquals( TEST_GROUP_ID, action.getGroupId() );
        assertEquals( TEST_ARTIFACT_ID, action.getArtifactId() );
        assertEquals( TEST_SNAPSHOT_VERSION, action.getVersion() );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model, TEST_SNAPSHOT_VERSION );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertEquals( ALL_TEST_SNAPSHOT_VERSIONS, action.getSnapshotVersions() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
    }

    public void testGetArtifactUniqueSnapshotTimestamped()
    {
        metadataRepository.setProjectBuild( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_TS_SNAPSHOT_VERSION ) );
        metadataRepository.setArtifactVersions( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_TS_SNAPSHOT_VERSION,
                                                ALL_TEST_SNAPSHOT_VERSIONS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_TS_SNAPSHOT_VERSION );

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertEquals( TEST_GROUP_ID, action.getGroupId() );
        assertEquals( TEST_ARTIFACT_ID, action.getArtifactId() );
        assertEquals( TEST_TS_SNAPSHOT_VERSION, action.getVersion() );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model, TEST_TS_SNAPSHOT_VERSION );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertEquals( Arrays.asList( ALL_TEST_SNAPSHOT_VERSIONS.get( 1 ), ALL_TEST_SNAPSHOT_VERSIONS.get( 2 ) ),
                      action.getSnapshotVersions() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
    }

    public void testGetMissingProject()
    {
        setActionParameters();

        String result = action.artifact();
        assertError( result );

        assertActionParameters( action );
        assertNoOutputFields();
    }

    public void testGetArtifactNoObservableRepos()
    {
        setObservableRepos( Collections.<String>emptyList() );

        setActionParameters();

        String result = action.artifact();

        // Actually, it'd be better to have an error:
        assertError( result );
        assertActionParameters( action );
        assertNoOutputFields();
    }

    public void testGetArtifactNotInObservableRepos()
    {
        metadataRepository.setProjectBuild( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();
        assertError( result );

        assertActionParameters( action );
        assertNoOutputFields();
    }

    public void testGetArtifactOnlySeenInSecondObservableRepo()
    {
        setObservableRepos( Arrays.asList( OTHER_TEST_REPO, TEST_REPO ) );
        metadataRepository.setProjectBuild( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getSnapshotVersions().isEmpty() );
    }

    public void testGetArtifactSeenInBothObservableRepo()
    {
        setObservableRepos( Arrays.asList( TEST_REPO, OTHER_TEST_REPO ) );
        metadataRepository.setProjectBuild( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );
        metadataRepository.setProjectBuild( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getSnapshotVersions().isEmpty() );
    }

    public void testGetArtifactCanOnlyObserveInOneOfTwoRepos()
    {
        setObservableRepos( Arrays.asList( TEST_REPO ) );
        metadataRepository.setProjectBuild( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );
        metadataRepository.setProjectBuild( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getSnapshotVersions().isEmpty() );
    }

    private void assertNoOutputFields()
    {
        assertNull( action.getModel() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getSnapshotVersions().isEmpty() );
    }

    private void assertError( String result )
    {
        assertEquals( Action.ERROR, result );
        assertEquals( 1, action.getActionErrors().size() );
    }

    private void setObservableRepos( List<String> repoIds )
    {
        UserRepositoriesStub repos = (UserRepositoriesStub) lookup( UserRepositories.class );
        repos.setObservableRepositoryIds( repoIds );
    }

    private void assertDefaultModel( ArchivaProjectModel model )
    {
        assertDefaultModel( model, TEST_VERSION );
    }

    private void assertDefaultModel( ArchivaProjectModel model, String version )
    {
        assertEquals( TEST_GROUP_ID, model.getGroupId() );
        assertEquals( TEST_ARTIFACT_ID, model.getArtifactId() );
        assertEquals( version, model.getVersion() );
        assertEquals( TEST_URL, model.getUrl() );
        assertEquals( TEST_NAME, model.getName() );
        assertEquals( TEST_DESCRIPTION, model.getDescription() );
        assertEquals( TEST_ORGANIZATION_NAME, model.getOrganization().getName() );
        assertEquals( TEST_ORGANIZATION_URL, model.getOrganization().getUrl() );
        assertEquals( 2, model.getLicenses().size() );
        org.apache.maven.archiva.model.License l = model.getLicenses().get( 0 );
        assertEquals( TEST_LICENSE_NAME, l.getName() );
        assertEquals( TEST_LICENSE_URL, l.getUrl() );
        l = model.getLicenses().get( 1 );
        assertEquals( TEST_LICENSE_NAME_2, l.getName() );
        assertEquals( TEST_LICENSE_URL_2, l.getUrl() );
        assertEquals( TEST_ISSUE_SYSTEM, model.getIssueManagement().getSystem() );
        assertEquals( TEST_ISSUE_URL, model.getIssueManagement().getUrl() );
        assertEquals( TEST_CI_SYSTEM, model.getCiManagement().getSystem() );
        assertEquals( TEST_CI_URL, model.getCiManagement().getUrl() );
        assertEquals( TEST_SCM_CONNECTION, model.getScm().getConnection() );
        assertEquals( TEST_SCM_DEV_CONNECTION, model.getScm().getDeveloperConnection() );
        assertEquals( TEST_SCM_URL, model.getScm().getUrl() );

        assertEquals( TEST_PACKAGING, model.getPackaging() );
        assertEquals( TEST_PARENT_GROUP_ID, model.getParentProject().getGroupId() );
        assertEquals( TEST_PARENT_ARTIFACT_ID, model.getParentProject().getArtifactId() );
        assertEquals( TEST_PARENT_VERSION, model.getParentProject().getVersion() );
    }

    private void setActionParameters()
    {
        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_VERSION );
    }

    private void assertActionParameters( ShowArtifactAction action )
    {
        assertEquals( TEST_GROUP_ID, action.getGroupId() );
        assertEquals( TEST_ARTIFACT_ID, action.getArtifactId() );
        assertEquals( TEST_VERSION, action.getVersion() );
    }

    private void assertActionSuccess( ShowArtifactAction action, String result )
    {
        assertEquals( Action.SUCCESS, result );
        assertTrue( action.getActionErrors().isEmpty() );
        assertTrue( action.getActionMessages().isEmpty() );
    }

    private ProjectBuildMetadata createProjectModel( String version )
    {
        ProjectBuildMetadata model = new ProjectBuildMetadata();
        model.setId( version );
        model.setUrl( TEST_URL );
        model.setName( TEST_NAME );
        model.setDescription( TEST_DESCRIPTION );
        CiManagement ci = new CiManagement();
        ci.setSystem( TEST_CI_SYSTEM );
        ci.setUrl( TEST_CI_URL );
        model.setCiManagement( ci );
        IssueManagement issue = new IssueManagement();
        issue.setSystem( TEST_ISSUE_SYSTEM );
        issue.setUrl( TEST_ISSUE_URL );
        model.setIssueManagement( issue );
        Organization organization = new Organization();
        organization.setName( TEST_ORGANIZATION_NAME );
        organization.setUrl( TEST_ORGANIZATION_URL );
        model.setOrganization( organization );
        License l = new License();
        l.setName( TEST_LICENSE_NAME );
        l.setUrl( TEST_LICENSE_URL );
        model.addLicense( l );
        l = new License();
        l.setName( TEST_LICENSE_NAME_2 );
        l.setUrl( TEST_LICENSE_URL_2 );
        model.addLicense( l );
        Scm scm = new Scm();
        scm.setConnection( TEST_SCM_CONNECTION );
        scm.setDeveloperConnection( TEST_SCM_DEV_CONNECTION );
        scm.setUrl( TEST_SCM_URL );
        model.setScm( scm );

        MavenProjectFacet mavenProjectFacet = new MavenProjectFacet();
        mavenProjectFacet.setGroupId( TEST_GROUP_ID );
        mavenProjectFacet.setArtifactId( TEST_ARTIFACT_ID );
        mavenProjectFacet.setPackaging( TEST_PACKAGING );
        MavenProjectParent parent = new MavenProjectParent();
        parent.setGroupId( TEST_PARENT_GROUP_ID );
        parent.setArtifactId( TEST_PARENT_ARTIFACT_ID );
        parent.setVersion( TEST_PARENT_VERSION );
        mavenProjectFacet.setParent( parent );
        model.addFacet( mavenProjectFacet );
        return model;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        action = (ShowArtifactAction) lookup( Action.class, ACTION_HINT );
        metadataRepository = (MemoryMetadataRepository) action.getMetadataRepository();
    }
}
