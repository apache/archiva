package org.apache.archiva.web.test;

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

import org.apache.archiva.web.test.parent.AbstractMergingRepositoriesTest;
import org.testng.annotations.Test;

@Test(groups = {"merging"}, dependsOnMethods = {"testWithCorrectUsernamePassword"}, sequential = true)
public class MergingRepositoriesTest
    extends AbstractMergingRepositoriesTest
{

    public void testAddStagingRepository()
    {
        goToRepositoriesPage();
        getSelenium().open( "/archiva/admin/addRepository.action" );
        addStagingRepository( "merging-repo", "merging-repo", getRepositoryDir() + "merging-repo/", "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );
        assertTextPresent( "merging-repo" );
    }

    // here we upload an artifact to the staging repository
    @Test(dependsOnMethods = {"testAddStagingRepository"})
    public void testAddArtifactToStagingRepository()
    {
        addArtifact( getGroupId(), getArtifactId(), getVersion(), getPackaging(), getValidArtifactFilePath(),
                     "merging-repo-stage", true );
        assertTextPresent( "Artifact '" + getGroupId() + ":" + getArtifactId() + ":" + getVersion() +
            "' was successfully deployed to repository 'merging-repo-stage'" );
    }

    // here we test the merging (no conflicts artifacts are available)
    @Test(dependsOnMethods = {"testAddArtifactToStagingRepository"})
    public void testMerging()
    {
        goToRepositoriesPage();
        clickButtonWithValue( "Merge" );
        assertTextPresent( "No conflicting artifacts" );
        clickButtonWithValue( "Merge All" );
        assertTextPresent( "Repository 'merging-repo-stage' successfully merged to 'merging-repo'." );
    }

    // check audit updating is done properly or not
    @Test(dependsOnMethods = {"testMerging"})
    public void testAuditLogs()
    {
        goToAuditLogReports();
        assertTextPresent( "Merged Artifact" );
        assertTextPresent( "merging-repo" );
    }

    // merging is done by skipping conflicts
    @Test(dependsOnMethods = {"testMerging"})
    public void testMergingWithSkippingConflicts()
    {
        goToRepositoriesPage();
        clickButtonWithValue( "Merge" );
        assertTextPresent( "WARNING: The following are the artifacts in conflict." );
        clickButtonWithValue( "Merge With Skip" );
        assertTextPresent( "Repository 'merging-repo-stage' successfully merged to 'merging-repo'." );
    }

    // merging all 
    @Test(dependsOnMethods = {"testMerging"})
    public void testMergingWithOutSkippingConflicts()
    {
        goToRepositoriesPage();
        clickButtonWithValue( "Merge" );
        assertTextPresent( "WARNING: The following are the artifacts in conflict." );
        clickButtonWithValue( "Merge All" );
        assertTextPresent( "Repository 'merging-repo-stage' successfully merged to 'merging-repo'." );
    }

    // change the configuaration first and try to upload existing artifact to the repository
    public void testConfigurationChangesOfStagingRepository()
    {
        editManagedRepository();
        addArtifact( getGroupId(), getArtifactId(), getVersion(), getPackaging(), getValidArtifactFilePath(),
                     "merging-repo-stage", true );
        assertTextPresent(
            "Overwriting released artifacts in repository '" + "merging-repo-stage" + "' is not allowed." );
    }

}