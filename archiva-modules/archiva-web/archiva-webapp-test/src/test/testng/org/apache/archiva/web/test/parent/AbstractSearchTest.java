package org.apache.archiva.web.test.parent;

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

public abstract class AbstractSearchTest
    extends AbstractArchivaTest
{
    // Search
    public void goToSearchPage()
    {
        goToHomePage();
        //if ( !"Apache Archiva \\ Quick Search".equals( getTitle() ) )
        if ( isElementPresent( "quickSearchBox" ) )
        {
            //clickLinkWithText( "Search" );
            clickLinkWithLocator( "menuSearchLink" );
            getSelenium().waitForPageToLoad( maxWaitTimeInMs );
            assertElementPresent( "quickSearchSubmit" );
            //assertPage( "Apache Archiva \\ Quick Search" );
        }
    }

    public void assertSearchPage()
    {
        assertPage( "Apache Archiva \\ Quick Search" );
        assertTextPresent( "Search for" );
        assertElementPresent( "quickSearchSubmit" );
        assertButtonWithValuePresent( "Search" );
        // assertLinkPresent( "Advanced Search" );
        assertTextPresent( "Enter your search terms. A variety of data will be searched for your keywords." );
        // assertButtonWithDivIdPresent( "searchHint" );
    }

    public void searchForArtifact( String artifactId )
    {
        goToSearchPage();

        getSelenium().type( "dom=document.forms[1].elements[0]", artifactId );
        //clickButtonWithValue( "Search" );
        clickButtonWithLocator( "quickSearchSubmit" );
    }

    public void searchForArtifactAdvancedSearch( String groupId, String artifactId, String version, String repositoryId,
                                                 String className, String rowCount )
    {
        goToSearchPage();

        clickLinkWithXPath( "//div[@id='contentArea']/div[1]/a[1]/strong", false );
        assertElementPresent( "filteredSearch_searchField" );
        assertElementPresent( "filteredSearch_repositoryId" );

        if ( groupId != null )
        {
            selectValue( "filteredSearch_searchField", "Group ID" );
            clickLinkWithLocator( "//a[@id='filteredSearch_']/img", false );

            assertElementPresent( "groupId" );
            setFieldValue( "groupId", groupId );
        }

        if ( artifactId != null )
        {
            selectValue( "filteredSearch_searchField", "Artifact ID" );
            clickLinkWithLocator( "//a[@id='filteredSearch_']/img", false );

            assertElementPresent( "artifactId" );
            setFieldValue( "artifactId", artifactId );
        }

        if ( version != null )
        {
            selectValue( "filteredSearch_searchField", "Version" );
            clickLinkWithLocator( "//a[@id='filteredSearch_']/img", false );

            assertElementPresent( "version" );
            setFieldValue( "version", version );
        }

        if ( className != null )
        {
            selectValue( "filteredSearch_searchField", "Class/Package Name" );
            clickLinkWithLocator( "//a[@id='filteredSearch_']/img", false );

            assertElementPresent( "className" );
            setFieldValue( "className", className );
        }

        if ( rowCount != null )
        {
            selectValue( "filteredSearch_searchField", "Row Count" );
            clickLinkWithLocator( "//a[@id='filteredSearch_']/img", false );

            assertElementPresent( "rowCount" );
            setFieldValue( "rowCount", rowCount );
        }

        if ( repositoryId != null )
        {
            selectValue( "filteredSearch_repositoryId", repositoryId );
        }
        clickSubmitWithLocator( "filteredSearch_0" );
    }
}