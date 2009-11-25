package org.apache.archiva.metadata.repository.storage.maven2;

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

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class Maven2RepositoryMetadataResolverTest
    extends PlexusInSpringTestCase
{
    private Maven2RepositoryMetadataResolver resolver;

    private static final String TEST_REPO_ID = "test";

    public void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaConfiguration configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( getTestPath( "src/test/repositories/test" ) );
        c.addManagedRepository( testRepo );
        configuration.save( c );

        resolver = (Maven2RepositoryMetadataResolver) lookup( MetadataResolver.class, "maven2" );
    }

    public void testGetProjectVersionMetadata()
    {
        ProjectVersionMetadata metadata =
            resolver.getProjectVersion( TEST_REPO_ID, "org.apache.archiva", "archiva-common", "1.2.1" );
        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
        assertEquals( "jar", facet.getPackaging() );
        assertEquals( "http://archiva.apache.org/ref/1.2.1/archiva-base/archiva-common", metadata.getUrl() );
        // TODO: more testing
    }

//    public void testGetProjectVersionMetadataForTimestampedSnapshot()
//    {
//        ProjectVersionMetadata metadata =
//            resolver.getProjectVersion( TEST_REPO_ID, "org.apache", "apache", "5-SNAPSHOT" );
//        MavenProjectFacet facet = (MavenProjectFacet) metadata.getFacet( MavenProjectFacet.FACET_ID );
//        assertEquals( "jar", facet.getPackaging() );
//        assertEquals( "http://www.apache.org/", metadata.getUrl() );
//        // TODO: more testing
//    }
}
