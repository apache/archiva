package org.apache.archiva.metadata.repository.stats;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.archiva.metadata.repository.DefaultMetadataResolver;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.jcr.JcrMetadataRepository;
import org.apache.archiva.metadata.repository.jcr.JcrRepositorySessionFactory;
import org.apache.archiva.metadata.repository.jcr.JcrRepositorySession;
import org.apache.archiva.metadata.repository.stats.model.DefaultRepositoryStatistics;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class JcrRepositoryStatisticsGatheringTest
    extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger( JcrRepositoryStatisticsGatheringTest.class );
    private static final int TOTAL_FILE_COUNT = 1000;

    private static final int NEW_FILE_COUNT = 500;

    private static final String TEST_REPO = "test-repo";

    static JcrMetadataRepository repository;
    static JcrRepositorySessionFactory sessionFactory;

    Session jcrSession;

    private static Repository jcrRepository;

    Logger logger = LoggerFactory.getLogger( getClass() );
    private int assertRetrySleepMs = 500;
    private int assertMaxTries = 5;

    @BeforeClass
    public static void setupSpec()
        throws IOException, InvalidFileStoreVersionException
    {
        Path directory = Paths.get( "target/test-repositories" );
        if ( Files.exists(directory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }
        directory = Paths.get( "target/jcr" );
        if (Files.exists( directory )) {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }

        List<MetadataFacetFactory> factories = AbstractMetadataRepositoryTest.createTestMetadataFacetFactories();

        MetadataService metadataService = new MetadataService( );
        metadataService.setMetadataFacetFactories( factories );

        JcrRepositorySessionFactory jcrSessionFactory = new JcrRepositorySessionFactory();
        jcrSessionFactory.setMetadataResolver(new DefaultMetadataResolver());
        jcrSessionFactory.setMetadataService(metadataService);

        jcrSessionFactory.open();
        sessionFactory = jcrSessionFactory;
        repository = jcrSessionFactory.getMetadataRepository();
    }


    @AfterClass
    public static void stopSpec() {
        try
        {
            repository.close();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        sessionFactory.close();
    }

    /*
     * Used by tryAssert to allow to throw exceptions in the lambda expression.
     */
    @FunctionalInterface
    protected interface AssertFunction
    {
        void accept( ) throws Exception;
    }

    protected void tryAssert( AssertFunction func ) throws Exception
    {
        tryAssert( func, assertMaxTries, assertRetrySleepMs );
    }


    /*
     * Runs the assert method until the assert is successful or the number of retries
     * is reached. This is needed because the JCR Oak index update is asynchronous, so updates
     * may not be visible immediately after the modification.
     */
    private void tryAssert( AssertFunction func, int retries, int sleepMillis ) throws Exception
    {
        Throwable t = null;
        int retry = retries;
        while ( retry-- > 0 )
        {
            try
            {
                func.accept( );
                return;
            }
            catch ( Exception | AssertionError e )
            {
                t = e;
                Thread.currentThread( ).sleep( sleepMillis );
                log.warn( "Retrying assert {}: {}", retry, e.getMessage( ) );
            }
        }
        log.warn( "Retries: {}, Exception: {}", retry, t.getMessage( ) );
        if ( retry <= 0 && t != null )
        {
            if ( t instanceof RuntimeException )
            {
                throw (RuntimeException) t;
            }
            else if ( t instanceof Exception )
            {
                throw (Exception) t;
            }
            else if ( t instanceof Error )
            {
                throw (Error) t;
            }
        }
    }

    private static void registerMixinNodeType( NodeTypeManager nodeTypeManager, String type )
        throws RepositoryException
    {
        NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
        nodeType.setMixin( true );
        nodeType.setName( type );
        nodeTypeManager.registerNodeType( nodeType, false );
    }

    @After
    public void tearDown()
        throws Exception
    {
        if ( repository != null )
        {
            try
            {
                repository.close( );
            } catch (Throwable e) {
                //
            }
        }
        if (sessionFactory!=null) {
            try
            {
                sessionFactory.close( );
            } catch (Throwable e) {
                //
            }
        }
        super.tearDown();

    }

    @Test
    public void testJcrStatisticsQuery()
        throws Exception
    {
        try(RepositorySession repSession = sessionFactory.createSession()) {
            Calendar cal = Calendar.getInstance();
            Date endTime = cal.getTime();
            cal.add(Calendar.HOUR, -1);
            Date startTime = cal.getTime();

            loadContentIntoRepo(repSession, TEST_REPO);
            loadContentIntoRepo( repSession, "another-repo");

            DefaultRepositoryStatistics testedStatistics = new DefaultRepositoryStatistics();
            testedStatistics.setNewFileCount(NEW_FILE_COUNT);
            testedStatistics.setTotalFileCount(TOTAL_FILE_COUNT);
            testedStatistics.setScanStartTime(startTime);
            testedStatistics.setScanEndTime(endTime);


            DefaultRepositoryStatistics expectedStatistics = new DefaultRepositoryStatistics();
            expectedStatistics.setNewFileCount(NEW_FILE_COUNT);
            expectedStatistics.setTotalFileCount(TOTAL_FILE_COUNT);
            expectedStatistics.setScanEndTime(endTime);
            expectedStatistics.setScanStartTime(startTime);
            expectedStatistics.setTotalArtifactFileSize(95954585);
            expectedStatistics.setTotalArtifactCount(269);
            expectedStatistics.setTotalGroupCount(1);
            expectedStatistics.setTotalProjectCount(43);
            expectedStatistics.setTotalCountForType("zip", 1);
            expectedStatistics.setTotalCountForType("gz", 1); // FIXME: should be tar.gz
            expectedStatistics.setTotalCountForType("java-source", 10);
            expectedStatistics.setTotalCountForType("jar", 108);
            expectedStatistics.setTotalCountForType("xml", 3);
            expectedStatistics.setTotalCountForType("war", 2);
            expectedStatistics.setTotalCountForType("pom", 144);
            expectedStatistics.setRepositoryId(TEST_REPO);

            tryAssert( () -> {
                repository.populateStatistics(repSession, repository, TEST_REPO, testedStatistics);

                logger.info("getTotalCountForType: {}", testedStatistics.getTotalCountForType());

            assertEquals(NEW_FILE_COUNT, testedStatistics.getNewFileCount());
            assertEquals(TOTAL_FILE_COUNT, testedStatistics.getTotalFileCount());
            assertEquals(endTime, testedStatistics.getScanEndTime());
            assertEquals(startTime, testedStatistics.getScanStartTime());
            assertEquals(269, testedStatistics.getTotalArtifactCount());
            assertEquals(1, testedStatistics.getTotalGroupCount());
            assertEquals(43, testedStatistics.getTotalProjectCount());
            assertEquals(1, testedStatistics.getTotalCountForType("zip"));
            assertEquals(1, testedStatistics.getTotalCountForType("gz"));
            assertEquals(10, testedStatistics.getTotalCountForType("java-source"));
            assertEquals(108, testedStatistics.getTotalCountForType("jar"));
            assertEquals(3, testedStatistics.getTotalCountForType("xml"));
            assertEquals(2, testedStatistics.getTotalCountForType("war"));
            assertEquals(144, testedStatistics.getTotalCountForType("pom"));
            assertEquals(10, testedStatistics.getTotalCountForType("java-source"));
            assertEquals(95954585, testedStatistics.getTotalArtifactFileSize());
        });

        }
    }

    private void loadContentIntoRepo( RepositorySession repoSession, String repoId )
        throws RepositoryException, IOException, MetadataRepositoryException
    {
            jcrSession = ((JcrRepositorySession) repoSession).getJcrSession();
        Node n = JcrUtils.getOrAddNode( jcrSession.getRootNode( ), "repositories" );
        n = JcrUtils.getOrAddNode( n, repoId );
        n = JcrUtils.getOrAddNode( n, "content" );
        n = JcrUtils.getOrAddNode( n, "org" );
        n = JcrUtils.getOrAddNode( n, "apache" );

        InputStream inputStream = getClass( ).getResourceAsStream( "/artifacts.xml" );
        jcrSession.importXML( n.getPath( ), inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW );
        jcrSession.save( );
    }
}
