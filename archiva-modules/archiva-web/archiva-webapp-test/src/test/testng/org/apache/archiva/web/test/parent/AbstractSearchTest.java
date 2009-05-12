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
	//Search
	public void goToSearchPage()
	{
		clickLinkWithText( "Search" );
		assertSearchPage();
	}
	
	public void assertSearchPage()
	{
		assertPage( "Apache Archiva \\ Quick Search" );
		assertTextPresent( "Search for" );
		assertElementPresent( "quickSearch_q" );
		assertButtonWithValuePresent( "Search" );
		//assertLinkPresent( "Advanced Search" );
		assertTextPresent( "Enter your search terms. A variety of data will be searched for your keywords." );
		//assertButtonWithDivIdPresent( "searchHint" );
	}
	
	public void searchForArtifact( String artifactId )
    {
        if ( !"Apache Archiva \\ Quick Search".equals( getSelenium().getTitle() ) )
        {
            clickLinkWithText( "Search" );
            
            getSelenium().waitForPageToLoad( maxWaitTimeInMs );
            
            assertPage( "Apache Archiva \\ Quick Search" );
        }

        getSelenium().type( "dom=document.forms[1].elements[0]", artifactId );
        clickButtonWithValue( "Search" );
    }
}