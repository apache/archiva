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
import org.apache.archiva.metadata.repository.MetadataResolverException;
import org.apache.archiva.metadata.repository.memory.TestMetadataResolver;

public class BrowseActionTest
    extends AbstractActionTestCase
{
    private static final String ACTION_HINT = "browseAction";

    private BrowseAction action;

    private static final List<String> GROUPS =
        Arrays.asList( "org.apache.archiva", "commons-lang", "org.apache.maven", "com.sun", "com.oracle",
                       "repeat.repeat" );

    public void testInstantiation()
    {
        assertFalse( action == lookup( Action.class, ACTION_HINT ) );
    }

    public void testBrowse()
    {
        metadataResolver.setNamespaces( GROUPS );

        String result = action.browse();
        assertSuccessResult( result );

        assertEquals( Arrays.asList( "com", "commons-lang", "org.apache", "repeat.repeat" ), action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );

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
        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifactNoObservableRepos()
        throws MetadataResolverException
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
        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );
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
        List<String> groups = Arrays.asList( "org.apache.archiva", "org.apache.maven" );

        metadataResolver.setNamespaces( groups );
        action.setGroupId( selectedGroupId );
        String result = action.browseGroup();
        assertSuccessResult( result );

        assertEquals( Collections.singletonList( "org.apache" ), action.getNamespaces() );
        assertEquals( Collections.<String>emptyList(), action.getProjectIds() );
        assertNull( action.getProjectVersions() );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseGroupWithArtifacts()
    {
        String artifacts = "apache";
        String selectedGroupId = "org.apache";
        List<String> groups = Arrays.asList( "org.apache.archiva", "org.apache.maven" );

        metadataResolver.setNamespaces( groups );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, artifacts, new ProjectVersionMetadata() );
        action.setGroupId( selectedGroupId );
        String result = action.browseGroup();
        assertSuccessResult( result );

        assertEquals( groups, action.getNamespaces() );
        assertEquals( Collections.singletonList( artifacts ), action.getProjectIds() );
        assertNull( action.getProjectVersions() );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseWithCollapsedGroupsAndArtifacts()
    {
        List<String> groups = Arrays.asList( "org.apache.archiva", "org.apache" );

        metadataResolver.setNamespaces( groups );
        // add an artifact in the tree to make sure "single" is not collapsed
        metadataResolver.setProjectVersion( TEST_REPO, "org.apache", "apache", new ProjectVersionMetadata() );

        String result = action.browse();
        assertSuccessResult( result );

        assertEquals( Collections.singletonList( "org.apache" ), action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );

        assertNull( action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseGroupWithCollapsedGroupsAndArtifacts()
    {
        String artifacts = "apache";
        String selectedGroupId = "org.apache";
        List<String> groups = Arrays.asList( "org.apache.archiva", "org.apache" );

        metadataResolver.setNamespaces( groups );
        // add an artifact in the tree to make sure "single" is not collapsed
        metadataResolver.setProjectVersion( TEST_REPO, "org.apache", "apache", new ProjectVersionMetadata() );

        action.setGroupId( selectedGroupId );
        String result = action.browseGroup();
        assertSuccessResult( result );

        assertEquals( Collections.singletonList( "org.apache.archiva" ), action.getNamespaces() );
        assertEquals( Collections.singletonList( artifacts ), action.getProjectIds() );
        assertNull( action.getProjectVersions() );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifactNoGroupId()
        throws MetadataResolverException
    {
        String selectedArtifactId = "apache";

        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertErrorResult( result );

        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );
        assertNull( action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifactNoArtifactId()
        throws MetadataResolverException
    {
        String selectedGroupId = "org.apache";

        action.setGroupId( selectedGroupId );
        String result = action.browseArtifact();
        assertErrorResult( result );

        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );
        assertEquals( selectedGroupId, action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    public void testBrowseArtifact()
        throws MetadataResolverException
    {
        String selectedGroupId = "org.apache";
        String selectedArtifactId = "apache";

        List<String> versions = Arrays.asList( "1", "2", "3", "4" );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "1" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "2" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "3" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "4" ) );

        action.setGroupId( selectedGroupId );
        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertSuccessResult( result );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getRepositoryId() );

        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertEquals( versions, action.getProjectVersions() );

        ProjectVersionMetadata model = action.getSharedModel();
        assertDefaultModel( model, selectedGroupId, selectedArtifactId, null );
    }

    public void testBrowseArtifactWithSnapshots()
        throws MetadataResolverException
    {
        String selectedGroupId = "org.apache";
        String selectedArtifactId = "apache";

        List<String> versions = Arrays.asList( "1", "2", "3", "4-SNAPSHOT", "4", "5-SNAPSHOT" );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "1" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "2" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "3" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "4-SNAPSHOT" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "4" ) );
        metadataResolver.setProjectVersion( TEST_REPO, selectedGroupId, selectedArtifactId,
                                            createProjectModel( selectedGroupId, selectedArtifactId, "5-SNAPSHOT" ) );

        action.setGroupId( selectedGroupId );
        action.setArtifactId( selectedArtifactId );
        String result = action.browseArtifact();
        assertSuccessResult( result );

        assertEquals( selectedGroupId, action.getGroupId() );
        assertEquals( selectedArtifactId, action.getArtifactId() );
        assertNull( action.getRepositoryId() );

        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertEquals( versions, action.getProjectVersions() );

        ProjectVersionMetadata model = action.getSharedModel();
        assertDefaultModel( model, selectedGroupId, selectedArtifactId, null );
    }

    // TODO: test with restricted observable repos
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
        assertNull( action.getNamespaces() );
        assertNull( action.getProjectIds() );
        assertNull( action.getProjectVersions() );
        assertNull( action.getGroupId() );
        assertNull( action.getArtifactId() );
        assertNull( action.getRepositoryId() );
        assertNull( action.getSharedModel() );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        action = (BrowseAction) lookup( Action.class, ACTION_HINT );
        metadataResolver = (TestMetadataResolver) action.getMetadataResolver();
    }
}