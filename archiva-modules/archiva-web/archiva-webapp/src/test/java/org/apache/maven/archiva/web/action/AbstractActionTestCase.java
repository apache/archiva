package org.apache.maven.archiva.web.action;

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

import java.util.List;

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.security.UserRepositoriesStub;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public abstract class AbstractActionTestCase
    extends PlexusInSpringTestCase
{
    protected static final String TEST_REPO = "test-repo";

    protected static final String TEST_GROUP_ID = "groupId";

    protected static final String TEST_ARTIFACT_ID = "artifactId";

    protected static final String TEST_PACKAGING = "packaging";

    protected static final String TEST_ISSUE_URL = "http://jira.codehaus.org/browse/MRM";

    protected static final String TEST_ISSUE_SYSTEM = "jira";

    protected static final String TEST_CI_SYSTEM = "continuum";

    protected static final String TEST_CI_URL = "http://vmbuild.apache.org/";

    protected static final String TEST_URL = "url";

    protected static final String TEST_NAME = "name";

    protected static final String TEST_DESCRIPTION = "description";

    protected static final String TEST_PARENT_GROUP_ID = "parentGroupId";

    protected static final String TEST_PARENT_ARTIFACT_ID = "parentArtifactId";

    protected static final String TEST_PARENT_VERSION = "parentVersion";

    protected static final String TEST_ORGANIZATION_NAME = "organizationName";

    protected static final String TEST_ORGANIZATION_URL = "organizationUrl";

    protected static final String TEST_LICENSE_URL = "licenseUrl";

    protected static final String TEST_LICENSE_NAME = "licenseName";

    protected static final String TEST_LICENSE_URL_2 = "licenseUrl_2";

    protected static final String TEST_LICENSE_NAME_2 = "licenseName_2";

    protected static final String TEST_SCM_CONNECTION = "scmConnection";

    protected static final String TEST_SCM_DEV_CONNECTION = "scmDevConnection";

    protected static final String TEST_SCM_URL = "scmUrl";

    protected void setObservableRepos( List<String> repoIds )
    {
        UserRepositoriesStub repos = (UserRepositoriesStub) lookup( UserRepositories.class );
        repos.setObservableRepositoryIds( repoIds );
    }

    protected void assertDefaultModel( ArchivaProjectModel model, String version )
    {
        assertDefaultModel( model, TEST_GROUP_ID, TEST_ARTIFACT_ID, version );
    }

    protected void assertDefaultModel( ArchivaProjectModel model, String groupId, String artifactId, String version )
    {
        assertEquals( groupId, model.getGroupId() );
        assertEquals( artifactId, model.getArtifactId() );
        assertEquals( version, model.getVersion() );
        assertEquals( TEST_URL, model.getUrl() );
        assertEquals( TEST_NAME, model.getName() );
        assertEquals( TEST_DESCRIPTION, model.getDescription() );
        assertEquals( TEST_ORGANIZATION_NAME, model.getOrganization().getName() );
        assertEquals( TEST_ORGANIZATION_URL, model.getOrganization().getUrl() );
        assertEquals( 2, model.getLicenses().size() );
        org.apache.maven.archiva.model.License l = model.getLicenses().get( 0 );
        assertEquals( TEST_LICENSE_NAME, l.getName() );
        assertEquals( TEST_LICENSE_URL, l.getUrl() );
        l = model.getLicenses().get( 1 );
        assertEquals( TEST_LICENSE_NAME_2, l.getName() );
        assertEquals( TEST_LICENSE_URL_2, l.getUrl() );
        assertEquals( TEST_ISSUE_SYSTEM, model.getIssueManagement().getSystem() );
        assertEquals( TEST_ISSUE_URL, model.getIssueManagement().getUrl() );
        assertEquals( TEST_CI_SYSTEM, model.getCiManagement().getSystem() );
        assertEquals( TEST_CI_URL, model.getCiManagement().getUrl() );
        assertEquals( TEST_SCM_CONNECTION, model.getScm().getConnection() );
        assertEquals( TEST_SCM_DEV_CONNECTION, model.getScm().getDeveloperConnection() );
        assertEquals( TEST_SCM_URL, model.getScm().getUrl() );

        assertEquals( TEST_PACKAGING, model.getPackaging() );
        assertEquals( TEST_PARENT_GROUP_ID, model.getParentProject().getGroupId() );
        assertEquals( TEST_PARENT_ARTIFACT_ID, model.getParentProject().getArtifactId() );
        assertEquals( TEST_PARENT_VERSION, model.getParentProject().getVersion() );
    }
}
