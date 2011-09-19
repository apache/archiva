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

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.testng.annotations.Test;

/**
 * Test all actions affected with XSS security issue.
 */
@Test( groups = { "xss" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class XSSSecurityTest
    extends AbstractArchivaTest
{
    public void testDeleteArtifactImmunityToURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/deleteArtifact!doDelete.action?groupId=\"/>1<script>alert('xss')</script>&artifactId=\"/>1<script>alert('xss')</script>&version=\"/>1<script>alert('xss')</script>&repositoryId=\"/>1<script>alert('xss')</script>" );
        assertDeleteArtifactPage();
        assertTextPresent( "Invalid version." );
        assertTextPresent(
            "User is not authorized to delete artifacts in repository '\"/>1<script>alert('xss')</script>'." );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Repository id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertElementValue( "//input[@id='deleteArtifact_groupId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='deleteArtifact_artifactId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='deleteArtifact_version']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//select[@id='deleteArtifact_repositoryId']", "internal" );
    }

    public void testDeleteArtifactImmunityToEncodedURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/deleteArtifact!doDelete.action?groupId=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&artifactId=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&version=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&repositoryId=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E" );
        assertDeleteArtifactPage();
        assertTextPresent( "Invalid version." );
        assertTextPresent(
            "User is not authorized to delete artifacts in repository '\"/>1<script>alert('xss')</script>'." );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Repository id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertElementValue( "//input[@id='deleteArtifact_groupId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='deleteArtifact_artifactId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='deleteArtifact_version']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//select[@id='deleteArtifact_repositoryId']", "internal" );
    }

    public void testEditAppearanceImmunityToURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/admin/configureAppearance.action?organisationName=<script>alert('xss')</script>&organisationUrl=<script>alert('xss')</script>&organisationLogo=<script>alert('xss')</script>" );
        assertAppearancePage();
        assertXpathCount( "//td[text()=\"<script>alert('xss')</script>\"]", 1 );
        assertXpathCount( "//code[text()=\"<script>alert('xss')</script>\"]", 2 );

    }

    public void testEditAppearanceImmunityToEncodedURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/admin/configureAppearance.action?organisationName=%3Cscript%3Ealert('xss')%3C%2Fscript%3E&organisationUrl=%3Cscript%3Ealert('xss')%3C%2Fscript%3E&organisationLogo=%3Cscript%3Ealert('xss')%3C%2Fscript%3E" );
        assertAppearancePage();
        assertXpathCount( "//td[text()=\"<script>alert('xss')</script>\"]", 1 );
        assertXpathCount( "//code[text()=\"<script>alert('xss')</script>\"]", 2 );
    }

    public void testAddLegacyArtifactImmunityToURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/admin/addLegacyArtifactPath!commit.action?legacyArtifactPath.path=\"/>1<script>alert('xss')</script>&groupId=\"/>1<script>alert('xss')</script>&artifactId=\"/>1<script>alert('xss')</script>&version=\"/>1<script>alert('xss')</script>&classifier=\"/>1<script>alert('xss')</script>&type=\"/>1<script>alert('xss')</script>" );
        assertAddLegacyArtifactPathPage();
        assertTextPresent(
            "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertElementValue( "//input[@id='addLegacyArtifactPath_legacyArtifactPath_path']",
                            "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_artifactId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_version']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_groupId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_classifier']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_type']", "\"/>1<script>alert('xss')</script>" );
    }

    public void testAddLegacyArtifactImmunityToEncodedURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/admin/addLegacyArtifactPath!commit.action?legacyArtifactPath.path=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&groupId=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&artifactId=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&version=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&classifier=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E&type=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E" );
        assertAddLegacyArtifactPathPage();
        assertTextPresent(
            "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertElementValue( "//input[@id='addLegacyArtifactPath_legacyArtifactPath_path']",
                            "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_artifactId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_version']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_groupId']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_classifier']", "\"/>1<script>alert('xss')</script>" );
        assertElementValue( "//input[@id='addLegacyArtifactPath_type']", "\"/>1<script>alert('xss')</script>" );
    }

    public void testDeleteNetworkProxyImmunityToURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/admin/deleteNetworkProxy!confirm.action?proxyid=\"/>1<script>alert('xss')</script>" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testDeleteNetworkProxyImmunityToEncodedURLCrossSiteScripting()
    {
        getSelenium().open(
            "/archiva/admin/deleteNetworkProxy!confirm.action?proxyid=%22%2F%3E1%3Cscript%3Ealert('xss')%3C%2Fscript%3E" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testAddManagedRepositoryImmunityToInputFieldCrossSiteScripting()
    {
        goToRepositoriesPage();
        getSelenium().open( "/archiva/admin/addRepository.action" );
        addManagedRepository( "test\"><script>alert('xss')</script>", "test\"><script>alert('xss')</script>",
                              "test\"><script>alert('xss')</script>", "test\"><script>alert('xss')</script>",
                              "Maven 2.x Repository", "", "-1", "101" );
        // xss inputs are blocked by validation.
        assertTextPresent(
            "Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent(
            "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
        assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
        assertTextPresent( "Invalid cron expression." );
    }

    public void testEditAppearanceImmunityToInputFieldCrossSiteScripting()
    {
        goToAppearancePage();
        clickLinkWithText( "Edit" );
        addEditAppearance( "test<script>alert('xss')</script>", "test<script>alert('xss')</script>",
                           "test<script>alert('xss')</script>", false );
        // xss inputs are blocked by validation.
        assertTextPresent(
            "Organisation name must only contain alphanumeric characters, white-spaces(' '), equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent( "You must enter a URL" );
        assertXpathCount( "//span[@class='errorMessage' and text()='You must enter a URL']", 2 );
    }

    public void testEditAppearanceImmunityToCrossSiteScriptingRendering()
    {
        goToAppearancePage();
        clickLinkWithText( "Edit" );
        addEditAppearance( "xss", "http://\">test<script>alert(\"xss\")</script>",
                           "http://\">test<script>alert(\"xss\")</script>", false );
        // escaped html/url prevents cross-site scripting exploits
        assertXpathCount( "//td[text()=\"xss\"]", 1 );
        assertXpathCount( "//code[text()='http://\">test<script>alert(\"xss\")</script>']", 2 );
    }

    public void testAddLegacyArtifactPathImmunityToInputFieldCrossSiteScripting()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "test<script>alert('xss')</script>", "test<script>alert('xss')</script>",
                               "test<script>alert('xss')</script>", "test<script>alert('xss')</script>",
                               "test<script>alert('xss')</script>", "test<script>alert('xss')</script>" );
        // xss inputs are blocked by validation.
        assertTextPresent(
            "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
    }

    public void testAddNetworkProxyImmunityToInputFieldCrossSiteScripting()
    {
        goToNetworkProxiesPage();
        addNetworkProxy( "test<script>alert('xss')</script>", "test<script>alert('xss')</script>",
                         "test<script>alert('xss')</script>", "test<script>alert('xss')</script>",
                         "test<script>alert('xss')</script>", "" );
        // xss inputs are blocked by validation.
        assertTextPresent(
            "Proxy id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent(
            "Protocol must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), dots(.), colons(:), and dashes(-)." );
        assertTextPresent(
            "Host must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        assertTextPresent( "Invalid field value for field \"proxy.port\"." );
        assertTextPresent(
            "Username must only contain alphanumeric characters, at's(@), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
    }
}