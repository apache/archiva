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

@Test( groups = { "legacysupport" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class LegacySupportTest
    extends AbstractArtifactManagementTest
{
    public void testAddLegacyArtifact_NullValues()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "", "", "", "", "", "", false );
        assertTextPresent( "You must enter a legacy path." );
        assertTextPresent( "You must enter a groupId." );
        assertTextPresent( "You must enter an artifactId." );
        assertTextPresent( "You must enter a version." );
        assertTextPresent( "You must enter a type." );
    }

    public void testAddLegacyArtifact_NullLegacyPath()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "", "test", "test", "1.0-SNAPSHOT", "testing", "jar", false );
        assertTextPresent( "You must enter a legacy path." );
    }

    public void testAddLegacyArtifact_NullGroupId()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "test", "", "test", "1.0-SNAPSHOT", "testing", "jar", false );
        assertTextPresent( "You must enter a groupId." );
    }

    public void testAddLegacyArtifact_NullArtifactId()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "test", "test", "", "1.0-SNAPSHOT", "testing", "jar", false );
        assertTextPresent( "You must enter an artifactId." );
    }

    public void testAddLegacyArtifact_NullVersion()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "test", "test", "test", "", "testing", "jar", false );
        assertTextPresent( "You must enter a version." );
    }

    public void testAddLegacyArtifact_NullType()
    {
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
        addLegacyArtifactPath( "test", "test", "test", "1.0-SNAPSHOT", "testing", "", false );
        assertTextPresent( "You must enter a type." );
    }

	public void testAddLegacyArtifact_InvalidValues()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "<> ~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"",
                               false );
		assertTextPresent( "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddLegacyArtifact_InvalidLegacyPath()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "<> ~+[ ]'\"" , "test" , "test" , "1.0-SNAPSHOT" , "testing" , "jar", false );
		assertTextPresent( "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddLegacyArtifact_InvalidGroupId()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "test" , "<> \\/~+[ ]'\"" , "test" , "1.0-SNAPSHOT" , "testing" , "jar", false );
		assertTextPresent( "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddLegacyArtifact_InvalidArtifactId()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "test" , "test" , "<> \\/~+[ ]'\"" , "1.0-SNAPSHOT" , "testing" , "jar", false );
		assertTextPresent( "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddLegacyArtifact_InvalidVersion()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "test" , "test" , "test" , "<> \\/~+[ ]'\"" , "testing" , "jar", false );
		assertTextPresent( "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddLegacyArtifact_InvalidType()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "test" , "test" , "test" , "1.0-SNAPSHOT" , "testing" , "<> \\/~+[ ]'\"", false );
		assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddLegacyArtifact_InvalidClassifier()
	{
        goToLegacySupportPage();
        clickLinkWithText( "Add" );
		addLegacyArtifactPath( "test" , "test" , "test" , "1.0-SNAPSHOT" , "<> \\/~+[ ]'\"" , "jar", false );
		assertTextPresent( "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}
}