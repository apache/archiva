package org.apache.archiva.metadata.repository.jcr;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.archiva.metadata.repository.DefaultMetadataResolver;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.MetadataSessionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Using static sessionFactory and repository, because initialization is expensive if we rebuild the whole repository for
 * each test.
 */
public class JcrMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{

    private static JcrRepositorySessionFactory sessionFactory;
    private static JcrMetadataRepository repository;

    @Override
    public JcrMetadataRepository getRepository( )
    {
        return repository;
    }

    @Override
    public JcrRepositorySessionFactory getSessionFactory( )
    {
        return sessionFactory;
    }

    @BeforeClass
    public static void setupSpec( ) throws IOException, InvalidFileStoreVersionException
    {
        Path directory = Paths.get( "target/test-repositories" );
        if ( Files.exists( directory ) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }

        List<MetadataFacetFactory> factories = createTestMetadataFacetFactories( );
        MetadataService metadataService = new MetadataService();
        metadataService.setMetadataFacetFactories( factories );
        JcrRepositorySessionFactory jcrSessionFactory = new JcrRepositorySessionFactory( );
        jcrSessionFactory.setMetadataResolver( new DefaultMetadataResolver( ) );
        jcrSessionFactory.setMetadataService( metadataService );

        jcrSessionFactory.open( );
        sessionFactory = jcrSessionFactory;
        repository = jcrSessionFactory.getMetadataRepository( );

    }

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        super.assertMaxTries=5;
        super.assertRetrySleepMs = 500;
        try( JcrRepositorySession session = (JcrRepositorySession) getSessionFactory().createSession() ) {
            Session jcrSession = session.getJcrSession( );
            if (jcrSession.itemExists( "/repositories/test" ))
            {
                jcrSession.removeItem( "/repositories/test" );
                session.save( );
            }
        }
    }

    @AfterClass
    public static void stopSpec( )
        throws Exception
    {
        if ( repository != null )
        {
            try
            {
                repository.close( );
            }
            catch ( Throwable e )
            {
                //
            }
        }
        if ( sessionFactory != null )
        {
            try
            {
                sessionFactory.close( );
            }
            catch ( Throwable e )
            {
                //
            }
        }
    }


    @Test
    public void testSearchArtifactsByKey( )
        throws Exception
    {
        try ( RepositorySession session = sessionFactory.createSession( ) )
        {
            createArtifactWithData( session );
        }


        tryAssert( ( ) -> {
            try ( RepositorySession session = sessionFactory.createSession( ) )
            {
                session.refreshAndDiscard( );
                Session jcrSession = ( (JcrRepositorySession) session ).getJcrSession( );
                assertThat(jcrSession.propertyExists( "/repositories/test/content/mytest/myproject/1.0/url" )).isTrue();

                Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( session, TEST_REPO_ID, "url", TEST_URL, false );
                assertThat( artifactsByProperty ).isNotNull( ).isNotEmpty( );
            }
        } );

    }


    @Test
    public void testSearchArtifactsByKeyExact()
        throws Exception {
        try (RepositorySession session = sessionFactory.createSession())
        {
            createArtifactWithData( session );
        }
        try (RepositorySession session = sessionFactory.createSession())
        {
            session.refreshAndDiscard();
            tryAssert(() -> {
                Session jcrSession = ( (JcrRepositorySession) session ).getJcrSession( );
                assertThat(jcrSession.propertyExists( "/repositories/test/content/mytest/myproject/1.0/url" )).isTrue();
                Collection<ArtifactMetadata> artifactsByProperty = repository.searchArtifacts( session, TEST_REPO_ID, "url", TEST_URL, true);
                assertThat(artifactsByProperty).describedAs( "Artifact search by url=%s must give a result.", TEST_URL ).isNotNull().isNotEmpty();
                artifactsByProperty = repository.searchArtifacts( session, TEST_REPO_ID, "org.name", "pache", true );
                assertThat( artifactsByProperty ).describedAs( "Artifact search by text org.name='pache' must be empty" ).isNotNull( ).isEmpty( );
            } );
        }
    }
}
