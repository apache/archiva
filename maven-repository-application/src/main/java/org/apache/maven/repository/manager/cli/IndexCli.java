package org.apache.maven.repository.manager.cli;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

/**
 * Entry point for indexing CLI.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class IndexCli
{
    private IndexCli()
    {
    }

    public static void main( String[] args )
        throws PlexusContainerException, ComponentLookupException, RepositoryIndexException, MalformedURLException
    {
        Embedder embedder = new Embedder();
        embedder.start( new ClassWorld() );

        RepositoryIndexingFactory indexFactory =
            (RepositoryIndexingFactory) embedder.lookup( RepositoryIndexingFactory.ROLE );

        ArtifactRepositoryFactory factory =
            (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout =
            (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        ArtifactRepository repository = factory.createArtifactRepository( "repository",
                                                                          new File( args[0] ).toURL().toString(),
                                                                          layout, null, null );

        ArtifactDiscoverer discoverer = (ArtifactDiscoverer) embedder.lookup( ArtifactDiscoverer.ROLE, "legacy" );

        List artifacts = discoverer.discoverArtifacts( repository, null, false );

        ArtifactRepositoryIndex index =
            indexFactory.createArtifactRepositoryIndex( new File( args[0], ".index" ), repository );

        long time = System.currentTimeMillis();
        try
        {
            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                index.indexArtifact( artifact );
            }
            index.optimize();
        }
        finally
        {
            index.close();
        }
        time = System.currentTimeMillis() - time;

        System.out.println( "Indexed " + artifacts.size() + " artifacts in " + time + "ms" );
    }

}
