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

public abstract class AbstractArtifactManagementTest
    extends AbstractArchivaTest
{

    public String getGroupId()
    {
        String groupId = getProperty( "GROUPID" );
        return groupId;
    }

    public String getArtifactId()
    {
        String artifactId = getProperty( "ARTIFACTID" );
        return artifactId;
    }

    public String getVersion()
    {
        String version = getProperty( "VERSION" );
        return version;
    }

    public String getPackaging()
    {
        String packaging = getProperty( "PACKAGING" );
        return packaging;
    }

    public String getArtifactFilePath()
    {
        return "src/test/resources/snapshots/org/apache/maven/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar";
    }

    public String getRepositoryId()
    {
        String repositoryId = getProperty( "REPOSITORYID" );
        return repositoryId;
    }

    public void goToDeleteArtifactPage()
    {
        login( getProperty( "ADMIN_USERNAME" ), getProperty( "ADMIN_PASSWORD" ) );
        getSelenium().open( "/archiva/deleteArtifact.action" );
        assertDeleteArtifactPage();
    }

    public void deleteArtifact( String groupId, String artifactId, String version, String repositoryId )
    {
        deleteArtifact( groupId, artifactId, version, repositoryId, false );
    }

    public void deleteArtifact( String groupId, String artifactId, String version, String repositoryId, boolean wait )
    {
        goToDeleteArtifactPage();
        setFieldValue( "groupId", groupId );
        setFieldValue( "artifactId", artifactId );
        setFieldValue( "version", version );
        selectValue( "repositoryId", repositoryId );
        clickButtonWithValue( "Submit", false );
    }
}
