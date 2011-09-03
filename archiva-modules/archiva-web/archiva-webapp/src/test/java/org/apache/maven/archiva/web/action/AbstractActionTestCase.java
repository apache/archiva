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

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.providers.XWorkConfigurationProvider;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;
import org.apache.archiva.metadata.generic.GenericMetadataFacet;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.memory.TestMetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectParent;
import org.apache.archiva.security.UserRepositoriesStub;
import org.apache.struts2.StrutsSpringTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractActionTestCase
    extends StrutsSpringTestCase
{
    protected static final String TEST_REPO = "test-repo";

    protected TestMetadataResolver metadataResolver;

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

    protected static final String TEST_GENERIC_METADATA_PROPERTY_NAME = "rating";

    protected static final String TEST_GENERIC_METADATA_PROPERTY_VALUE = "5 stars";

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    protected void setObservableRepos( List<String> repoIds )
    {
        UserRepositoriesStub repos = applicationContext.getBean( "userRepositories", UserRepositoriesStub.class );
        repos.setObservableRepositoryIds( repoIds );
    }

    protected void assertDefaultModel( ProjectVersionMetadata model, String version )
    {
        assertDefaultModel( model, TEST_GROUP_ID, TEST_ARTIFACT_ID, version );
    }

    protected void assertDefaultModel( ProjectVersionMetadata model, String groupId, String artifactId, String version )
    {
        assertEquals( version, model.getVersion() );
        assertEquals( TEST_URL, model.getUrl() );
        assertEquals( TEST_NAME, model.getName() );
        assertEquals( TEST_DESCRIPTION, model.getDescription() );
        assertEquals( TEST_ORGANIZATION_NAME, model.getOrganization().getName() );
        assertEquals( TEST_ORGANIZATION_URL, model.getOrganization().getUrl() );
        assertEquals( 2, model.getLicenses().size() );
        License l = model.getLicenses().get( 0 );
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

        MavenProjectFacet mavenFacet = (MavenProjectFacet) model.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( groupId, mavenFacet.getGroupId() );
        assertEquals( artifactId, mavenFacet.getArtifactId() );
        assertEquals( TEST_PACKAGING, mavenFacet.getPackaging() );
        assertEquals( TEST_PARENT_GROUP_ID, mavenFacet.getParent().getGroupId() );
        assertEquals( TEST_PARENT_ARTIFACT_ID, mavenFacet.getParent().getArtifactId() );
        assertEquals( TEST_PARENT_VERSION, mavenFacet.getParent().getVersion() );
    }

    protected ProjectVersionMetadata createProjectModel( String version )
    {
        return createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, version );
    }

    protected ProjectVersionMetadata createProjectModel( String groupId, String artifactId, String version )
    {
        ProjectVersionMetadata model = new ProjectVersionMetadata();
        model.setId( version );
        model.setUrl( TEST_URL );
        model.setName( TEST_NAME );
        model.setDescription( TEST_DESCRIPTION );
        CiManagement ci = new CiManagement();
        ci.setSystem( TEST_CI_SYSTEM );
        ci.setUrl( TEST_CI_URL );
        model.setCiManagement( ci );
        IssueManagement issue = new IssueManagement();
        issue.setSystem( TEST_ISSUE_SYSTEM );
        issue.setUrl( TEST_ISSUE_URL );
        model.setIssueManagement( issue );
        Organization organization = new Organization();
        organization.setName( TEST_ORGANIZATION_NAME );
        organization.setUrl( TEST_ORGANIZATION_URL );
        model.setOrganization( organization );
        License l = new License();
        l.setName( TEST_LICENSE_NAME );
        l.setUrl( TEST_LICENSE_URL );
        model.addLicense( l );
        l = new License();
        l.setName( TEST_LICENSE_NAME_2 );
        l.setUrl( TEST_LICENSE_URL_2 );
        model.addLicense( l );
        Scm scm = new Scm();
        scm.setConnection( TEST_SCM_CONNECTION );
        scm.setDeveloperConnection( TEST_SCM_DEV_CONNECTION );
        scm.setUrl( TEST_SCM_URL );
        model.setScm( scm );

        MavenProjectFacet mavenProjectFacet = new MavenProjectFacet();
        mavenProjectFacet.setGroupId( groupId );
        mavenProjectFacet.setArtifactId( artifactId );
        mavenProjectFacet.setPackaging( TEST_PACKAGING );
        MavenProjectParent parent = new MavenProjectParent();
        parent.setGroupId( TEST_PARENT_GROUP_ID );
        parent.setArtifactId( TEST_PARENT_ARTIFACT_ID );
        parent.setVersion( TEST_PARENT_VERSION );
        mavenProjectFacet.setParent( parent );
        model.addFacet( mavenProjectFacet );

        GenericMetadataFacet genericMetadataFacet = new GenericMetadataFacet();
        Map<String, String> props = new HashMap<String, String>();
        props.put( TEST_GENERIC_METADATA_PROPERTY_NAME, TEST_GENERIC_METADATA_PROPERTY_VALUE );
        genericMetadataFacet.setAdditionalProperties( props );
        model.addFacet( genericMetadataFacet );

        return model;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        ConfigurationManager configurationManager = new ConfigurationManager();
        configurationManager.addContainerProvider( new XWorkConfigurationProvider() );
        Configuration config = configurationManager.getConfiguration();
        Container container = config.getContainer();

        ValueStack stack = container.getInstance( ValueStackFactory.class ).createValueStack();
        stack.getContext().put( ActionContext.CONTAINER, container );
        ActionContext.setContext( new ActionContext( stack.getContext() ) );
    }
}
