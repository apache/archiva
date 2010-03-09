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
    
    // TODO: add test for adding via WebDAV
    @Test (dependsOnMethods = { "testAddArtifactValidValues" }, groups = "requiresUpload")
    public void testViewAuditLogsDataFound()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        selectValue( "repository", "internal" );
        setFieldValue( "groupId", getProperty( "VALIDARTIFACT_GROUPID" ) );
        submit();
                
        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( getProperty( "VALIDARTIFACT_ARTIFACTID" ) + "-" + getProperty( "ARTIFACT_VERSION" ) +
                           "." + getProperty( "ARTIFACT_PACKAGING" ) );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }
    
    // TODO: add test for adding via WebDAV
    @Test (dependsOnMethods = { "testAddArtifactValidValues" }, groups = "requiresUpload")
    public void testViewAuditLogsOnlyArtifactIdIsSpecified()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        selectValue( "repository", "internal" );
        setFieldValue( "artifactId", getProperty( "AUDITLOGARTIFACT_ARTIFACTID" ) );
        submit();
                
        assertAuditLogsReportPage();
        assertTextPresent( "If you specify an artifact ID, you must specify a group ID" );
        assertTextNotPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextNotPresent( getProperty( "VALIDARTIFACT_ARTIFACTID" ) + "-" + getProperty( "ARTIFACT_VERSION" ) +
            "." + getProperty( "ARTIFACT_PACKAGING" ) );
        assertTextNotPresent( "Uploaded File" );
    }
    
    // TODO: add test for adding via WebDAV
    @Test (dependsOnMethods = { "testAddArtifactValidValues" }, groups = "requiresUpload")
    public void testViewAuditLogsForAllRepositories()
    {
        goToAuditLogReports();        
        assertAuditLogsReportPage();
        
        selectValue( "repository", "all" );
        submit();
        
        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( getProperty( "VALIDARTIFACT_ARTIFACTID" ) + "-" + getProperty( "ARTIFACT_VERSION" ) +
            "." + getProperty( "ARTIFACT_PACKAGING" ) );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }
    
    @Test (dependsOnMethods = { "testAddArtifactValidValues", "testUserWithRepoManagerInternalRole" }, groups = "requiresUpload")
    public void testViewAuditLogsViewAuditEventsForManageableRepositoriesOnly()
    {
        String groupId = getProperty( "SNAPSHOT_GROUPID" );
        String artifactId = getProperty( "SNAPSHOT_ARTIFACTID" );
        String version = getProperty( "SNAPSHOT_VERSION" );
        String repo = getProperty( "SNAPSHOT_REPOSITORYID" );
        String packaging = getProperty( "SNAPSHOT_PACKAGING" );
        
        addArtifact( groupId, artifactId, version, packaging, getProperty( "SNAPSHOT_ARTIFACTFILEPATH" ), repo );        
        assertTextPresent( "Artifact '" + groupId + ":" + artifactId + ":" + version +
            "' was successfully deployed to repository '" + repo + "'" );
        
        clickLinkWithText( "Logout" );
                
        login( getProperty( "REPOMANAGER_INTERNAL_USERNAME" ), getUserRoleNewPassword() );

        goToAuditLogReports();
        assertAuditLogsReportPage();

        selectValue( "repository", "all" );
        submit();

        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( getProperty( "VALIDARTIFACT_ARTIFACTID" ) + "-" + getProperty( "ARTIFACT_VERSION" ) + "." + packaging );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );

        assertTextNotPresent( artifactId + "-" + version + "." + packaging );
        clickLinkWithText( "Logout" );
        login( getProperty( "ADMIN_USERNAME" ), getProperty( "ADMIN_PASSWORD" ) );
    }
    
    // MRM-1304
    @Test( dependsOnMethods = { "testAddArtifactValidValues" }, groups = "requiresUpload" )
    public void testViewAuditLogsReportForGroupId()
    {
        String groupId = getProperty( "AUDITLOGARTIFACT_GROUPID" );
        String artifactId = getProperty( "AUDITLOGARTIFACT_ARTIFACTID" );
        String version = getProperty( "VERSION" );
        String packaging = getProperty( "PACKAGING" );
        String repositoryId = getProperty( "REPOSITORYID" );
        String expectedArtifact = getProperty( "AUDITLOG_EXPECTED_ARTIFACT" );

        addArtifact( groupId, artifactId, version, packaging, getProperty( "SNAPSHOT_ARTIFACTFILEPATH" ), repositoryId );

        goToAuditLogReports();

        selectValue( "repository", repositoryId );
        setFieldValue( "groupId", groupId );
        submit();

        assertAuditLogsReportPage();
        assertTextPresent( expectedArtifact );
        assertTextPresent( repositoryId );
    }
}
