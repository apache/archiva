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

import org.apache.lucene.document.Document;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;

import java.io.File;

/**
 * This class searches the index for existing artifacts that contains the
 * specified query string.
 */
public class ArtifactRepositoryIndexSearcher
    extends AbstractRepositoryIndexSearcher
{
    private ArtifactFactory factory;

    /**
     * Constructor
     *
     * @param index   the index object
     * @param factory ArtifactFactory object
     */
    public ArtifactRepositoryIndexSearcher( ArtifactRepositoryIndex index, ArtifactFactory factory )
    {
        super( index );
        this.factory = factory;
    }

    /**
     * @see AbstractRepositoryIndexSearcher#createSearchedObjectFromIndexDocument(org.apache.lucene.document.Document)
     */
    protected Object createSearchedObjectFromIndexDocument( Document doc )
    {
        String groupId = doc.get( ArtifactRepositoryIndex.FLD_GROUPID );
        String artifactId = doc.get( ArtifactRepositoryIndex.FLD_ARTIFACTID );
        String version = doc.get( ArtifactRepositoryIndex.FLD_VERSION );
        String name = doc.get( ArtifactRepositoryIndex.FLD_NAME );
        String packaging = name.substring( name.lastIndexOf( '.' ) + 1 );
        Artifact artifact = factory.createBuildArtifact( groupId, artifactId, version, packaging );
        String groupIdTemp = groupId.replace( '.', '/' );
        artifact.setFile( new File(
            index.getRepository().getBasedir() + groupIdTemp + "/" + artifactId + "/" + version + "/" + name ) );

        return artifact;
    }
}
