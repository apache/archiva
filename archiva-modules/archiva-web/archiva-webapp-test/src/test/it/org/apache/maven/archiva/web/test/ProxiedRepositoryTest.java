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
 * Test archiva proxied repositories configuration
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class ProxiedRepositoryTest
    extends AbstractArchivaTestCase
{
    /**
     *
     */
    protected void initialize()
    {
        super.initialize();

        createTestRepo();
    }

    /**
     * Create a proxied repo which will be used for testing
     */
    private void createTestRepo()
    {
        clickProxiedRepositories();

        if ( isTextPresent( "There are no proxied repositories configured yet." ) )
        {
            clickLinkWithText( "Add Repository" );
            assertPage( "Configuration" );
            setFieldValue( "id", "test-proxied" );
            setFieldValue( "name", "Test Proxied Repository" );
            setFieldValue( "url", "http://test.com/test-proxied" );
            clickButtonWithValue( "Add Repository" );
            waitPage();

            assertPage( "Administration" );
            assertTextPresent( "Test Proxied Repository" );
            assertLinkPresent( "Edit Repository" );
        }

        logout();
    }

    /**
     * Test add proxied repo with invalid data
     */
    public void testInvalidAddProxiedRepoConfiguration()
    {
        clickProxiedRepositories();

        clickLinkWithText( "Add Repository" );
        assertPage( "Configuration" );

        clickButtonWithValue( "Add Repository", false );
        assertPage( "Configuration" );
        assertTextPresent( "You must enter the repository identifier." );
        assertTextPresent( "You must enter the repository name." );
        assertTextPresent( "You must enter the repository URL." );

        logout();
    }

    /**
     * Test edit proxied repo with valid data
     */
    public void testValidEditProxiedRepoConfiguration()
    {
        clickProxiedRepositories();
        clickLinkWithText( "Edit Repository" );

        assertPage( "Configuration" );
        assertTextPresent( "Edit Proxied Repository" );
        setFieldValue( "name", "Test Valid" );
        setFieldValue( "url", "http://valid.org/test-valid" );
        clickButtonWithValue( "Update Repository" );
        waitPage();

        assertPage( "Administration" );
        assertTextPresent( "Test Valid" );
        assertLinkPresent( "Edit Repository" );

        logout();
    }

    /**
     * Test edit proxied repo with invalid data
     */
    public void testInvalidEditProxiedRepoConfiguration()
    {
        clickProxiedRepositories();
        clickLinkWithText( "Edit Repository" );

        assertPage( "Configuration" );
        assertTextPresent( "Edit Proxied Repository" );
        setFieldValue( "name", "" );
        setFieldValue( "url", "" );
        clickButtonWithValue( "Update Repository", false );

        assertPage( "Configuration" );
        assertTextPresent( "You must enter the repository name." );
        assertTextPresent( "You must enter the repository URL." );

        logout();
    }

    /**
     * Test delete repository, unmodified entry and contents
     */
    public void testDeleteRepoUnmodified()
    {
        clickProxiedRepositories();
        clickLinkWithText( "Delete Repository" );

        assertPage( "Configuration" );
        assertTextPresent( "Delete Proxied Repository" );
        clickButtonWithValue( "Go" );

        assertPage( "Administration" );
        assertTextPresent( "Test Proxied Repository" );

        logout();
    }

    /**
     * Test delete repository including contents
     */
    public void testDeleteRepoRemoveFromDisk()
    {
        clickProxiedRepositories();
        clickLinkWithText( "Delete Repository" );

        assertPage( "Configuration" );
        clickLinkWithLocator( "deleteProxiedRepository_operationdelete-contents", false );
        clickButtonWithValue( "Go" );

        assertPage( "Administration" );
        assertTextNotPresent( "Test Proxied Repository" );

        logout();
    }

    /**
     * Test delete repository, unmodified contents/entry deleted
     */
    public void testDeleteRepoUnmodifiedContents()
    {
        clickProxiedRepositories();
        clickLinkWithText( "Delete Repository" );

        assertPage( "Configuration" );
        clickLinkWithLocator( "deleteProxiedRepository_operationdelete-entry", false );
        clickButtonWithValue( "Go" );

        assertPage( "Administration" );
        assertTextNotPresent( "Test Proxied Repository" );

        logout();
    }

    /**
     * Click Settings from the navigation menu
     */
    private void clickProxiedRepositories()
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );

        clickLinkWithText( "Proxied Repositories" );
        assertPage( "Administration" );
        assertTextPresent( "Proxied Repositories" );
    }

    /**
     * Remove the created test repo
     */
    protected void removeTestRepo()
    {
        if ( !isLinkPresent( "Login" ) )
        {
            logout();
        }

        clickProxiedRepositories();

        if ( isTextPresent( "Delete Repository " ) )
        {
            clickLinkWithText( "Delete Repository" );
            assertPage( "Configuration" );
            clickLinkWithLocator( "deleteProxiedRepository_operationdelete-entry", false );
            clickButtonWithValue( "Go" );

            assertPage( "Administration" );
            assertTextNotPresent( "Test Proxied Repository" );
        }

        logout();
    }

    /**
     * Revert to original value
     *
     * @throws Exception
     */
    public void tearDown()
        throws Exception
    {
        removeTestRepo();

        super.tearDown();
    }

}
