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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Map;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class JcrMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{
    private JcrMetadataRepository jcrMetadataRepository;

    @Inject
    private ApplicationContext applicationContext;

    @Before
    @Override
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

        // TODO: probably don't need to use Spring for this
        Repository repository = applicationContext.getBean( Repository.class );
        jcrMetadataRepository = new JcrMetadataRepository( factories, repository );

        try
        {
            Session session = jcrMetadataRepository.getJcrSession();

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


    @After
    @Override
    public void tearDown()
        throws Exception
    {
        jcrMetadataRepository.close();

        super.tearDown();
    }


}
