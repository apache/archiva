package org.apache.maven.archiva.database;

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

import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.codehaus.plexus.spring.PlexusToSpringUtils;

public class RepositoryDatabaseEventListenerTest
    extends AbstractArchivaDatabaseTestCase
{
    private RepositoryListener listener;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        listener = (RepositoryListener) lookup( RepositoryListener.class.getName(), "database" );
    }

    @SuppressWarnings("unchecked")
    public void testWiring()
    {
        List<RepositoryListener> listeners =
            PlexusToSpringUtils.lookupList( PlexusToSpringUtils.buildSpringId( RepositoryListener.class ),
                                            getApplicationContext() );

        assertEquals( 1, listeners.size() );
        assertEquals( listener, listeners.get( 0 ) );
    }

    public ArchivaArtifact createArtifact( String artifactId, String version, ArtifactDAO artifactDao, String type )
    {
        ArchivaArtifact artifact =
            artifactDao.createArtifact( "org.apache.maven.archiva.test", artifactId, version, "", type );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testDeleteArtifact()
        throws Exception
    {
        ArtifactDAO artifactDao = (ArtifactDAO) lookup( ArtifactDAO.class.getName(), "jdo" );

        ArchivaArtifact pomArtifact = createPom( artifactDao );
        ArchivaArtifact jarArtifact = createJar( artifactDao );

        assertEquals( pomArtifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0",
                                                            null, "pom" ) );
        assertEquals( jarArtifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0",
                                                            null, "jar" ) );

        jarArtifact = new ArchivaArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null, "jar" );
        ManagedRepositoryContent repository =
            (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class.getName(), "default" );
        listener.deleteArtifact( repository, jarArtifact );

        try
        {
            artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null, "jar" );
            fail( "Should not find artifact" );
        }
        catch ( ObjectNotFoundException e )
        {
            assertTrue( true );
        }

        assertEquals( pomArtifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0",
                                                            null, "pom" ) );
    }

    private ArchivaArtifact createJar( ArtifactDAO artifactDao )
        throws ArchivaDatabaseException
    {
        ArchivaArtifact artifact = createArtifact( "test-artifact", "1.0", artifactDao, "jar" );
        artifactDao.saveArtifact( artifact );
        return artifact;
    }

    public void testDeletePomArtifact()
        throws Exception
    {
        ArtifactDAO artifactDao = (ArtifactDAO) lookup( ArtifactDAO.class.getName(), "jdo" );

        ArchivaArtifact pomArtifact = createPom( artifactDao );
        ArchivaArtifact jarArtifact = createJar( artifactDao );

        assertEquals( pomArtifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0",
                                                            null, "pom" ) );
        assertEquals( jarArtifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0",
                                                            null, "jar" ) );

        pomArtifact = new ArchivaArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null, "pom" );
        ManagedRepositoryContent repository =
            (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class.getName(), "default" );
        listener.deleteArtifact( repository, pomArtifact );

        try
        {
            artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null, "pom" );
            fail( "Should not find artifact" );
        }
        catch ( ObjectNotFoundException e )
        {
            assertTrue( true );
        }

        assertEquals( jarArtifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0",
                                                            null, "jar" ) );
    }

    private ArchivaArtifact createPom( ArtifactDAO artifactDao )
        throws ArchivaDatabaseException
    {
        ArchivaArtifact artifact = createArtifact( "test-artifact", "1.0", artifactDao, "pom" );
        artifactDao.saveArtifact( artifact );
        return artifact;
    }
}
