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
        addLegacyArtifactPath( "", "", "", "", "", "" );
        assertTextPresent( "You must enter a legacy path." );
        assertTextPresent( "You must enter a groupId." );
        assertTextPresent( "You must enter an artifactId." );
        assertTextPresent( "You must enter a version." );
        assertTextPresent( "You must enter a type." );
    }

    @Test( dependsOnMethods = { "testAddLegacyArtifact_NullValues" } )
    public void testAddLegacyArtifact_NullLegacyPath()
    {
        addLegacyArtifactPath( "", "test", "test", "1.0-SNAPSHOT", "testing", "jar" );
        assertTextPresent( "You must enter a legacy path." );
    }

    @Test( dependsOnMethods = { "testAddLegacyArtifact_NullLegacyPath" } )
    public void testAddLegacyArtifact_NullGroupId()
    {
        addLegacyArtifactPath( "test", "", "test", "1.0-SNAPSHOT", "testing", "jar" );
        assertTextPresent( "You must enter a groupId." );
    }

    @Test( dependsOnMethods = { "testAddLegacyArtifact_NullGroupId" } )
    public void testAddLegacyArtifact_NullArtifactId()
    {
        addLegacyArtifactPath( "test", "test", "", "1.0-SNAPSHOT", "testing", "jar" );
        assertTextPresent( "You must enter an artifactId." );
    }

    @Test( dependsOnMethods = { "testAddLegacyArtifact_NullArtifactId" } )
    public void testAddLegacyArtifact_NullVersion()
    {
        addLegacyArtifactPath( "test", "test", "test", "", "testing", "jar" );
        assertTextPresent( "You must enter a version." );
    }

    @Test( dependsOnMethods = { "testAddLegacyArtifact_NullVersion" } )
    public void testAddLegacyArtifact_NullType()
    {
        addLegacyArtifactPath( "test", "test", "test", "1.0-SNAPSHOT", "testing", "" );
        assertTextPresent( "You must enter a type." );
    }

    @Test( dependsOnMethods = { "testAddLegacyArtifact_NullType" })
	public void testAddLegacyArtifact_InvalidValues()
	{
		addLegacyArtifactPath( "<> ~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"" , "<> \\/~+[ ]'\"");
		assertTextPresent( "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

    @Test( dependsOnMethods = { "testAddLegacyArtifact_InvalidValues" })
	public void testAddLegacyArtifact_InvalidLegacyPath()
	{
		addLegacyArtifactPath( "<> ~+[ ]'\"" , "test" , "test" , "1.0-SNAPSHOT" , "testing" , "jar");
		assertTextPresent( "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
	}

    @Test( dependsOnMethods = { "testAddLegacyArtifact_InvalidLegacyPath" })
	public void testAddLegacyArtifact_InvalidGroupId()
	{
		addLegacyArtifactPath( "test" , "<> \\/~+[ ]'\"" , "test" , "1.0-SNAPSHOT" , "testing" , "jar");
		assertTextPresent( "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

    @Test( dependsOnMethods = { "testAddLegacyArtifact_InvalidGroupId" })
	public void testAddLegacyArtifact_InvalidArtifactId()
	{
		addLegacyArtifactPath( "test" , "test" , "<> \\/~+[ ]'\"" , "1.0-SNAPSHOT" , "testing" , "jar");
		assertTextPresent( "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

    @Test( dependsOnMethods = { "testAddLegacyArtifact_InvalidArtifactId" })
	public void testAddLegacyArtifact_InvalidVersion()
	{
		addLegacyArtifactPath( "test" , "test" , "test" , "<> \\/~+[ ]'\"" , "testing" , "jar");
		assertTextPresent( "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

    @Test( dependsOnMethods = { "testAddLegacyArtifact_InvalidVersion" })
	public void testAddLegacyArtifact_InvalidType()
	{
		addLegacyArtifactPath( "test" , "test" , "test" , "1.0-SNAPSHOT" , "testing" , "<> \\/~+[ ]'\"");
		assertTextPresent( "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

    @Test( dependsOnMethods = { "testAddLegacyArtifact_InvalidType" })
	public void testAddLegacyArtifact_InvalidClassifier()
	{
		addLegacyArtifactPath( "test" , "test" , "test" , "1.0-SNAPSHOT" , "<> \\/~+[ ]'\"" , "jar");
		assertTextPresent( "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}
}