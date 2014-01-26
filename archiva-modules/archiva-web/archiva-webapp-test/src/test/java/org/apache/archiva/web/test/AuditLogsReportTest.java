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

import org.apache.archiva.web.test.parent.AbstractArtifactManagementTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test( groups = { "auditlogsreport" } )
public class AuditLogsReportTest
    extends AbstractArtifactManagementTest
{
    @BeforeTest
    public void setUp()
    {
        loginAsAdmin();
        addArtifact( getGroupId(), "testAudit", getVersion(), getPackaging(), getArtifactFilePath(), getRepositoryId() );
    }

    private void goToAuditLogReports()
    {
        getSelenium().open( "/archiva/report/queryAuditLogReport.action" );
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

    @Test
    public void testAuditLogsReport()
    {
        goToAuditLogReports();
        assertAuditLogsReportPage();
        assertTextPresent( "Latest Events" );
    }

    @Test
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
    @Test (groups = "requiresUpload")
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
        assertTextPresent( "testAudit-1.0.jar" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }

    // TODO: add test for adding via WebDAV
    @Test ( groups = "requiresUpload")
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
        assertTextPresent( "testAudit-1.0.jar" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }

    // TODO: add test for adding via WebDAV
    @Test (groups = "requiresUpload")
    public void testViewAuditLogsForAllRepositories()
    {
        goToAuditLogReports();
        assertAuditLogsReportPage();

        selectValue( "repository", "all" );
        submit();

        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( "testAudit-1.0.jar" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );
    }

    @Test (groups = "requiresUpload")
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

        goToUserManagementPage();
        String username = "testAuditUser";
        if ( !isLinkPresent( username ) )
        {
            createUserWithRole( username, "Repository Manager - internal", getUserEmail(), getUserRolePassword() );

            logout();
            login( username, getUserRolePassword() );
            changePassword( getUserRolePassword(), getUserRoleNewPassword() );
        }
        else
        {
            logout();
            login( username, getUserRoleNewPassword() );
        }

        goToAuditLogReports();
        assertAuditLogsReportPage();

        selectValue( "repository", "all" );
        submit();

        assertAuditLogsReportPage();
        assertTextPresent( "Results" );
        assertTextNotPresent( "No audit logs found." );
        assertTextPresent( "testAudit-1.0.jar" );
        assertTextPresent( "Uploaded File" );
        assertTextPresent( "internal" );
        assertTextPresent( "admin" );

        assertTextNotPresent( artifactId + "-" + version + "." + packaging );
        clickLinkWithText( "Logout" );
        loginAsAdmin();
    }

    @Test ( groups = "requiresUpload")
    public void testViewAuditLogsReportForGroupId()
    {
        String groupId = getProperty("AUDITLOG_GROUPID");
        String artifactId = getProperty("ARTIFACTID");
        String version = getProperty("VERSION");
        String packaging = getProperty("PACKAGING");
        String repositoryId = getProperty("REPOSITORYID");
        String expectedArtifact = getProperty("AUDITLOG_EXPECTED_ARTIFACT");

        addArtifact( groupId, artifactId, version, packaging,  getProperty( "SNAPSHOT_ARTIFACTFILEPATH" ), repositoryId );

        goToAuditLogReports();

        selectValue( "repository", repositoryId );
        setFieldValue( "groupId", groupId );
        submit();

        assertAuditLogsReportPage();
        assertTextPresent( expectedArtifact );
        assertTextPresent( repositoryId );
    }
}
