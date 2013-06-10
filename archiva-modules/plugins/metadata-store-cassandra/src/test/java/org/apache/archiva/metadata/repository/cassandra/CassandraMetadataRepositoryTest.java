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

import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.archiva.metadata.repository.cassandra.model.ArtifactMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.MetadataFacetModel;
import org.apache.archiva.metadata.repository.cassandra.model.Namespace;
import org.apache.archiva.metadata.repository.cassandra.model.Project;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.Repository;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
public class CassandraMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "archivaEntityManagerFactory#cassandra" )
    CassandraEntityManagerFactory cassandraEntityManagerFactory;

    CassandraMetadataRepository cmr;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        File directory = new File( "target/test-repositories" );
        if ( directory.exists() )
        {
            FileUtils.deleteDirectory( directory );
        }

        Map<String, MetadataFacetFactory> factories = createTestMetadataFacetFactories();

        this.cmr = new CassandraMetadataRepository( factories, null, cassandraEntityManagerFactory.getKeyspace() );
        this.repository = this.cmr;

        clearReposAndNamespace();
    }


    @After
    public void shutdown()
        throws Exception
    {
        clearReposAndNamespace();
        super.tearDown();
    }

    protected void clearReposAndNamespace()
        throws Exception
    {
        List<Project> projects = cmr.getProjectEntityManager().getAll();

        cmr.getProjectEntityManager().remove( projects );

        List<Namespace> namespaces = cmr.getNamespaceEntityManager().getAll();

        cmr.getNamespaceEntityManager().remove( namespaces );

        List<Repository> repositories = cmr.getRepositoryEntityManager().getAll();

        cmr.getRepositoryEntityManager().remove( repositories );

        List<ArtifactMetadataModel> artifactMetadataModels = cmr.getArtifactMetadataModelEntityManager().getAll();
        cmr.getArtifactMetadataModelEntityManager().remove( artifactMetadataModels );

        List<MetadataFacetModel> metadataFacetModels = cmr.getMetadataFacetModelEntityManager().getAll();
        cmr.getMetadataFacetModelEntityManager().remove( metadataFacetModels );

        List<ProjectVersionMetadataModel> projectVersionMetadataModels =
            cmr.getProjectVersionMetadataModelEntityManager().getAll();
        cmr.getProjectVersionMetadataModelEntityManager().remove( projectVersionMetadataModels );


    }

}
