package org.apache.archiva.metadata.repository.file;

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
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import java.io.File;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{

    public void setUp()
        throws Exception
    {
        super.setUp();

        File directory = new File( "target/test-repositories" );
        if (directory.exists())
        {
            FileUtils.deleteDirectory( directory );
        }
        ArchivaConfiguration config = createTestConfiguration( directory );
        Map<String, MetadataFacetFactory> factories = createTestMetadataFacetFactories();

        this.repository = new FileMetadataRepository( factories, config );
    }

    protected static ArchivaConfiguration createTestConfiguration( File directory )
    {
        ArchivaConfiguration config = mock( ArchivaConfiguration.class );
        Configuration configData = new Configuration();
        configData.addManagedRepository( createManagedRepository( TEST_REPO_ID, directory ) );
        configData.addManagedRepository( createManagedRepository( "other-repo", directory ) );
        when( config.getConfiguration() ).thenReturn( configData );
        return config;
    }

    private static ManagedRepositoryConfiguration createManagedRepository( String repoId, File directory )
    {
        ManagedRepositoryConfiguration managedRepository = new ManagedRepositoryConfiguration();
        managedRepository.setId( repoId );
        managedRepository.setLocation( new File( directory, repoId ).getAbsolutePath() );
        return managedRepository;
    }
}
