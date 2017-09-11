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
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.jcr.JcrMetadataRepository;
import org.apache.archiva.metadata.repository.jcr.RepositoryFactory;
import org.apache.archiva.metadata.repository.stats.model.DefaultRepositoryStatistics;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class JcrRepositoryStatisticsGatheringTest
    extends TestCase
{
    private static final int TOTAL_FILE_COUNT = 1000;

    private static final int NEW_FILE_COUNT = 500;

    private static final String TEST_REPO = "test-repo";

    JcrMetadataRepository repository;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private ApplicationContext applicationContext;

    Session session;

    private static Repository jcrRepository;

    Logger logger = LoggerFactory.getLogger( getClass() );

    @BeforeClass
    public static void setupSpec()
        throws IOException, InvalidFileStoreVersionException
    {
        Path directory = Paths.get( "target/test-repositories" );
        if ( Files.exists(directory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }
        RepositoryFactory factory = new RepositoryFactory();
        factory.setRepositoryPath( directory.toString() );
        factory.setStoreType( RepositoryFactory.StoreType.IN_MEMORY_TYPE );
        jcrRepository = factory.createRepository();
    }


    @Before
    public void setUp()
        throws Exception
    {

        Map<String, MetadataFacetFactory> factories = AbstractMetadataRepositoryTest.createTestMetadataFacetFactories();

        assertNotNull( jcrRepository );
        // TODO: probably don't need to use Spring for this
        JcrMetadataRepository jcrMetadataRepository = new JcrMetadataRepository( factories, jcrRepository );

        session = jcrMetadataRepository.getJcrSession();

        try
        {
            session = jcrMetadataRepository.getJcrSession();

            // set up namespaces, etc.
            JcrMetadataRepository.initialize( session );

            // removing content is faster than deleting and re-copying the files from target/jcr
            session.getRootNode().getNode( "repositories" ).remove();
        }
        catch ( RepositoryException e )
        {
            // ignore
        }

        this.repository = jcrMetadataRepository;
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
            repository.close();
        }

    }

    @Test
    public void testJcrStatisticsQuery()
        throws Exception
    {
        Calendar cal = Calendar.getInstance();
        Date endTime = cal.getTime();
        cal.add( Calendar.HOUR, -1 );
        Date startTime = cal.getTime();

        loadContentIntoRepo( TEST_REPO );
        loadContentIntoRepo( "another-repo" );

        DefaultRepositoryStatistics testedStatistics = new DefaultRepositoryStatistics();
        testedStatistics.setNewFileCount( NEW_FILE_COUNT );
        testedStatistics.setTotalFileCount( TOTAL_FILE_COUNT );
        testedStatistics.setScanStartTime( startTime );
        testedStatistics.setScanEndTime( endTime );

        repository.populateStatistics( repository, TEST_REPO, testedStatistics );

        DefaultRepositoryStatistics expectedStatistics = new DefaultRepositoryStatistics();
        expectedStatistics.setNewFileCount( NEW_FILE_COUNT );
        expectedStatistics.setTotalFileCount( TOTAL_FILE_COUNT );
        expectedStatistics.setScanEndTime( endTime );
        expectedStatistics.setScanStartTime( startTime );
        expectedStatistics.setTotalArtifactFileSize( 95954585 );
        expectedStatistics.setTotalArtifactCount( 269 );
        expectedStatistics.setTotalGroupCount( 1 );
        expectedStatistics.setTotalProjectCount( 43 );
        expectedStatistics.setTotalCountForType( "zip", 1 );
        expectedStatistics.setTotalCountForType( "gz", 1 ); // FIXME: should be tar.gz
        expectedStatistics.setTotalCountForType( "java-source", 10 );
        expectedStatistics.setTotalCountForType( "jar", 108 );
        expectedStatistics.setTotalCountForType( "xml", 3 );
        expectedStatistics.setTotalCountForType( "war", 2 );
        expectedStatistics.setTotalCountForType( "pom", 144 );
        expectedStatistics.setRepositoryId( TEST_REPO );

        logger.info("getTotalCountForType: {}", testedStatistics.getTotalCountForType() );

        assertEquals( NEW_FILE_COUNT, testedStatistics.getNewFileCount() );
        assertEquals( TOTAL_FILE_COUNT, testedStatistics.getTotalFileCount() );
        assertEquals( endTime, testedStatistics.getScanEndTime() );
        assertEquals( startTime, testedStatistics.getScanStartTime() );
        assertEquals( 95954585, testedStatistics.getTotalArtifactFileSize() );
        assertEquals( 269, testedStatistics.getTotalArtifactCount() );
        assertEquals( 1, testedStatistics.getTotalGroupCount() );
        assertEquals( 43, testedStatistics.getTotalProjectCount() );
        assertEquals( 1, testedStatistics.getTotalCountForType( "zip" ) );
        assertEquals( 1, testedStatistics.getTotalCountForType( "gz" ) );
        assertEquals( 10, testedStatistics.getTotalCountForType( "java-source" ) );
        assertEquals( 108, testedStatistics.getTotalCountForType( "jar" ) );
        assertEquals( 3, testedStatistics.getTotalCountForType( "xml" ) );
        assertEquals( 2, testedStatistics.getTotalCountForType( "war" ) );
        assertEquals( 144, testedStatistics.getTotalCountForType( "pom" ) );
        assertEquals( 10, testedStatistics.getTotalCountForType( "java-source" ) );


    }

    private void loadContentIntoRepo( String repoId )
        throws RepositoryException, IOException
    {
        Node n = JcrUtils.getOrAddNode( session.getRootNode(), "repositories" );
        n = JcrUtils.getOrAddNode( n, repoId );
        n = JcrUtils.getOrAddNode( n, "content" );
        n = JcrUtils.getOrAddNode( n, "org" );
        n = JcrUtils.getOrAddNode( n, "apache" );

        GZIPInputStream inputStream = new GZIPInputStream( getClass().getResourceAsStream( "/artifacts.xml.gz" ) );
        session.importXML( n.getPath(), inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW );
        session.save();
    }
}
