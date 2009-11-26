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
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.memory.TestMetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectParent;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsRelatedConstraint;
import org.apache.maven.archiva.database.constraints.ProjectsByArtifactUsageConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.security.UserRepositoriesStub;
import org.apache.maven.archiva.web.action.admin.repositories.ArchivaDAOStub;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

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

    private ArchivaDAOStub archivaDao;

    private TestMetadataResolver metadataResolver;

    public void testInstantiation()
    {
        assertFalse( action == lookup( Action.class, ACTION_HINT ) );
    }

    public void testGetArtifactUniqueRelease()
    {
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
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
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_SNAPSHOT_VERSION ) );
        metadataResolver.setArtifactVersions( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION,
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
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_TS_SNAPSHOT_VERSION ) );
        metadataResolver.setArtifactVersions( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_TS_SNAPSHOT_VERSION,
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
        metadataResolver.setProjectVersion( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
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
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
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
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );
        metadataResolver.setProjectVersion( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
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
        metadataResolver.setProjectVersion( OTHER_TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
                                            createProjectModel( TEST_VERSION ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID,
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

    public void testGetArtifactNoMavenFacet()
    {
        ProjectVersionMetadata versionMetadata = new ProjectVersionMetadata();
        versionMetadata.setId( TEST_VERSION );
        versionMetadata.setUrl( TEST_URL );
        versionMetadata.setName( TEST_NAME );
        versionMetadata.setDescription( TEST_DESCRIPTION );

        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertEquals( TEST_VERSION, model.getVersion() );
        assertEquals( TEST_URL, model.getUrl() );
        assertEquals( TEST_NAME, model.getName() );
        assertEquals( TEST_DESCRIPTION, model.getDescription() );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertTrue( action.getSnapshotVersions().isEmpty() );
    }

    public void testGetMailingLists()
        throws ArchivaDatabaseException
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );
        MailingList ml1 = createMailingList( "Users List", "users" );
        MailingList ml2 = createMailingList( "Developers List", "dev" );
        versionMetadata.setMailingLists( Arrays.asList( ml1, ml2 ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.mailingLists();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertNotNull( action.getMailingLists() );
        assertMailingList( action.getMailingLists().get( 0 ), "Users List", "users" );
        assertMailingList( action.getMailingLists().get( 1 ), "Developers List", "dev" );

        assertNull( action.getRepositoryId() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getSnapshotVersions() );
    }

    public void testGetDependencies()
        throws ArchivaDatabaseException
    {
        ProjectVersionMetadata versionMetadata = createProjectModel( TEST_VERSION );
        Dependency dependency1 = createDependencyBasic( "artifactId1" );
        Dependency dependency2 = createDependencyExtended( "artifactId2" );
        versionMetadata.setDependencies( Arrays.asList( dependency1, dependency2 ) );
        metadataResolver.setProjectVersion( TEST_REPO, TEST_GROUP_ID, TEST_ARTIFACT_ID, versionMetadata );

        setActionParameters();

        String result = action.dependencies();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertNotNull( action.getDependencies() );
        assertDependencyBasic( action.getDependencies().get( 0 ), "artifactId1" );
        assertDependencyExtended( action.getDependencies().get( 1 ), "artifactId2" );

        assertNull( action.getRepositoryId() );
        assertNull( action.getDependees() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    public void testGetDependees()
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifact> artifacts =
            Collections.singletonList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, 1 );
        ArchivaProjectModel legacyModel = createLegacyProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION );

        MockControl projectDaoMockControl = MockControl.createNiceControl( ProjectModelDAO.class );
        ProjectModelDAO dao = (ProjectModelDAO) projectDaoMockControl.getMock();
        archivaDao.setProjectDao( dao );

        projectDaoMockControl.expectAndReturn(
            dao.getProjectModel( legacyModel.getGroupId(), legacyModel.getArtifactId(), legacyModel.getVersion() ),
            legacyModel );

        ArchivaProjectModel dependee1 = createBasicLegacyModel( "groupId", "artifactId1", "version" );
        ArchivaProjectModel dependee2 = createBasicLegacyModel( "groupId", "artifactId2", "version" );
        projectDaoMockControl.expectAndReturn( dao.queryProjectModels(
            new ProjectsByArtifactUsageConstraint( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) ),
                                               Arrays.asList( dependee1, dependee2 ) );

        projectDaoMockControl.replay();

        setActionParameters();

        String result = action.dependees();

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertActionSuccess( action, result );

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertNotNull( action.getDependees() );
        assertCoordinate( action.getDependees().get( 0 ), "artifactId1" );
        assertCoordinate( action.getDependees().get( 1 ), "artifactId2" );

        assertNull( action.getRepositoryId() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    private void assertCoordinate( ArchivaProjectModel dependee, String artifactId )
    {
        assertEquals( artifactId, dependee.getArtifactId() );
        assertEquals( "groupId", dependee.getGroupId() );
        assertEquals( "version", dependee.getVersion() );
    }

    private void assertDependencyBasic( org.apache.maven.archiva.model.Dependency dependency, String artifactId )
    {
        assertEquals( artifactId, dependency.getArtifactId() );
        assertEquals( "groupId", dependency.getGroupId() );
        assertEquals( "version", dependency.getVersion() );
    }

    private void assertDependencyExtended( org.apache.maven.archiva.model.Dependency dependency, String artifactId )
    {
        assertDependencyBasic( dependency, artifactId );
        assertEquals( true, dependency.isOptional() );
        assertEquals( "classifier", dependency.getClassifier() );
        assertEquals( "type", dependency.getType() );
        assertEquals( "scope", dependency.getScope() );
        assertEquals( "systemPath", dependency.getSystemPath() );
    }

    private Dependency createDependencyExtended( String artifactId )
    {
        Dependency dependency = createDependencyBasic( artifactId );
        dependency.setClassifier( "classifier" );
        dependency.setOptional( true );
        dependency.setScope( "scope" );
        dependency.setSystemPath( "systemPath" );
        dependency.setType( "type" );
        return dependency;
    }

    private Dependency createDependencyBasic( String artifactId )
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( artifactId );
        dependency.setGroupId( "groupId" );
        dependency.setVersion( "version" );
        return dependency;
    }

    private void assertMailingList( org.apache.maven.archiva.model.MailingList mailingList, String name, String prefix )
    {
        assertEquals( name, mailingList.getName() );
        assertEquals( prefix + "-post@", mailingList.getPostAddress() );
        assertEquals( prefix + "-subscribe@", mailingList.getSubscribeAddress() );
        assertEquals( prefix + "-unsubscribe@", mailingList.getUnsubscribeAddress() );
        assertEquals( prefix + "-archive-url", mailingList.getMainArchiveUrl() );
        assertEquals( Arrays.asList( "other-" + prefix + "-archive-url-1", "other-" + prefix + "-archive-url-2" ),
                      mailingList.getOtherArchives() );
    }

    private MailingList createMailingList( String name, String prefix )
    {
        MailingList ml1 = new MailingList();
        ml1.setName( name );
        ml1.setPostAddress( prefix + "-post@" );
        ml1.setSubscribeAddress( prefix + "-subscribe@" );
        ml1.setUnsubscribeAddress( prefix + "-unsubscribe@" );
        ml1.setMainArchiveUrl( prefix + "-archive-url" );
        ml1.setOtherArchives(
            Arrays.asList( "other-" + prefix + "-archive-url-1", "other-" + prefix + "-archive-url-2" ) );
        return ml1;
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

    private ProjectVersionMetadata createProjectModel( String version )
    {
        ProjectVersionMetadata model = new ProjectVersionMetadata();
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

    private ArchivaProjectModel createLegacyProjectModel( String groupId, String artifactId, String version )
    {
        ArchivaProjectModel model = createBasicLegacyModel( groupId, artifactId, version );
        model.setPackaging( TEST_PACKAGING );
        model.setUrl( TEST_URL );
        model.setName( TEST_NAME );
        model.setDescription( TEST_DESCRIPTION );
        VersionedReference parent = new VersionedReference();
        parent.setGroupId( TEST_PARENT_GROUP_ID );
        parent.setArtifactId( TEST_PARENT_ARTIFACT_ID );
        parent.setVersion( TEST_PARENT_VERSION );
        model.setParentProject( parent );
        org.apache.maven.archiva.model.CiManagement ci = new org.apache.maven.archiva.model.CiManagement();
        ci.setSystem( TEST_CI_SYSTEM );
        ci.setUrl( TEST_CI_URL );
        model.setCiManagement( ci );
        org.apache.maven.archiva.model.IssueManagement issue = new org.apache.maven.archiva.model.IssueManagement();
        issue.setSystem( TEST_ISSUE_SYSTEM );
        issue.setUrl( TEST_ISSUE_URL );
        model.setIssueManagement( issue );
        org.apache.maven.archiva.model.Organization org = new org.apache.maven.archiva.model.Organization();
        org.setName( TEST_ORGANIZATION_NAME );
        org.setUrl( TEST_ORGANIZATION_URL );
        model.setOrganization( org );
        org.apache.maven.archiva.model.License l = new org.apache.maven.archiva.model.License();
        l.setName( TEST_LICENSE_NAME );
        l.setUrl( TEST_LICENSE_URL );
        model.addLicense( l );
        l = new org.apache.maven.archiva.model.License();
        l.setName( TEST_LICENSE_NAME_2 );
        l.setUrl( TEST_LICENSE_URL_2 );
        model.addLicense( l );
        org.apache.maven.archiva.model.Scm scm = new org.apache.maven.archiva.model.Scm();
        scm.setConnection( TEST_SCM_CONNECTION );
        scm.setDeveloperConnection( TEST_SCM_DEV_CONNECTION );
        scm.setUrl( TEST_SCM_URL );
        model.setScm( scm );
        return model;
    }

    private ArchivaProjectModel createBasicLegacyModel( String groupId, String artifactId, String version )
    {
        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return model;
    }

    private MockControl createArtifactDaoMock( List<ArchivaArtifact> artifacts, int count )
        throws ArchivaDatabaseException
    {
        return createArtifactDaoMock( artifacts, TEST_VERSION, count );
    }

    private MockControl createArtifactDaoMock( List<ArchivaArtifact> artifacts, String version, int count )
        throws ArchivaDatabaseException
    {
        // testing deeper than normal with the mocks as we intend to replace RepositoryBrowsing, not just the database
        // underneath it - those sections will be adjusted with a mock content repository later
        MockControl control = MockControl.createNiceControl( ArtifactDAO.class );
        ArtifactDAO dao = (ArtifactDAO) control.getMock();
        archivaDao.setArtifactDao( dao );

        ArtifactsRelatedConstraint c = new ArtifactsRelatedConstraint( TEST_GROUP_ID, TEST_ARTIFACT_ID, version );
        dao.queryArtifacts( c );
        control.setReturnValue( artifacts, count );

        control.replay();
        return control;
    }

    private ArchivaArtifact createArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, TEST_REPO );
    }

    private ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String repoId )
    {
        ArchivaArtifactModel model = new ArchivaArtifactModel();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setRepositoryId( repoId );
        return new ArchivaArtifact( model );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        action = (ShowArtifactAction) lookup( Action.class, ACTION_HINT );
        metadataResolver = (TestMetadataResolver) action.getMetadataResolver();
        archivaDao = (ArchivaDAOStub) lookup( ArchivaDAO.class, "jdo" );
    }
}
