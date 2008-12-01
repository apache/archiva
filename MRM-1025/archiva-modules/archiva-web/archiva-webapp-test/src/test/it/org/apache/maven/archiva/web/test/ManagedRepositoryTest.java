package org.apache.maven.archiva.web.test;

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

/**
 * Archiva's webapp UI test for adding/editing/deleting managed repositories.
 * 
 */
public class ManagedRepositoryTest
    extends AbstractArchivaTestCase
{
    private static final String TEST_REPOSITORY_ID = "test-repository-id";
    
    private static final String TEST_REPOSITORY_URL = "test-repository-url";
    
    private static final String TEST_REPOSITORY_NAME = "test-repository-name";
    
    private static final String TEST_REPOSITORY_DIRECTORY = "test-repository-directory";
    
    
    private void clickManagedRepositories()
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );
        
        clickLinkWithText( "Managed Repositories" );
        assertPage( "Administration" );
        assertTextPresent( "Administration" );
    }
    
    private void createManagedRepository( String id, String url, String name, String directory )
    {
        clickManagedRepositories();
        
        clickLinkWithText( "Add Repository" );
        assertTextPresent( "Configuration" );
        
        setFieldValue( "addRepository_id", id );
        setFieldValue( "urlName", url );
        setFieldValue( "addRepository_name", name );
        setFieldValue( "addRepository_directory", directory );
        
        clickButtonWithValue( "Add Repository", false );
    }
    
    private void removeManagedRepository( String id )
    {
        logout();
        
        clickManagedRepositories();
        
        clickLinkWithLocator( "//a[contains(@href, '/admin/deleteRepository!input.action?repoId=" + id + "')]" );
        clickLinkWithLocator( "deleteRepository_operationdelete-contents", false );
        clickButtonWithValue( "Go" );
        
        assertPage( "Administration" );
        assertTextNotPresent( TEST_REPOSITORY_ID );
    }
    
    public void testAddRepositoryWithValidValues()
    {
        createManagedRepository( TEST_REPOSITORY_ID, TEST_REPOSITORY_URL, TEST_REPOSITORY_NAME, TEST_REPOSITORY_DIRECTORY );
        waitPage();
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_ID );
        
        removeManagedRepository( TEST_REPOSITORY_ID );
    }
    
    public void testAddRepositoryWithInvalidValues()
    {
        createManagedRepository( "", "", "", "" );
        
        assertTextPresent( "You must enter the repository identifier." );
        assertTextPresent( "You must enter the url name." );
        assertTextPresent( "You must enter the repository name." );
        assertTextPresent( "You must enter the repository directory." );
    }
    
    public void testEditRepositoryWithValidValues()
    {
        createManagedRepository( TEST_REPOSITORY_ID, TEST_REPOSITORY_URL, TEST_REPOSITORY_NAME, TEST_REPOSITORY_DIRECTORY );
        waitPage();
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_NAME );
        
        clickLinkWithLocator( "//a[contains(@href, '/admin/editRepository!input.action?repoId=" + TEST_REPOSITORY_ID + "')]" );
        assertPage( "Configuration" );
        assertTextPresent( "Configuration" );
        
        assertTextPresent( "Edit Managed Repository" );
        assertEquals( TEST_REPOSITORY_URL, getFieldValue( "urlName" ) );
        assertEquals( TEST_REPOSITORY_NAME, getFieldValue( "editRepository_name" ) );
        assertTrue( getFieldValue( "editRepository_directory" ).endsWith( TEST_REPOSITORY_DIRECTORY ) );
        
        setFieldValue( "urlName", "edited-" + TEST_REPOSITORY_URL );
        setFieldValue( "editRepository_name", "edited-" + TEST_REPOSITORY_NAME );
        setFieldValue( "editRepository_directory", "edited-" + TEST_REPOSITORY_DIRECTORY );
        
        clickButtonWithValue( "Update Repository" );
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_ID );
        assertTextPresent( "edited-" + TEST_REPOSITORY_NAME );
        
        removeManagedRepository( TEST_REPOSITORY_ID );
    }
    
    public void testEditRepositoryWithInvalidValues()
    {
        createManagedRepository( TEST_REPOSITORY_ID, TEST_REPOSITORY_URL, TEST_REPOSITORY_NAME, TEST_REPOSITORY_DIRECTORY );
        waitPage();
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_NAME );
        
        clickLinkWithLocator( "//a[contains(@href, '/admin/editRepository!input.action?repoId=" + TEST_REPOSITORY_ID + "')]" );
        assertPage( "Configuration" );
        assertTextPresent( "Configuration" );
        
        assertTextPresent( "Edit Managed Repository" );
        assertEquals( TEST_REPOSITORY_URL, getFieldValue( "urlName" ) );
        assertEquals( TEST_REPOSITORY_NAME, getFieldValue( "editRepository_name" ) );
        assertTrue( getFieldValue( "editRepository_directory" ).endsWith( TEST_REPOSITORY_DIRECTORY ) );
        
        setFieldValue( "urlName", "" );
        setFieldValue( "editRepository_name", "" );
        setFieldValue( "editRepository_directory", "" );
        
        clickButtonWithValue( "Update Repository", false );
        assertTextPresent( "You must enter the url name." );
        assertTextPresent( "You must enter the repository name." );
        assertTextPresent( "You must enter the repository directory." );
        
        removeManagedRepository( TEST_REPOSITORY_ID );
    }
    
    public void testDeleteRepositoryButLeaveUnmodified()
    {
        createManagedRepository( TEST_REPOSITORY_ID, TEST_REPOSITORY_URL, TEST_REPOSITORY_NAME, TEST_REPOSITORY_DIRECTORY );
        waitPage();
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_ID );
        
        clickLinkWithLocator( "//a[contains(@href, '/admin/deleteRepository!input.action?repoId=" + TEST_REPOSITORY_ID + "')]" );
        clickLinkWithLocator( "deleteRepository_operationunmodified", false );
        clickButtonWithValue( "Go" );
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_ID );
        
        removeManagedRepository( TEST_REPOSITORY_ID );
    }
    
    public void testDeleteRepositoryAndContents()
    {
        createManagedRepository( TEST_REPOSITORY_ID, TEST_REPOSITORY_URL, TEST_REPOSITORY_NAME, TEST_REPOSITORY_DIRECTORY );
        waitPage();
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_ID );
        
        removeManagedRepository( TEST_REPOSITORY_ID );
    }
    
    public void testDeleteRepositoryButLeaveContentsUnmodified()
    {
        createManagedRepository( TEST_REPOSITORY_ID, TEST_REPOSITORY_URL, TEST_REPOSITORY_NAME, TEST_REPOSITORY_DIRECTORY );
        waitPage();
        
        assertPage( "Administration" );
        assertTextPresent( TEST_REPOSITORY_ID );
        
        clickLinkWithLocator( "//a[contains(@href, '/admin/deleteRepository!input.action?repoId=" + TEST_REPOSITORY_ID + "')]" );
        clickLinkWithLocator( "deleteRepository_operationdelete-entry", false );
        clickButtonWithValue( "Go" );
        
        assertPage( "Administration" );
        assertTextNotPresent( TEST_REPOSITORY_ID );
    }
}
