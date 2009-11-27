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
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsRelatedConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.security.UserRepositoriesStub;
import org.apache.maven.archiva.web.action.admin.repositories.ArchivaDAOStub;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

public class ShowArtifactActionTest
    extends AbstractActionTestCase
{
    private static final String ACTION_HINT = "showArtifactAction";

    private static final String TEST_VERSION = "version";

    private ShowArtifactAction action;

    private static final String TEST_SNAPSHOT_VERSION = "1.0-SNAPSHOT";

    private static final String TEST_TS_SNAPSHOT_VERSION = "1.0-20091120.111111-1";

    private ArchivaDAOStub archivaDao;

    private static final List<String> ALL_TEST_SNAPSHOT_VERSIONS =
        Arrays.asList( TEST_TS_SNAPSHOT_VERSION, "1.0-20091120.222222-2", "1.0-20091123.333333-3" );

    private static final String OTHER_TEST_REPO = "first-repo";

    public void testInstantiation()
    {
        assertFalse( action == lookup( Action.class, ACTION_HINT ) );
    }

    public void testGetArtifactUniqueRelease()
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifact> artifacts =
            Collections.singletonList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, 2 );
        MockControl projectDaoMockControl =
            createProjectDaoMock( createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    public void testGetArtifactUniqueSnapshot()
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifact> artifacts =
            Collections.singletonList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, TEST_SNAPSHOT_VERSION, 2 );
        MockControl projectDaoMockControl =
            createProjectDaoMock( createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_SNAPSHOT_VERSION ) );
        archivaDao.setVersions( ALL_TEST_SNAPSHOT_VERSIONS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_SNAPSHOT_VERSION );

        String result = action.artifact();

        assertActionSuccess( action, result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

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
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifact> artifacts =
            Collections.singletonList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_TS_SNAPSHOT_VERSION ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, TEST_TS_SNAPSHOT_VERSION, 2 );
        MockControl projectDaoMockControl =
            createProjectDaoMock( createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_TS_SNAPSHOT_VERSION ) );
        archivaDao.setVersions( ALL_TEST_SNAPSHOT_VERSIONS );

        action.setGroupId( TEST_GROUP_ID );
        action.setArtifactId( TEST_ARTIFACT_ID );
        action.setVersion( TEST_TS_SNAPSHOT_VERSION );

        String result = action.artifact();

        assertActionSuccess( action, result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

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
        throws ArchivaDatabaseException
    {
        MockControl artifactDaoMockControl = createArtifactDaoMock( Collections.<ArchivaArtifact>emptyList(), 1 );

        setActionParameters();

        String result = action.artifact();
        assertError( result );

        artifactDaoMockControl.verify();

        assertActionParameters( action );
        assertNoOutputFields();
    }

    public void testGetArtifactNoObservableRepos()
        throws ArchivaDatabaseException
    {
        setObservableRepos( Collections.<String>emptyList() );

        setActionParameters();

        try
        {
            action.artifact();

            // Actually, it'd be better to have an error:
//            assertError( result );
//            assertActionParameters( action );
//            assertNoOutputFields();
            fail();
        }
        catch ( ArchivaDatabaseException e )
        {
            assertTrue( true );
        }
    }

    public void testGetArtifactNotInObservableRepos()
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifact> artifacts = Collections.singletonList(
            createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION, OTHER_TEST_REPO ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, 1 );

        setActionParameters();

        String result = action.artifact();
        assertError( result );

        artifactDaoMockControl.verify();

        assertActionParameters( action );
        assertNoOutputFields();
    }

    public void testGetArtifactOnlySeenInSecondObservableRepo()
        throws ArchivaDatabaseException
    {
        setObservableRepos( Arrays.asList( OTHER_TEST_REPO, TEST_REPO ) );
        List<ArchivaArtifact> artifacts =
            Collections.singletonList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, 2 );
        MockControl projectDaoMockControl =
            createProjectDaoMock( createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    public void testGetArtifactSeenInBothObservableRepo()
        throws ArchivaDatabaseException
    {
        setObservableRepos( Arrays.asList( OTHER_TEST_REPO, TEST_REPO ) );
        List<ArchivaArtifact> artifacts =
            Arrays.asList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ),
                           createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION, OTHER_TEST_REPO ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, 2 );
        MockControl projectDaoMockControl =
            createProjectDaoMock( createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    public void testGetArtifactCanOnlyObserveInOneOfTwoRepos()
        throws ArchivaDatabaseException
    {
        setObservableRepos( Arrays.asList( TEST_REPO ) );
        List<ArchivaArtifact> artifacts =
            Arrays.asList( createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION, OTHER_TEST_REPO ),
                           createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );
        MockControl artifactDaoMockControl = createArtifactDaoMock( artifacts, 2 );
        MockControl projectDaoMockControl =
            createProjectDaoMock( createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ) );

        setActionParameters();

        String result = action.artifact();

        assertActionSuccess( action, result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertActionParameters( action );
        ArchivaProjectModel model = action.getModel();
        assertDefaultModel( model );

        assertEquals( TEST_REPO, action.getRepositoryId() );

        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    private void assertNoOutputFields()
    {
        assertNull( action.getModel() );
        assertNull( action.getDependees() );
        assertNull( action.getDependencies() );
        assertNull( action.getMailingLists() );
        assertNull( action.getSnapshotVersions() );
    }

    private void assertError( String result )
    {
        assertEquals( Action.ERROR, result );
        assertEquals( 1, action.getActionErrors().size() );
    }

    private void assertDefaultModel( ArchivaProjectModel model )
    {
        assertDefaultModel( model, TEST_VERSION );
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

    private ArchivaProjectModel createProjectModel( String groupId, String artifactId, String version )
    {
        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( TEST_PACKAGING );
        model.setUrl( TEST_URL );
        model.setName( TEST_NAME );
        model.setDescription( TEST_DESCRIPTION );
        VersionedReference parent = new VersionedReference();
        parent.setGroupId( TEST_PARENT_GROUP_ID );
        parent.setArtifactId( TEST_PARENT_ARTIFACT_ID );
        parent.setVersion( TEST_PARENT_VERSION );
        model.setParentProject( parent );
        CiManagement ci = new CiManagement();
        ci.setSystem( TEST_CI_SYSTEM );
        ci.setUrl( TEST_CI_URL );
        model.setCiManagement( ci );
        IssueManagement issue = new IssueManagement();
        issue.setSystem( TEST_ISSUE_SYSTEM );
        issue.setUrl( TEST_ISSUE_URL );
        model.setIssueManagement( issue );
        Organization org = new Organization();
        org.setName( TEST_ORGANIZATION_NAME );
        org.setUrl( TEST_ORGANIZATION_URL );
        model.setOrganization( org );
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

    private MockControl createProjectDaoMock( ArchivaProjectModel project )
        throws ArchivaDatabaseException
    {
        MockControl control = MockControl.createNiceControl( ProjectModelDAO.class );
        ProjectModelDAO dao = (ProjectModelDAO) control.getMock();
        archivaDao.setProjectDao( dao );

        control.expectAndReturn(
            dao.getProjectModel( project.getGroupId(), project.getArtifactId(), project.getVersion() ), project );

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
        archivaDao = (ArchivaDAOStub) lookup( ArchivaDAO.class, "jdo" );
    }
}
