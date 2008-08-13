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

public class SearchTest
    extends AbstractArchivaTestCase
{
    private static int PAGE_LOAD_WAIT = 1500;
    
    public void testSearchNonExistingArtifact()
    {
        searchForArtifact( "asdf" );
        
        waitPage( PAGE_LOAD_WAIT );

        assertTextPresent( "No results found" );
    }

    public void testSearchExistingArtifact()
    {
        searchForArtifact( "artifact-a" );

        waitPage( PAGE_LOAD_WAIT );
        
        assertPage( "Search Results" );
        
        assertTextPresent( "artifact-a" );
    }

    public void testViewSearchedArtifact()
    {
        // test viewing artifact (header link) listed in search results 

        searchForArtifact( "artifact-a" );
        
        waitPage( PAGE_LOAD_WAIT );

        getSelenium().click( "link=artifact-a" );
        
        waitPage( PAGE_LOAD_WAIT );

        assertPage( "Browse Repository" );

        assertTextPresent( "artifact-a" );

        //test viewing artifact listed in search results
        
        searchForArtifact( "artifact-a" );
        
        waitPage( PAGE_LOAD_WAIT );

        clickLinkWithText( "1.0" );
        
        waitPage( PAGE_LOAD_WAIT );
        
        assertPage( "Browse Repository" );

        assertTextPresent( "Artifact ID" );

        assertTextPresent( "artifact-a" );

        assertTextPresent( "Version" );

        assertTextPresent( "1.0" );
    }

    public void testBrowseSearchedArtifact()
    {
        // test viewing artifact listed in search results 

        searchForArtifact( "artifact-a" );
        
        waitPage( PAGE_LOAD_WAIT );

        getSelenium().click("//p[1]/span/a[1]");
        
        waitPage( PAGE_LOAD_WAIT );

        assertPage( "Browse Repository" );

        assertTextPresent( "artifact-a" );
    }

    public void testBrowseRepoFromSearchResults()
    {
        searchForArtifact( "artifact-a" );
        
        waitPage( PAGE_LOAD_WAIT );

        clickLinkWithText( "[top]" );
        
        waitPage( PAGE_LOAD_WAIT );
        
        assertPage( "Browse Repository" );
    }

    private void searchForArtifact( String artifactId )
    {
        if ( !"Maven Archiva :: Quick Search".equals( getSelenium().getTitle() ) )
        {
            clickLinkWithText( "Search" );
            
            waitPage( PAGE_LOAD_WAIT );
            
            assertPage( "Quick Search" );
        }

        setFieldValue( "quickSearch_q", artifactId );

        clickButtonWithValue( "Submit" );
    }
}