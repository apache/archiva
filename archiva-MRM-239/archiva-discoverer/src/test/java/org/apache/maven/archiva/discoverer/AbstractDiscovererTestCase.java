package org.apache.maven.archiva.discoverer;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 */
public abstract class AbstractDiscovererTestCase
    extends PlexusTestCase
{
    protected Discoverer discoverer;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (Discoverer) lookup( Discoverer.ROLE );
    }

    protected void tearDown()
        throws Exception
    {
        release( discoverer );
        super.tearDown();
    }

    protected ArtifactRepository getLegacyRepository()
        throws Exception
    {
        File repoBaseDir = new File( getBasedir(), "src/test/legacy-repository" );
        ArtifactRepository repository = createRepository( repoBaseDir, "legacy" );
        resetRepositoryState( repository );
        return repository;
    }

    protected ArtifactRepository getDefaultRepository()
        throws Exception
    {
        File repoBaseDir = new File( getBasedir(), "src/test/repository" );
        ArtifactRepository repository = createRepository( repoBaseDir, "default" );
        resetRepositoryState( repository );
        return repository;
    }

    protected void resetRepositoryState( ArtifactRepository repository )
    {
        // Implement any kind of repository cleanup.
    }

    protected ArtifactRepository createRepository( File basedir, String layout )
        throws Exception
    {
        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, layout );

        return factory.createArtifactRepository( "discoveryRepo-" + getName(), "file://" + basedir, repoLayout, null,
                                                 null );
    }
}
