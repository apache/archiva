package org.apache.maven.repository.indexing;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.digest.Digester;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.indexing.RepositoryIndexingFactory"
 * @todo these methods should be replaced by plexus lookups of some kind!
 */
public class DefaultRepositoryIndexingFactory
    implements RepositoryIndexingFactory
{
    /**
     * @plexus.requirement
     */
    private Digester digester;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @see RepositoryIndexingFactory#createArtifactRepositoryIndex(String, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public ArtifactRepositoryIndex createArtifactRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        return new ArtifactRepositoryIndex( indexPath, repository, digester );
    }

    /**
     * @see RepositoryIndexingFactory#createPomRepositoryIndex(String, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public PomRepositoryIndex createPomRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        return new PomRepositoryIndex( indexPath, repository, digester, artifactFactory );
    }

    /**
     * @see RepositoryIndexingFactory#createMetadataRepositoryIndex(String, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public MetadataRepositoryIndex createMetadataRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        return new MetadataRepositoryIndex( indexPath, repository );
    }

    /*
     * @see RepositoryIndexingFactory#createRepositoryIndexSearchLayer(RepositoryIndex)
     */
    public RepositoryIndexSearchLayer createRepositoryIndexSearchLayer( RepositoryIndex index )
    {
        return new RepositoryIndexSearchLayer( index, artifactFactory );
    }

    /**
     * @see RepositoryIndexingFactory#createDefaultRepositoryIndexSearcher(RepositoryIndex)
     */
    public DefaultRepositoryIndexSearcher createDefaultRepositoryIndexSearcher( RepositoryIndex index )
    {
        return new DefaultRepositoryIndexSearcher( index, artifactFactory );
    }

}
