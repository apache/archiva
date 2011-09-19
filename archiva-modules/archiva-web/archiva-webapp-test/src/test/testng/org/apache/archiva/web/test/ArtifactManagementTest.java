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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.web.test.parent.AbstractArtifactManagementTest;
import org.testng.annotations.Test;

@Test( groups = { "artifactmanagement" }, dependsOnGroups = { "about" } )
public class ArtifactManagementTest
    extends AbstractArtifactManagementTest
{

    @Test( alwaysRun = true, dependsOnGroups = "about")
    public void testAddArtifactNullValues()
    {
        goToAddArtifactPage();
        clickButtonWithValue( "Submit", false );
        //assertTextPresent( "Please add a file to upload." );
        assertTextPresent( "You must enter a version." );
        assertTextPresent( "You must enter a groupId." );
        assertTextPresent( "You must enter an artifactId." );
        assertTextPresent( "You must enter a version" );
        assertTextPresent( "You must enter a packaging" );
    }

    @Test( dependsOnMethods = { "testAddArtifactNullValues" }, alwaysRun = true )
    public void testAddArtifactNoGroupId()
    {
        addArtifact( " ", getArtifactId(), getVersion(), getPackaging(), getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "You must enter a groupId." );
    }

    @Test( dependsOnMethods = { "testAddArtifactNoGroupId" }, alwaysRun = true )
    public void testAddArtifactNoArtifactId()
    {

        addArtifact( getGroupId(), " ", getVersion(), getPackaging(), getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "You must enter an artifactId." );
    }

    @Test( dependsOnMethods = { "testAddArtifactNoGroupId" }, alwaysRun = true )
    public void testAddArtifactNoVersion()
    {
        addArtifact( getGroupId(), getArtifactId(), " ", getPackaging(), getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "You must enter a version." );
    }

    @Test( dependsOnMethods = { "testAddArtifactNoGroupId" }, alwaysRun = true )
    public void testAddArtifactInvalidVersion()
    {
        addArtifact( getGroupId(), getArtifactId(), "asdf", getPackaging(), getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "Invalid version." );
    }

    @Test( dependsOnMethods = { "testAddArtifactNoGroupId" }, alwaysRun = true )
    public void testAddArtifactNoPackaging()
    {
        addArtifact( getGroupId(), getArtifactId(), getVersion(), " ", getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "You must enter a packaging." );
    }

    @Test( dependsOnMethods = { "testAddArtifactNoGroupId" }, alwaysRun = true )
    public void testAddArtifactNoFilePath()
    {
        addArtifact( getGroupId(), getArtifactId(), getVersion(), getPackaging(), " ", getRepositoryId() );
        assertTextPresent( "Please add a file to upload." );
    }

    @Test( groups = "requiresUpload" )
    public void testAddArtifactValidValues()
    {
        String groupId = getProperty( "VALIDARTIFACT_GROUPID" );
        String artifactId = getProperty( "VALIDARTIFACT_ARTIFACTID" );

        addArtifact( groupId, artifactId, getVersion(), getPackaging(), getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "Artifact '" + groupId + ":" + artifactId + ":" + getVersion()
                               + "' was successfully deployed to repository 'internal'" );
    }

    @Test( groups = "requiresUpload" )
    public void testDotNetTypes()
    {
        String groupId = getProperty( "GROUPID_DOTNETARTIFACT" );
        String artifactId = getProperty( "ARTIFACTID_DOTNETARTIFACT" );
        String packaging = getProperty( "PACKAGING_DOTNETARTIFACT" );

        addArtifact( groupId, artifactId, getVersion(), packaging, getArtifactFilePath(), getRepositoryId() );
        assertTextPresent( "Artifact '" + groupId + ":" + artifactId + ":" + getVersion()
                               + "' was successfully deployed to repository 'internal'" );
        getSelenium().open( baseUrl + "/browse/" + groupId + "/" + artifactId + "/" + getVersion() );
        waitPage();

        assertTextPresent( "<type>library</type>" );
        String basePath =
            "/archiva/repository/internal/" + groupId + "/" + artifactId + "/" + getVersion() + "/" + artifactId + "-"
                + getVersion();
        assertLinkPresent( ".NET Library" );
        assertElementPresent( "//a[@href='" + basePath + ".dll']" );
        assertElementPresent( "//a[@href='" + basePath + ".pom']" );
    }

    // MRM-747
    @Test( groups = "requiresUpload" )
    public void testAddArtifactBlockRedeployments()
    {
        addArtifact( getGroupId(), getArtifactId(), getVersion(), getPackaging(), getArtifactFilePath(),
                     getRepositoryId() );
        assertTextPresent( "Overwriting released artifacts in repository '" + getRepositoryId() + "' is not allowed." );
    }

    @Test( groups = "requiresUpload" )
    public void testDeleteArtifact()
    {
        // prep
        String groupId = getProperty( "GROUPID1" );
        String artifactId = getProperty( "ARTIFACTID1" );
        String version = getProperty( "VERSION1" );
        String packaging = getProperty( "PACKAGING1" );
        String repositoryId = getProperty( "REPOSITORYID1" );
        // TODO: do this differently as it only works in Firefox's chrome mode
        addArtifact( groupId, artifactId, version, packaging, getArtifactFilePath(), repositoryId );
        assertTextPresent( "Artifact 'delete:delete:1.0' was successfully deployed to repository 'internal'" );

        deleteArtifact( "delete", "delete", "1.0", "internal" );
        assertTextPresent( "Artifact 'delete:delete:1.0' was successfully deleted from repository 'internal'" );
    }

    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactNoGroupId()
    {
        deleteArtifact( " ", "delete", "1.0", "internal" );
        assertTextPresent( "You must enter a groupId." );
    }

    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactNoArtifactId()
    {
        deleteArtifact( "delete", " ", "1.0", "internal" );
        assertTextPresent( "You must enter an artifactId." );
    }

    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactNoVersion()
    {
        deleteArtifact( "delete", "delete", " ", "internal" );
        assertTextPresent( "Invalid version." );
        assertTextPresent( "You must enter a version." );
    }

    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactInvalidVersion()
    {
        deleteArtifact( "delete", "delete", "asdf", "internal" );
        assertTextPresent( "Invalid version." );
    }

    // HTML select should have the proper value, else it will cause a selenium error: Option with label 'customValue' not found
    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactInvalidValues()
    {
        deleteArtifact( "<> \\/~+[ ]'\"", "<> \\/~+[ ]'\"", "<>", "internal" );
        assertTextPresent( "Invalid version." );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
    }

    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactInvalidGroupId()
    {
        deleteArtifact( "<> \\/~+[ ]'\"", "delete", "1.0", "internal" );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
    }

    @Test( alwaysRun = true, dependsOnMethods = { "testAddArtifactNullValues" } )
    public void testDeleteArtifactInvalidArtifactId()
    {
        deleteArtifact( "delete", "<> \\/~+[ ]'\"", "1.0", "internal" );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
    }
}