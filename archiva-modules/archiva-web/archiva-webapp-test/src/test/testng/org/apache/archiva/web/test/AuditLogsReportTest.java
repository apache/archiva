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

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.testng.annotations.Test;

@Test( groups = { "auditlogsreport" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class AuditLogsReportTest
    extends AbstractArchivaTest
{
    private void goToAuditLogReports()
    {
        clickLinkWithText( "Audit Log Report" );        
    }
    
    private void assertAuditLogsReportPage()
    {   
        assertPage( "Apache Archiva \\ Audit Log Report" );
        assertTextPresent( "Audit Log Report" );
        
        assertElementPresent( "repository" );
        assertElementPresent( "groupId" );
        assertElementPresent( "artifactId" );
        assertElementPresent( "startDate" );
        assertElementPresent( "endDate" );
        assertElementPresent( "rowCount" );
        assertButtonWithValuePresent( "View Audit Log" );
    }
    
    @Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
    public void testAuditLogsReport()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        assertTextPresent( "Latest Events" );
    }
    
    @Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
    public void testViewAuditLogsNoDataFound()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        setFieldValue( "groupId", "non.existing" );
        submit();
        
        assertPage( "Apache Archiva \\ Audit Log Report" );   
        assertTextPresent( "Results" );
        assertTextPresent( "No audit logs found." );
    }    
    
    @Test (dependsOnMethods = { "testAddArtifactValidValues" } )
    public void testViewAuditLogsDataFound()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        selectValue( "repository", "internal" );
        setFieldValue( "groupId", "test" );
        submit();
                
        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( "test:test:1.0" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }
    
    @Test (dependsOnMethods = { "testViewAuditLogsDataFound" } )
    public void testViewAuditLogsOnlyArtifactIdIsSpecified()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        selectValue( "repository", "internal" );
        setFieldValue( "artifactId", "test" );
        submit();
                
        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( "test:test:1.0" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }
    
    @Test (dependsOnMethods = { "testViewAuditLogsOnlyArtifactIdIsSpecified" } )
    public void testViewAuditLogsForAllRepositories()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        selectValue( "repository", "all" );
        submit();
        
        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( "test:test:1.0" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }
}
