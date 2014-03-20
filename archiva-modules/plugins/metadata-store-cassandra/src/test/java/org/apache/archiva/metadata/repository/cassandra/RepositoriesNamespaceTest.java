package org.apache.archiva.metadata.repository.cassandra;

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

import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.repository.cassandra.model.Namespace;
import org.apache.archiva.metadata.repository.cassandra.model.Repository;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RepositoriesNamespaceTest
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "archivaEntityManagerFactory#cassandra" )
    CassandraArchivaManager cassandraArchivaManager;


    CassandraMetadataRepository cmr;

    @Before
    public void setup()
        throws Exception
    {
        cmr = new CassandraMetadataRepository( null, null, cassandraArchivaManager );
        if ( !cassandraArchivaManager.started() )
        {
            cassandraArchivaManager.start();
        }
        clearReposAndNamespace();
    }

    @After
    public void shutdown()
        throws Exception
    {
        clearReposAndNamespace();
        cassandraArchivaManager.shutdown();
    }


    @Test
    public void testMetadataRepo()
        throws Exception
    {

        Repository r = null;
        Namespace n = null;

        try
        {

            cmr.updateNamespace( "release", "org" );

            r = cmr.getRepository( "release" );

            Assertions.assertThat( r ).isNotNull();

            Assertions.assertThat( cmr.getRepositories() ).isNotEmpty().hasSize( 1 );
            Assertions.assertThat( cmr.getNamespaces( "release" ) ).isNotEmpty().hasSize( 1 );

            n = cmr.getNamespace( "release", "org" );

            Assertions.assertThat( n ).isNotNull();
            Assertions.assertThat( n.getRepository() ).isNotNull();

            cmr.updateNamespace( "release", "org.apache" );

            r = cmr.getRepository( "release" );

            Assertions.assertThat( r ).isNotNull();
            Assertions.assertThat( cmr.getNamespaces( "release" ) ).isNotEmpty().hasSize( 2 );

            cmr.removeNamespace( "release", "org.apache" );
            Assertions.assertThat( cmr.getNamespaces( "release" ) ).isNotEmpty().hasSize( 1 );
            Assertions.assertThat( cmr.getNamespaces( "release" ) ).containsExactly( "org" );

            ProjectMetadata projectMetadata = new ProjectMetadata();
            projectMetadata.setId( "theproject" );
            projectMetadata.setNamespace( "org" );

            cmr.updateProject( "release", projectMetadata );

            Assertions.assertThat( cmr.getProjects( "release", "org" ) ).isNotEmpty().hasSize( 1 ).containsExactly(
                "theproject" );

            cmr.removeProject( "release", "org", "theproject" );

            Assertions.assertThat( cmr.getProjects( "release", "org" ) ).isEmpty();

            cmr.removeRepository( "release" );

            r = cmr.getRepository( "release" );

            Assertions.assertThat( r ).isNull();

        }
        catch ( Exception e )
        {
            logger.error( e.getMessage(), e );
            throw e;
        }
        finally
        {
            clearReposAndNamespace();
        }
    }

    protected void clearReposAndNamespace()
        throws Exception
    {
        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       "project" );
        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       "namespace" );
        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       "repository" );
    }
}
