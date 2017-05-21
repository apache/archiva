package org.apache.archiva.scheduler.repository;

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

import junit.framework.TestCase;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.mock.MockRepositorySessionFactory;
import org.apache.archiva.redback.components.taskqueue.execution.TaskExecutor;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Calendar;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * ArchivaRepositoryScanningTaskExecutorPhase1Test
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public abstract class ArchivaRepositoryScanningTaskExecutorAbstractTest
    extends TestCase
{
    @Inject
    @Named( value = "taskExecutor#test-repository-scanning" )
    protected TaskExecutor taskExecutor;

    @Inject
    @Named( value = "archivaConfiguration#test-repository-scanning" )
    protected ArchivaConfiguration archivaConfig;

    @Inject
    @Named( value = "repositoryStatisticsManager#test" )
    protected RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    @Named( value = "knownRepositoryContentConsumer#test-consumer" )
    protected TestConsumer testConsumer;

    @Inject
    @Named( value = "repositorySessionFactory#mock" )
    private MockRepositorySessionFactory factory;

    protected File repoDir;

    protected static final String TEST_REPO_ID = "testRepo";

    protected MetadataRepository metadataRepository;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        File sourceRepoDir = new File( "./src/test/repositories/default-repository" );
        repoDir = new File( "./target/default-repository" );

        FileUtils.deleteDirectory( repoDir );
        assertFalse( "Default Test Repository should not exist.", repoDir.exists() );

        repoDir.mkdir();

        FileUtils.copyDirectoryStructure( sourceRepoDir, repoDir );
        // set the timestamps to a time well in the past
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.YEAR, -1 );
        for ( File f : (List<File>) FileUtils.getFiles( repoDir, "**", null ) )
        {
            f.setLastModified( cal.getTimeInMillis() );
        }
        // TODO: test they are excluded instead
        for ( String dir : (List<String>) FileUtils.getDirectoryNames( repoDir, "**/.svn", null, false ) )
        {
            FileUtils.deleteDirectory( new File( repoDir, dir ) );
        }

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        assertNotNull( archivaConfig );

        // Create it
        ManagedRepositoryConfiguration repositoryConfiguration = new ManagedRepositoryConfiguration();
        repositoryConfiguration.setId( TEST_REPO_ID );
        repositoryConfiguration.setName( "Test Repository" );
        repositoryConfiguration.setLocation( repoDir.getAbsolutePath() );
        archivaConfig.getConfiguration().getManagedRepositories().clear();
        archivaConfig.getConfiguration().addManagedRepository( repositoryConfiguration );

        metadataRepository = mock( MetadataRepository.class );

        factory.setRepository( metadataRepository );
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( repoDir );

        assertFalse( repoDir.exists() );

        super.tearDown();
    }

}
