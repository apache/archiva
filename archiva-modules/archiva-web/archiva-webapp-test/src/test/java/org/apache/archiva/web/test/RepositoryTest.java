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

import junit.framework.Assert;
import org.apache.archiva.web.test.parent.AbstractRepositoryTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

@Test( groups = { "repository" }, sequential = true )
public class RepositoryTest
	extends AbstractRepositoryTest
{
    @BeforeTest
    public void setUp()
    {
        loginAsAdmin();
    }

    @AfterSuite
    public void tearDown()
    {
        goToRepositoriesPage();
        deleteManagedRepository( "managedrepo1", true, false );
        deleteManagedRepository( "managedrepo2", true, false );
        deleteManagedRepository( "managedrepoedit", true, false );
        deleteRemoteRepository( "remoterepo", false );
    }

    public void testAddManagedRepoValidValues()
        throws IOException
    {
        File dir = new File( getRepositoryDir() + "repository/" );
        if ( dir.exists() )
        {
            FileUtils.deleteDirectory( dir );
        }
        addManagedRepository( "managedrepo1", "Managed Repository Sample 1", dir.getAbsolutePath(), "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );
        assertTextPresent( "Managed Repository Sample 1" );
		assertRepositoriesPage();

        Assert.assertTrue( dir.exists() && dir.isDirectory() );
    }

    public void testAddManagedRepoDirectoryExists()
        throws IOException
    {
        File dir = new File( getRepositoryDir() + "repository-exists/" );
        dir.mkdirs();
        Assert.assertTrue( dir.exists() && dir.isDirectory() );

        addManagedRepository( "managedrepo2", "Managed Repository Sample 2", dir.getAbsolutePath(), "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );
        assertTextPresent( "Managed Repository Sample 2" );

        assertTextPresent( "WARNING: Repository location already exists." );

        clickButtonWithValue( "Save" );

		assertRepositoriesPage();
        assertTextPresent( "Managed Repository Sample 2" );
    }

    public void testAddManagedRepoDirectoryExistsCancel()
        throws IOException
    {
        File dir = new File( getRepositoryDir() + "repository-exists/" );
        dir.mkdirs();
        Assert.assertTrue( dir.exists() && dir.isDirectory() );

        addManagedRepository( "managedrepo3", "Managed Repository Sample 3", dir.getAbsolutePath(), "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );
        assertTextPresent( "Managed Repository Sample 3" );
        assertTextPresent( "WARNING: Repository location already exists." );

        clickButtonWithValue( "Cancel" );

        assertRepositoriesPage();
        assertTextNotPresent( "Managed Repository Sample 3" );
    }

        public void testAddManagedRepoInvalidValues()
        {
		addManagedRepository( "<> \\/~+[ ]'\"", "<>\\~+[]'\"" , "<> ~+[ ]'\"" , "<> ~+[ ]'\"", "Maven 2.x Repository", "", "-1", "101" );
		assertTextPresent( "Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
		assertTextPresent( "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
		assertTextPresent( "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
                assertTextPresent( "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
                assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100.");
                assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0.");
		assertTextPresent( "Invalid cron expression." );
        }

	public void testAddManagedRepoInvalidIdentifier()
	{
		addManagedRepository( "<> \\/~+[ ]'\"", "name" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
		assertTextPresent( "Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddManagedRepoInvalidRepoName()
	{
		addManagedRepository( "identifier", "<>\\~+[]'\"" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
		assertTextPresent( "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
	}

	public void testAddManagedRepoInvalidDirectory()
	{
		addManagedRepository( "identifier", "name" , "<> ~+[ ]'\"" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
		assertTextPresent( "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
	}

	public void testAddManagedRepoInvalidIndexDir()
	{
		addManagedRepository( "identifier", "name" , "/home" , "<> ~+[ ]'\"", "Maven 2.x Repository", "0 0 * * * ?", "1", "1" );
		assertTextPresent( "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
	}

	public void testAddManagedRepoInvalidRetentionCount()
	{
		addManagedRepository( "identifier", "name" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "101" );
		assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
	}

	public void testAddManagedRepoInvalidDaysOlder()
	{
		addManagedRepository( "identifier", "name" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "-1", "1" );
		assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
	}

	public void testAddManagedRepoBlankValues()
	{
		addManagedRepository( "", "" , "" , "", "Maven 2.x Repository", "", "", "" );
		assertTextPresent( "You must enter a repository identifier." );
		assertTextPresent( "You must enter a repository name." );
		assertTextPresent( "You must enter a directory." );
		assertTextPresent( "Invalid cron expression." );
	}

	public void testAddManagedRepoNoIdentifier()
	{
		addManagedRepository( "", "name", "/home", "/.index", "Maven 2.x Repository", "0 0 * * * ?", "", "" );
		assertTextPresent( "You must enter a repository identifier." );
	}

	public void testAddManagedRepoNoRepoName()
	{
		addManagedRepository( "identifier", "" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "", "" );
		assertTextPresent( "You must enter a repository name." );
	}

	public void testAddManagedRepoNoDirectory()
	{
		addManagedRepository( "identifier", "name" , "" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "", "" );
		assertTextPresent( "You must enter a directory." );
	}

	public void testAddManagedRepoNoCron()
	{
		addManagedRepository( "identifier", "name" , "/home" , "/.index", "Maven 2.x Repository", "", "", "" );
		assertTextPresent( "Invalid cron expression." );
	}

    public void testEditManagedRepo()
        throws IOException
    {
        String directory = getRepositoryDir() + "local-repo/";
        File dir = new File( directory );
        if ( dir.exists() )
        {
            FileUtils.deleteDirectory( dir );
        }
        addManagedRepository( "managedrepoedit", "Managed Repository for Editing", directory, "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );

        editManagedRepository( "repository.name" , "New Managed Repo Name" );
        assertRepositoriesPage();
        assertTextNotPresent( "Managed Repository for Editing" );
        assertTextPresent( "New Managed Repo Name" );
    }

    @Test(dependsOnMethods = "testEditManagedRepo")
    public void testEditManagedRepoDirectoryChangedToNonExistant()
        throws IOException
    {
        goToRepositoriesPage();
        String directory = getRepositoryDir() + "new-repo-dir/";
        File dir = new File( directory );
        if ( dir.exists() )
        {
            FileUtils.deleteDirectory( dir );
        }

        editManagedRepository( "repository.location", dir.getAbsolutePath() );
        assertRepositoriesPage();
        assertTextPresent( "new-repo-dir" );
        Assert.assertTrue( dir.exists() );
    }

    @Test(dependsOnMethods = "testEditManagedRepo")
    public void testEditManagedRepoDirectoryChangedToExisting()
        throws IOException
    {
        goToRepositoriesPage();
        String directory = getRepositoryDir() + "new-repo-dir/";
        File dir = new File( directory );
        dir.mkdirs();
        Assert.assertTrue( dir.exists() && dir.isDirectory() );

        editManagedRepository( "repository.location", dir.getAbsolutePath() );

        assertTextPresent( "WARNING: Repository location already exists." );
        clickButtonWithValue( "Save" );

        assertRepositoriesPage();
        assertTextPresent( "new-repo-dir" );
        Assert.assertTrue( dir.exists() );
    }

    @Test(dependsOnMethods = "testEditManagedRepo")
    public void testEditManagedRepoDirectoryChangedToExistingCancel()
        throws IOException
    {
        goToRepositoriesPage();
        String directory = getRepositoryDir() + "existing-dir/";
        File dir = new File( directory );
        dir.mkdirs();
        Assert.assertTrue( dir.exists() && dir.isDirectory() );

        editManagedRepository( "repository.location", dir.getAbsolutePath() );

        assertTextPresent( "WARNING: Repository location already exists." );
        clickButtonWithValue( "Cancel" );

        assertRepositoriesPage();
        assertTextNotPresent( "existing-dir" );
        Assert.assertTrue( dir.exists() );
    }

    public void testEditManagedRepoInvalidValues()
	{
		editManagedRepository("<>\\~+[]'\"" , "<> ~+[ ]'\"" , "<> ~+[ ]'\"", "Maven 2.x Repository", "", "-1", "101");
                assertTextPresent( "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
		assertTextPresent( "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
                assertTextPresent( "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
                assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100.");
                assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0.");
		assertTextPresent( "Invalid cron expression." );
	}

        public void testEditManagedRepoInvalidRepoName()
	{
                editManagedRepository("<>\\~+[]'\"" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1");
                assertTextPresent( "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'), underscores(_), dots(.), and dashes(-)." );
	}

        public void testEditManagedRepoInvalidDirectory()
	{
                editManagedRepository("name" , "<> ~+[ ]'\"" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "1");
                assertTextPresent( "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
	}

        public void testEditManagedRepoInvalidIndexDir()
	{
                editManagedRepository("name" , "/home" , "<> ~+[ ]'\"", "Maven 2.x Repository", "0 0 * * * ?", "1", "1");
                assertTextPresent( "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
	}

        public void testEditManagedRepoInvalidCron()
	{
                editManagedRepository("name" , "/home" , "/.index", "Maven 2.x Repository", "", "1", "1");
                assertTextPresent( "Invalid cron expression." );
	}

        public void testEditManagedRepoInvalidRetentionCount()
	{
                editManagedRepository("name" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "1", "101");
                assertTextPresent( "Repository Purge By Retention Count needs to be between 1 and 100." );
	}

        public void testEditManagedRepoInvalidDaysOlder()
	{
                editManagedRepository("name" , "/home" , "/.index", "Maven 2.x Repository", "0 0 * * * ?", "-1", "1");
                assertTextPresent( "Repository Purge By Days Older Than needs to be larger than 0." );
	}

	public void testDeleteManagedRepo()
        throws IOException
    {
        File dir = new File( getRepositoryDir() + "managedrepodelete/" );
        if ( dir.exists() )
        {
            FileUtils.deleteDirectory( dir );
        }
        addManagedRepository( "managedrepodelete", "Managed Repository for Deleting", dir.getAbsolutePath(), "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );

        deleteManagedRepository( "managedrepodelete", false, true );

        // assert removed, but contents remain
        assertRepositoriesPage();
        assertTextNotPresent( "managedrepodelete" );

        Assert.assertTrue( dir.exists() && dir.isDirectory() );
    }

	public void testDeleteManagedRepoWithContents()
        throws IOException
    {
        File dir = new File( getRepositoryDir() + "managedrepodeletecontents/" );
        if ( dir.exists() )
        {
            FileUtils.deleteDirectory( dir );
        }
        addManagedRepository( "managedrepodeletecontents", "Managed Repository for Deleting", dir.getAbsolutePath(), "",
                              "Maven 2.x Repository", "0 0 * * * ?", "", "" );

        deleteManagedRepository( "managedrepodeletecontents", true, true );

        // assert removed, but contents remain
        assertRepositoriesPage();
        assertTextNotPresent( "managedrepodeletecontents" );

        Assert.assertFalse( dir.exists() );
    }

	public void testAddRemoteRepoNullValues()
	{
		addRemoteRepository( "" , "" , "" , "" , "" , "" , "Maven 2.x Repository" );
		assertTextPresent( "You must enter a repository identifier." );
		assertTextPresent( "You must enter a repository name." );
		assertTextPresent( "You must enter a url." );
	}

	public void testAddRemoteRepositoryNullIdentifier()
	{
		addRemoteRepository( "" , "Remote Repository Sample" , "http://repository.codehaus.org/org/codehaus/mojo/" , "" , "" , "" , "Maven 2.x Repository" );
		assertTextPresent( "You must enter a repository identifier." );
	}

	public void testAddRemoteRepoNullName()
	{
		addRemoteRepository( "remotenullrepo" , "" , "http://repository.codehaus.org/org/codehaus/mojo/" , "" , "" , "" , "Maven 2.x Repository" );
		assertTextPresent( "You must enter a repository name." );
	}

	public void testAddRemoteRepoNullURL()
	{
		addRemoteRepository( "remotenullrepo" , "Remote Repository Sample" , "" , "" , "" , "" , "Maven 2.x Repository" );
		assertTextPresent( "You must enter a url." );
	}

	public void testAddRemoteRepoValidValues()
	{
		getSelenium().open( "/archiva/admin/addRemoteRepository.action" );
		addRemoteRepository( "remoterepo" , "Remote Repository Sample" , "http://repository.codehaus.org/org/codehaus/mojo/" , "" , "" , "" , "Maven 2.x Repository" );
		assertTextPresent( "Remote Repository Sample" );
	}

    // *** BUNDLED REPOSITORY TEST ***

    @Test ( dependsOnMethods = { "testWithCorrectUsernamePassword" }, alwaysRun = true )
    public void testBundledRepository()
    {
        String repo1 = baseUrl + "repository/internal/";
        String repo2 = baseUrl + "repository/snapshots/";

        assertRepositoryAccess( repo1 );
        assertRepositoryAccess( repo2 );

        getSelenium().open( "/archiva" );
    }

    private void assertRepositoryAccess( String repo )
    {
        getSelenium().open( "/archiva" );
        goToRepositoriesPage();
        assertLinkPresent( repo );
        clickLinkWithText( repo );
        assertPage( "Collection: /" );
        assertTextPresent( "Collection: /" );
    }
}
