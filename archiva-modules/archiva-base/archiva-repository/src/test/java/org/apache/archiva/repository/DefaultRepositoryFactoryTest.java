package org.apache.archiva.repository;

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

import java.io.File;
import org.apache.archiva.repository.api.Repository;
import org.apache.archiva.repository.api.RepositoryFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class DefaultRepositoryFactoryTest extends PlexusInSpringTestCase
{
    private static final String NEW_REPOSITORY_ID = "new-id";
    private static final String NEW_REPOSITORY_NAME = "New Repository";
    protected static final String REPOID_INTERNAL = "internal";

    private ArchivaConfiguration archivaConfiguration;
    private File repoRootInternal;
    private RepositoryFactory repositoryFactory;

    public void testGetRepository()
        throws Exception
    {
        assertRepositoryValid( repositoryFactory, REPOID_INTERNAL );
    }

    public void testGetRepositoryAfterDelete()
        throws Exception
    {
        assertNotNull( repositoryFactory );

        Configuration c = archivaConfiguration.getConfiguration();
        c.removeManagedRepository( c.findManagedRepositoryById( REPOID_INTERNAL ) );
        saveConfiguration( archivaConfiguration );

        Repository repository = repositoryFactory.getRepositories().get(REPOID_INTERNAL);
        assertNull( repository );
    }

    public void testGetRepositoryAfterAdd()
        throws Exception
    {
        assertNotNull( repositoryFactory );

        Configuration c = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( NEW_REPOSITORY_ID );
        repo.setName( NEW_REPOSITORY_NAME );
        File repoRoot = new File( getBasedir(), "target/test-repository-root" );
        if ( !repoRoot.exists() )
        {
            repoRoot.mkdirs();
        }
        repo.setLocation( repoRoot.getAbsolutePath() );
        c.addManagedRepository( repo );
        saveConfiguration( archivaConfiguration );

        Repository repository = repositoryFactory.getRepositories().get(NEW_REPOSITORY_ID);
        assertNotNull( repository );
        assertEquals( NEW_REPOSITORY_NAME, repository.getName() );

        // check other is still intact
        assertRepositoryValid( repositoryFactory, REPOID_INTERNAL );
    }

    @Override
    protected String getPlexusConfigLocation()
    {
        return "org/apache/archiva/repository/DefaultRepositoryFactoryTest.xml";
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
        throws Exception
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );
    }

    protected void assertRepositoryValid( RepositoryFactory repositoryFactory, String repoId )
    {
        Repository repository = repositoryFactory.getRepositories().get(repoId);
        assertNotNull( "Archiva Managed Repository id:<" + repoId + "> should exist.", repository );
        assertTrue( "Archiva Managed Repository id:<" + repoId + "> should have a valid location on disk.", repository.getLocalPath().exists()
            && repository.getLocalPath().isDirectory() );
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );

        File testConf = getTestFile( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        FileUtils.copyFile( testConf, testConfDest );

        archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        repoRootInternal = new File( appserverBase, "data/repositories/internal" );
        Configuration config = archivaConfiguration.getConfiguration();

        config.addManagedRepository( createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal ) );
        saveConfiguration( archivaConfiguration );

        repositoryFactory = (RepositoryFactory)lookup(RepositoryFactory.class);
    }
}
