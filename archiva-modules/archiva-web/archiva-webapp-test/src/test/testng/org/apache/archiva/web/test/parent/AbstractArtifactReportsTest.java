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

public abstract class AbstractArtifactReportsTest
    extends AbstractArchivaTest
{

    // Reports
    public void goToReportsPage()
    {
        clickLinkWithText( "Reports" );
        assertReportsPage();
    }

    public void assertReportsPage()
    {
        assertPage( "Apache Archiva \\ Reports" );
        assertTextPresent( "Reports" );
        assertTextPresent( "Repository Statistics" );
        assertTextPresent( "Repositories To Be Compared" );
        assertElementPresent( "availableRepositories" );
        assertButtonWithValuePresent( "v" );
        assertButtonWithValuePresent( "^" );
        assertButtonWithValuePresent( "<-" );
        assertButtonWithValuePresent( "->" );
        assertButtonWithValuePresent( "<<--" );
        assertButtonWithValuePresent( "-->>" );
        assertButtonWithValuePresent( "<*>" );
        assertElementPresent( "selectedRepositories" );
        assertButtonWithValuePresent( "v" );
        assertButtonWithValuePresent( "^" );
        assertTextPresent( "Row Count" );
        assertElementPresent( "rowCount" );
        assertTextPresent( "Start Date" );
        assertElementPresent( "startDate" );
        assertTextPresent( "End Date" );
        assertElementPresent( "endDate" );
        assertButtonWithValuePresent( "View Statistics" );
        assertTextPresent( "Repository Health" );
        assertTextPresent( "Row Count" );
        assertElementPresent( "rowCount" );
        assertTextPresent( "Group ID" );
        assertElementPresent( "groupId" );
        assertTextPresent( "Repository ID" );
        assertElementPresent( "repositoryId" );
        assertButtonWithValuePresent( "Show Report" );
    }

    public void compareRepositories( String labelSelected, String startDate, String endDate )
    {
        goToReportsPage();
        getSelenium().removeSelection( "generateStatisticsReport_availableRepositories", labelSelected );
        clickButtonWithValue( "->", false );
        getSelenium().type( "startDate", startDate );
        // clickLinkWithLocator( "1" , false );
        // getSelenium().click( "endDate" );
        getSelenium().type( "endDate", endDate );
        // clickLinkWithLocator( "30" , false );
        clickButtonWithValue( "View Statistics" );
    }

}