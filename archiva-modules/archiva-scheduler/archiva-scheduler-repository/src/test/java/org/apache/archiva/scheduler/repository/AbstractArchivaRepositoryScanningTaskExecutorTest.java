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
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.mock.MockRepositorySessionFactory;
import org.apache.archiva.components.taskqueue.execution.TaskExecutor;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * ArchivaRepositoryScanningTaskExecutorPhase1Test
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public abstract class AbstractArchivaRepositoryScanningTaskExecutorTest
    extends TestCase
{
    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    @Named( value = "taskExecutor#test-repository-scanning" )
    protected TaskExecutor<RepositoryTask> taskExecutor;

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

    protected Path repoDir;

    protected static final String TEST_REPO_ID = "testRepo";

    protected MetadataRepository metadataRepository;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        Path sourceRepoDir = Paths.get( "src/test/repositories/default-repository" );
        repoDir = Paths.get( "target/default-repository" );

        FileUtils.deleteDirectory( repoDir );
        assertFalse( "Default Test Repository should not exist.", Files.exists(repoDir) );

        Files.createDirectories(repoDir);

        org.apache.commons.io.FileUtils.copyDirectory( sourceRepoDir.toFile(), repoDir.toFile() );
        // set the timestamps to a time well in the past
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.YEAR, -1 );
        try(Stream<Path> stream = Files.walk( repoDir,FileVisitOption.FOLLOW_LINKS)) {
            stream.forEach( path ->
            {
                try
                {
                    Files.setLastModifiedTime( path, FileTime.fromMillis( cal.getTimeInMillis( ) ) );
                }
                catch ( IOException e )
                {
                    e.printStackTrace( );
                }
            } );
        }
        PathMatcher m = FileSystems.getDefault().getPathMatcher( "glob:**/.svn" );
        Files.walk(repoDir, FileVisitOption.FOLLOW_LINKS).filter(Files::isDirectory)
            .sorted( Comparator.reverseOrder( ))
            .filter( path -> m.matches( path ) )
            .forEach( path ->
                org.apache.archiva.common.utils.FileUtils.deleteQuietly( path )
            );

        assertTrue( "Default Test Repository should exist.", Files.exists(repoDir) && Files.isDirectory( repoDir) );

        assertNotNull( archivaConfig );

        // Create it
        ManagedRepositoryConfiguration repositoryConfiguration = new ManagedRepositoryConfiguration();
        repositoryConfiguration.setId( TEST_REPO_ID );
        repositoryConfiguration.setName( "Test Repository" );
        repositoryConfiguration.setLocation( repoDir.toAbsolutePath().toString() );
        for ( ManagedRepository repo : repositoryRegistry.getManagedRepositories()) {
            repositoryRegistry.removeRepository( repo );
        }
        repositoryRegistry.putRepository( repositoryConfiguration );

        metadataRepository = mock( MetadataRepository.class );

        factory.setRepository( metadataRepository );
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoDir );

        assertFalse( Files.exists(repoDir) );

        super.tearDown();
    }

}
