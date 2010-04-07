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

import java.io.File;

import org.apache.archiva.web.test.parent.AbstractSearchTest;
import org.testng.annotations.Test;

@Test( groups = { "search" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class SearchTest
    extends AbstractSearchTest
{

    public void testSearchNonExistingArtifact()
        throws Exception
    {
        searchForArtifact( getProperty( "SEARCH_BAD_ARTIFACT" ) );
        assertTextPresent( "No results found" );
    }

    // TODO: make search tests more robust especially when comparing/asserting number of hits
    public void testSearchExistingArtifact()
    {
        searchForArtifact( getProperty( "ARTIFACT_ARTIFACTID" ) );
        assertTextPresent( "Results" );
        assertTextPresent( "Hits: 1 to 1 of 1" );
        assertLinkPresent( "test" );
    }

    public void testViewSearchedArtifact()
    {
        searchForArtifact( getProperty( "ARTIFACT_ARTIFACTID" ) );
        clickLinkWithText( getProperty( "ARTIFACT_ARTIFACTID" ) );
        assertPage( "Apache Archiva \\ Browse Repository" );
        assertTextPresent( getProperty( "ARTIFACT_ARTIFACTID" ) );
        clickLinkWithText( getProperty( "ARTIFACT_VERSION" ) + "/" );
        assertPage( "Apache Archiva \\ Browse Repository" );
    }

    public void testSearchNonExistingArtifactInAdvancedSearch()
    {
        searchForArtifactAdvancedSearch( null, getProperty( "SEARCH_BAD_ARTIFACT" ), null, null, null, null );
        assertTextPresent( "No results found" );
    }

    public void testSearchNoSearchCriteriaSpecifiedInAdvancedSearch()
    {
        searchForArtifactAdvancedSearch( null, null, null, null, null, null );
        assertTextPresent( "Advanced Search - At least one search criteria must be provided." );
    }

    public void testSearchExistingArtifactUsingAdvancedSearchArtifactId()
    {
        searchForArtifactAdvancedSearch( null, getProperty( "ARTIFACT_ARTIFACTID" ), null,
                                         getProperty( "REPOSITORYID" ), null, null );
        assertTextPresent( "Results" );
        assertTextPresent( "Hits: 1 to 1 of 1" );
        assertLinkPresent( "test" );
    }

    public void testSearchExistingArtifactUsingAdvancedSearchGroupId()
    {
        searchForArtifactAdvancedSearch( getProperty( "GROUPID" ), null, null, getProperty( "REPOSITORYID" ), null,
                                         null );
        assertTextPresent( "Results" );
        assertTextPresent( "Hits: 1 to 1 of 1" );
        assertLinkPresent( "test" );
    }

    public void testSearchExistingArtifactUsingAdvancedSearchNotInRepository()
    {
        searchForArtifactAdvancedSearch( null, getProperty( "ARTIFACT_ARTIFACTID" ), null, "snapshots", null, null );
        assertTextPresent( "No results found" );
        assertTextNotPresent( "Results" );
        assertTextNotPresent( "Hits: 1 to 1 of 1" );
        assertLinkNotPresent( "test" );
    }
}
