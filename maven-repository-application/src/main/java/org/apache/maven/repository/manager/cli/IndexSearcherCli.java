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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.apache.maven.repository.indexing.DefaultRepositoryIndexSearcher;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Entry point for indexing CLI.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class IndexSearcherCli
{
    public static void main( String[] args )
        throws PlexusContainerException, ComponentLookupException, RepositoryIndexException, MalformedURLException,
        RepositoryIndexSearchException
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

        ArtifactRepositoryIndex index =
            indexFactory.createArtifactRepositoryIndex( new File( args[0], ".index" ).getAbsolutePath(), repository );
              
        DefaultRepositoryIndexSearcher searcher = indexFactory.createDefaultRepositoryIndexSearcher( index );

        try
        {
            System.out.println( searcher.search( new SinglePhraseQuery( args[1], args[2] ) ) );
        }
        finally
        {
            index.close();
        }
    }

}
