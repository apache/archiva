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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
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

    public void testWiring()
    {
        List<RepositoryListener> listeners =
            PlexusToSpringUtils.lookupList( PlexusToSpringUtils.buildSpringId( RepositoryListener.class ),
                                            getApplicationContext() );

        assertEquals( 1, listeners.size() );
        assertEquals( listener, listeners.get( 0 ) );
    }

    public ArchivaArtifact createArtifact( String artifactId, String version, ArtifactDAO artifactDao )
    {
        ArchivaArtifact artifact =
            artifactDao.createArtifact( "org.apache.maven.archiva.test", artifactId, version, "", "jar", "testable_repo" );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testDeleteArtifact()
        throws Exception
    {
        ArtifactDAO artifactDao = (ArtifactDAO) lookup( ArtifactDAO.class.getName(), "jdo" );

        // Setup artifacts in fresh DB.
        ArchivaArtifact artifact = createArtifact( "test-artifact", "1.0", artifactDao );
        artifactDao.saveArtifact( artifact );

        assertEquals( artifact, artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null,
                                                         "jar", "testable_repo" ) );

        artifact = new ArchivaArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null, "jar", "testable_repo" );
        ManagedRepositoryContent repository =
            (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class.getName(), "default" );
        ManagedRepositoryConfiguration configuration = new ManagedRepositoryConfiguration();
        configuration.setId("testable_repo");
        repository.setRepository(configuration);
        
        listener.deleteArtifact( repository, artifact );

        try
        {
            artifactDao.getArtifact( "org.apache.maven.archiva.test", "test-artifact", "1.0", null, "jar", "testable_repo" );
            fail( "Should not find artifact" );
        }
        catch ( ObjectNotFoundException e )
        {
            assertTrue( true );
        }
    }
}
