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

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import static org.mockito.Mockito.*;

public class JcrRepositoryStatisticsGatheringTest
    extends PlexusInSpringTestCase
{
    private static final int TOTAL_FILE_COUNT = 1000;

    private static final int NEW_FILE_COUNT = 500;

    private static final String TEST_REPO = "test-repo";

    private RepositoryStatisticsManager repositoryStatisticsManager;

    private MetadataRepository metadataRepository;

    private Session session;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        File confFile = getTestFile( "src/test/repository.xml" );
        File dir = getTestFile( "target/jcr" );
        FileUtils.deleteDirectory( dir );

        TransientRepository repository = new TransientRepository( confFile, dir );
        session = repository.login( new SimpleCredentials( "username", "password".toCharArray() ) );

        // TODO: perhaps have an archiva-jcr-utils module shared by these plugins that does this and can contain
        //      structure information
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry registry = workspace.getNamespaceRegistry();
        registry.registerNamespace( "archiva", "http://archiva.apache.org/jcr/" );

        NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
        registerMixinNodeType( nodeTypeManager, "archiva:namespace" );
        registerMixinNodeType( nodeTypeManager, "archiva:project" );
        registerMixinNodeType( nodeTypeManager, "archiva:projectVersion" );
        registerMixinNodeType( nodeTypeManager, "archiva:artifact" );
        registerMixinNodeType( nodeTypeManager, "archiva:facet" );

        metadataRepository = mock( MetadataRepository.class );
        when( metadataRepository.canObtainAccess( Session.class ) ).thenReturn( true );
        when( metadataRepository.obtainAccess( Session.class ) ).thenReturn( session );

        repositoryStatisticsManager = new DefaultRepositoryStatisticsManager();
    }

    private static void registerMixinNodeType( NodeTypeManager nodeTypeManager, String type )
        throws RepositoryException
    {
        NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
        nodeType.setMixin( true );
        nodeType.setName( type );
        nodeTypeManager.registerNodeType( nodeType, false );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        session.logout();

        super.tearDown();
    }

    public void testJcrStatisticsQuery()
        throws Exception
    {
        Calendar cal = Calendar.getInstance();
        Date endTime = cal.getTime();
        cal.add( Calendar.HOUR, -1 );
        Date startTime = cal.getTime();

        loadContentIntoRepo( TEST_REPO );
        loadContentIntoRepo( "another-repo" );

        repositoryStatisticsManager.addStatisticsAfterScan( metadataRepository, TEST_REPO, startTime, endTime,
                                                            TOTAL_FILE_COUNT, NEW_FILE_COUNT );

        RepositoryStatistics expectedStatistics = new RepositoryStatistics();
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

        verify( metadataRepository ).addMetadataFacet( TEST_REPO, expectedStatistics );
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
