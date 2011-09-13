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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.managed.ManagedRepository;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.memory.TestRepositorySessionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.struts2.StrutsSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.io.File;
import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteArtifactActionTest
    extends StrutsSpringTestCase
{
    private DeleteArtifactAction action;

    private ArchivaConfiguration configuration;

    private MockControl configurationControl;

    private RepositoryContentFactory repositoryFactory;

    private MockControl repositoryFactoryControl;

    private MetadataRepository metadataRepository;

    private MockControl metadataRepositoryControl;

    private static final String REPOSITORY_ID = "test-repo";

    private static final String GROUP_ID = "org.apache.archiva";

    private static final String ARTIFACT_ID = "npe-metadata";

    private static final String VERSION = "1.0";

    private static final String REPO_LOCATION = "target/test-classes/test-repo";

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        //action = (DeleteArtifactAction) lookup( Action.class.getName(), "deleteArtifactAction" );
        action = (DeleteArtifactAction) getActionProxy( "/deleteArtifact.action" ).getAction();
        assertNotNull( action );

        configurationControl = MockControl.createControl( ArchivaConfiguration.class );
        configuration = (ArchivaConfiguration) configurationControl.getMock();

        repositoryFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repositoryFactory = (RepositoryContentFactory) repositoryFactoryControl.getMock();

        metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();

        RepositorySession repositorySession = mock( RepositorySession.class );
        when( repositorySession.getRepository() ).thenReturn( metadataRepository );

        TestRepositorySessionFactory repositorySessionFactory =
            applicationContext.getBean( "repositorySessionFactory#test", TestRepositorySessionFactory.class );

        repositorySessionFactory.setRepositorySession( repositorySession );

        (( DefaultManagedRepositoryAdmin)action.getManagedRepositoryAdmin()).setArchivaConfiguration( configuration );
        action.setRepositoryFactory( repositoryFactory );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        action = null;

        super.tearDown();
    }

    public void testGetListeners()
        throws Exception
    {
        assertNotNull( action.getListeners() );
        assertFalse( action.getListeners().isEmpty() );
    }

    public void testNPEInDeleteArtifact()
        throws Exception
    {
        action.setGroupId( GROUP_ID );
        action.setArtifactId( ARTIFACT_ID );
        action.setVersion( VERSION );
        action.setRepositoryId( REPOSITORY_ID );

        Configuration config = createConfiguration();

        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository(
            new BeanReplicator().replicateBean( config.findManagedRepositoryById( REPOSITORY_ID ),
                                                ManagedRepository.class ) );

        configurationControl.expectAndReturn( configuration.getConfiguration(), config );
        repositoryFactoryControl.expectAndReturn( repositoryFactory.getManagedRepositoryContent( REPOSITORY_ID ),
                                                  repoContent );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getArtifacts( REPOSITORY_ID, GROUP_ID, ARTIFACT_ID, VERSION ),
            new ArrayList<ArtifactMetadata>() );

        configurationControl.replay();
        repositoryFactoryControl.replay();
        metadataRepositoryControl.replay();

        action.doDelete();

        String artifactPath = REPO_LOCATION + "/" + StringUtils.replace( GROUP_ID, ".", "/" ) + "/"
            + StringUtils.replace( ARTIFACT_ID, ".", "/" ) + "/" + VERSION + "/" + ARTIFACT_ID + "-" + VERSION;

        assertFalse( new File( artifactPath + ".jar" ).exists() );
        assertFalse( new File( artifactPath + ".jar.sha1" ).exists() );
        assertFalse( new File( artifactPath + ".jar.md5" ).exists() );

        assertFalse( new File( artifactPath + ".pom" ).exists() );
        assertFalse( new File( artifactPath + ".pom.sha1" ).exists() );
        assertFalse( new File( artifactPath + ".pom.md5" ).exists() );
    }

    private Configuration createConfiguration()
    {
        ManagedRepositoryConfiguration managedRepo = new ManagedRepositoryConfiguration();
        managedRepo.setId( REPOSITORY_ID );
        managedRepo.setName( "Test Repository" );

        managedRepo.setLocation( REPO_LOCATION );
        managedRepo.setReleases( true );

        Configuration config = new Configuration();
        config.addManagedRepository( managedRepo );

        return config;
    }
}
