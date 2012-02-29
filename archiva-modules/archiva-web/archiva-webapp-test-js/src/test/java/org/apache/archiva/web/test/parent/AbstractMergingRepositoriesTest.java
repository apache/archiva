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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

public class AbstractMergingRepositoriesTest
    extends AbstractArchivaTest
{

    public void goToAuditLogReports()
    {
        getSelenium().open( "/archiva/report/queryAuditLogReport.action" );
    }

    public String getRepositoryDir()
    {
        File f = new File( "" );
        String artifactFilePath = f.getAbsolutePath();
        return artifactFilePath + "/target/";
    }

    public void editManagedRepository()
    {
        goToRepositoriesPage();
        clickLinkWithXPath( "//div[@id='contentArea']/div/div[5]/div[1]/a[1]/img" );
        assertPage( "Apache Archiva \\ Admin: Edit Managed Repository" );
        checkField( "repository.blockRedeployments" );
        clickButtonWithValue( "Update Repository" );
    }

    public String getGroupId()
    {
        return getProperty( "VALIDARTIFACT_GROUPID" );
    }

    public String getArtifactId()
    {
        return getProperty( "VALIDARTIFACT_ARTIFACTID" );
    }

    public String getVersion()
    {
        return getProperty( "VERSION" );
    }

    public String getPackaging()
    {
        return getProperty( "PACKAGING" );
    }

    public String getValidArtifactFilePath()
    {
        return "src/test/resources/snapshots/org/apache/maven/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar";
    }

}
