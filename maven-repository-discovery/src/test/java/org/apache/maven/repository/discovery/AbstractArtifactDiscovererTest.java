package org.apache.maven.repository.discovery;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.PlexusTestCase;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.Artifact;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractArtifactDiscovererTest
    extends PlexusTestCase
{
    protected ArtifactDiscoverer discoverer;

    private ArtifactFactory factory;

    protected ArtifactRepository repository;

    protected abstract String getLayout();

    protected abstract File getRepositoryFile();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (ArtifactDiscoverer) lookup( ArtifactDiscoverer.ROLE, getLayout() );

        factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        repository = getRepository();
    }

    protected ArtifactRepository getRepository()
        throws Exception
    {
        File basedir = getRepositoryFile();

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, getLayout() );

        return factory.createArtifactRepository( "discoveryRepo", "file://" + basedir, layout, null, null );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return factory.createArtifact( groupId, artifactId, version, null, "jar" );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        return factory.createArtifact( groupId, artifactId, version, null, type );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type, String classifier )
    {
        return factory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }
}
