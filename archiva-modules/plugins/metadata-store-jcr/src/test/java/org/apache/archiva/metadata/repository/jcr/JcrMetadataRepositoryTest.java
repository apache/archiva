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

import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class JcrMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{
    private JcrMetadataRepository jcrMetadataRepository;

    @Inject
    private ApplicationContext applicationContext;

    private static Repository jcrRepository;

    @BeforeClass
    public static void setupSpec() throws IOException, InvalidFileStoreVersionException
    {
        Path directory = Paths.get( "target/test-repositories" );
        if (Files.exists(directory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }
        RepositoryFactory factory = new RepositoryFactory();
        factory.setRepositoryPath( directory.toString());
        jcrRepository = factory.createRepository();
    }

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();


        Map<String, MetadataFacetFactory> factories = createTestMetadataFacetFactories();

        // TODO: probably don't need to use Spring for this
        jcrMetadataRepository = new JcrMetadataRepository( factories, jcrRepository );

        try
        {
            Session session = jcrMetadataRepository.getJcrSession();

            // set up namespaces, etc.
            JcrMetadataRepository.initialize( session );

            // removing content is faster than deleting and re-copying the files from target/jcr
            session.getRootNode().getNode( "repositories" ).remove();
            session.save();
        }
        catch ( RepositoryException e )
        {
            // ignore
        }

        this.repository = jcrMetadataRepository;
    }


    @After
    @Override
    public void tearDown()
        throws Exception
    {
        jcrMetadataRepository.close();

        super.tearDown();
    }


}
