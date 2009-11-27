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
import java.util.Collections;
import java.util.List;

import com.opensymphony.xwork2.Action;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.memory.TestMetadataResolver;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.database.browsing.BrowsingResults;
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
import org.apache.maven.archiva.web.action.admin.repositories.ArchivaDAOStub;
import org.easymock.MockControl;

public class BrowseActionTest
    extends AbstractActionTestCase
{
    private static final String ACTION_HINT = "browseAction";

    private BrowseAction action;

    private ArchivaDAOStub archivaDao;

    private static final List<String> GROUPS =
        Arrays.asList( "org.apache.archiva", "commons-lang", "org.apache.maven", "com.sun", "com.oracle",
                       "repeat.repeat", "org.apache", "single.group" );

    public void testInstantiation()
    {
        assertFalse( action == lookup( Action.class, ACTION_HINT ) );
    }

    public void testBrowse()
    {
        metadataResolver.setNamespaces( GROUPS );
        // add an artifact in the tree to make sure "single" is not collapsed
        metadataResolver.setProjectVersion( TEST_REPO, "single", "single", new ProjectVersionMetadata() );

        String result = action.browse();
        assertSuccessResult( result );

        BrowsingResults results = action.getResults();
        assertNotNull( results );
        assertEquals( Arrays.asList( TEST_REPO ), results.getSelectedRepositoryIds() );
        assertEquals( Arrays.asList( "com", "commons-lang", "org.apache", "repeat.repeat", "single" ),
                      results.getGroupIds() );
        assertNull( results.getArtifacts() );
        assertNull( results.getSelectedArtifactId() );
        assertNull( results.getSelectedGroupId() );
        assertNull( results.getVersions() );

        assertNull( action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseNoObservableRepos()
    {
        setObservableRepos( Collections.<String>emptyList() );

        String result = action.browse();
        assertNoAccessResult( result );

        assertNoOutputVariables();
    }

    public void testBrowseGroupNoObservableRepos()
    {
        setObservableRepos( Collections.<String>emptyList() );
        String selectedGroupId = "org";

        action.setGroupId( selectedGroupId );
        String result = action.browseGroup();
        assertNoAccessResult( result );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getResults() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifactNoObservableRepos()
    {
        setObservableRepos( Collections.<String>emptyList() );
        String selectedGroupId = "org.apache";
        String selectedArtifactId = "apache";

        action.setGroupId( selectedGroupId );
        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertNoAccessResult( result );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getResults() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseGroupNoGroupId()
    {
        String result = action.browseGroup();
        assertErrorResult( result );
        assertNoOutputVariables();
    }

    public void testBrowseGroupNoArtifacts()
    {
        String selectedGroupId = "org";
        List<String> groups = Arrays.asList( "apache.archiva", "apache.maven" );

        archivaDao.setGroups( groups );
        archivaDao.setArtifacts( Collections.<String>emptyList() );
        action.setGroupId( selectedGroupId );
        String result = action.browseGroup();
        assertSuccessResult( result );

        BrowsingResults results = action.getResults();
        assertNotNull( results );
        assertEquals( Arrays.asList( TEST_REPO ), results.getSelectedRepositoryIds() );
        assertEquals( groups, results.getGroupIds() );
        assertEquals( Collections.<String>emptyList(), results.getArtifacts() );
        assertNull( results.getSelectedArtifactId() );
        assertEquals( selectedGroupId, results.getSelectedGroupId() );
        assertNull( results.getVersions() );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseGroupWithArtifacts()
    {
        String artifacts = "apache";
        String selectedGroupId = "org.apache";
        List<String> groups = Arrays.asList( "archiva", "maven" );

        archivaDao.setGroups( groups );
        archivaDao.setArtifacts( Collections.singletonList( artifacts ) );
        action.setGroupId( selectedGroupId );
        String result = action.browseGroup();
        assertSuccessResult( result );

        BrowsingResults results = action.getResults();
        assertNotNull( results );
        assertEquals( Arrays.asList( TEST_REPO ), results.getSelectedRepositoryIds() );
        assertEquals( groups, results.getGroupIds() );
        assertEquals( Collections.singletonList( artifacts ), results.getArtifacts() );
        assertNull( results.getSelectedArtifactId() );
        assertEquals( selectedGroupId, results.getSelectedGroupId() );
        assertNull( results.getVersions() );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifactNoGroupId()
    {
        String selectedArtifactId = "apache";

        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertErrorResult( result );

        assertNull( action.getResults() );
        assertNull( action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifactNoArtifactId()
    {
        String selectedGroupId = "org.apache";

        action.setGroupId( selectedGroupId );
        String result = action.browseArtifact();
        assertErrorResult( result );

        assertNull( action.getResults() );
        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifact()
        throws ArchivaDatabaseException
    {
        String selectedGroupId = "org.apache";
        String selectedArtifactId = "apache";

        List<String> versions = Arrays.asList( "1", "2", "3", "4" );
        archivaDao.setVersions( versions );
        MockControl artifactDaoMockControl = createArtifactDaoMock( selectedGroupId, selectedArtifactId, versions );
        MockControl projectDaoMockControl = createProjectDaoMock(
            Arrays.asList( createProjectModel( selectedGroupId, selectedArtifactId, "1" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "2" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "3" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "4" ) ) );

        action.setGroupId( selectedGroupId );
        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertSuccessResult( result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertEquals( selectedGroupId, action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getRepositoryId() );

        BrowsingResults results = action.getResults();
        assertNotNull( results );
        assertEquals( Arrays.asList( TEST_REPO ), results.getSelectedRepositoryIds() );
        assertNull( results.getGroupIds() );
        assertNull( results.getArtifacts() );
        assertEquals( selectedGroupId, results.getSelectedGroupId() );
        assertEquals( selectedArtifactId, results.getSelectedArtifactId() );
        assertEquals( versions, results.getVersions() );

        ArchivaProjectModel model = action.getSharedModel();
        assertDefaultModel( model, selectedGroupId, selectedArtifactId, null );
    }

    public void testBrowseArtifactWithSnapshots()
        throws ArchivaDatabaseException
    {
        String selectedGroupId = "org.apache";
        String selectedArtifactId = "apache";

        List<String> versions = Arrays.asList( "1", "2", "3", "4-SNAPSHOT", "4", "5-SNAPSHOT" );
        archivaDao.setVersions( versions );
        MockControl artifactDaoMockControl = createArtifactDaoMock( selectedGroupId, selectedArtifactId, versions );
        MockControl projectDaoMockControl = createProjectDaoMock(
            Arrays.asList( createProjectModel( selectedGroupId, selectedArtifactId, "1" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "2" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "3" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "4-SNAPSHOT" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "4" ),
                           createProjectModel( selectedGroupId, selectedArtifactId, "5-SNAPSHOT" ) ) );

        action.setGroupId( selectedGroupId );
        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertSuccessResult( result );

        artifactDaoMockControl.verify();
        projectDaoMockControl.verify();

        assertEquals( selectedGroupId, action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getRepositoryId() );

        BrowsingResults results = action.getResults();
        assertNotNull( results );
        assertEquals( Arrays.asList( TEST_REPO ), results.getSelectedRepositoryIds() );
        assertNull( results.getGroupIds() );
        assertNull( results.getArtifacts() );
        assertEquals( selectedGroupId, results.getSelectedGroupId() );
        assertEquals( selectedArtifactId, results.getSelectedArtifactId() );
        assertEquals( versions, results.getVersions() );

        ArchivaProjectModel model = action.getSharedModel();
        assertDefaultModel( model, selectedGroupId, selectedArtifactId, null );
    }

    // TODO: test with restricted observable repos
    //       not currently relevant since it is controlled at the DefaultRepositoryBrowsing level
    // TODO: current behaviour is to ignore values that differ between models - instead, pick the latest and use that.
    //       Need to update the tests to verify this as models are currently the same

    private void assertNoAccessResult( String result )
    {
        assertEquals( GlobalResults.ACCESS_TO_NO_REPOS, result );
        assertEquals( 0, action.getActionErrors().size() );
        assertEquals( 0, action.getActionMessages().size() );
    }

    private void assertSuccessResult( String result )
    {
        assertEquals( Action.SUCCESS, result );
        assertEquals( 0, action.getActionErrors().size() );
        assertEquals( 0, action.getActionMessages().size() );
    }

    private void assertErrorResult( String result )
    {
        assertEquals( Action.ERROR, result );
        assertEquals( 1, action.getActionErrors().size() );
        assertEquals( 0, action.getActionMessages().size() );
    }

    private void assertNoOutputVariables()
    {
        assertNull( action.getResults() );
        assertNull( action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    private MockControl createArtifactDaoMock( String groupId, String artifactId, List<String> versions )
        throws ArchivaDatabaseException
    {
        // testing deeper than normal with the mocks as we intend to replace RepositoryBrowsing, not just the database
        // underneath it - those sections will be adjusted with a mock content repository later
        MockControl control = MockControl.createNiceControl( ArtifactDAO.class );
        ArtifactDAO dao = (ArtifactDAO) control.getMock();
        archivaDao.setArtifactDao( dao );

        for ( String v : versions )
        {
            ArtifactsRelatedConstraint c = new ArtifactsRelatedConstraint( groupId, artifactId, v );
            dao.queryArtifacts( c );
            control.setReturnValue( Collections.singletonList( createArtifact( groupId, artifactId, v ) ) );
        }

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

    private MockControl createProjectDaoMock( List<ArchivaProjectModel> projects )
        throws ArchivaDatabaseException
    {
        MockControl control = MockControl.createNiceControl( ProjectModelDAO.class );
        ProjectModelDAO dao = (ProjectModelDAO) control.getMock();
        archivaDao.setProjectDao( dao );

        for ( ArchivaProjectModel project : projects )
        {
            control.expectAndReturn(
                dao.getProjectModel( project.getGroupId(), project.getArtifactId(), project.getVersion() ), project );
        }

        control.replay();
        return control;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        action = (BrowseAction) lookup( Action.class, ACTION_HINT );
        archivaDao = (ArchivaDAOStub) lookup( ArchivaDAO.class, "jdo" );
        metadataResolver = (TestMetadataResolver) action.getMetadataResolver();
    }

    protected ArchivaProjectModel createProjectModel( String groupId, String artifactId, String version )
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
}