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
import org.apache.lucene.document.Field;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * This class indexes the metadata in the repository.
 */
public class MetadataRepositoryIndex
    extends AbstractRepositoryIndex
{
    protected static final String GROUP_METADATA = "GROUP_METADATA";

    protected static final String ARTIFACT_METADATA = "ARTIFACT_METADATA";

    protected static final String SNAPSHOT_METADATA = "SNAPSHOT_METADATA";

    /**
     * Class Constructor
     *
     * @param indexPath  the path to the index
     * @param repository the repository where the metadata to be indexed is located
     * @throws RepositoryIndexException
     */
    public MetadataRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        super( indexPath, repository );
    }

    /**
     * Index the paramater object
     *
     * @param obj
     * @throws RepositoryIndexException
     */
    public void index( Object obj )
        throws RepositoryIndexException
    {
        if ( obj instanceof RepositoryMetadata )
        {
            indexMetadata( (RepositoryMetadata) obj );
        }
        else
        {
            throw new RepositoryIndexException(
                "This instance of indexer cannot index instances of " + obj.getClass().getName() );
        }
    }

    /**
     * Index the contents of the specified RepositoryMetadata paramter object
     *
     * @param repoMetadata the metadata object to be indexed
     * @throws RepositoryIndexException
     */
    private void indexMetadata( RepositoryMetadata repoMetadata )
        throws RepositoryIndexException
    {
        //get lastUpdated from Versioning (specified in Metadata object)
        //get pluginPrefixes from Plugin (spcified in Metadata object) -----> concatenate/append???
        //get the metadatapath: check where metadata is located, then concatenate the groupId,
        // artifactId, version based on its location
        Document doc = new Document();
        doc.add( Field.Keyword( FLD_ID, (String) repoMetadata.getKey() ) );
        String path = "";
        Metadata metadata = repoMetadata.getMetadata();

        if ( repoMetadata.storedInGroupDirectory() && !repoMetadata.storedInArtifactVersionDirectory() )
        {
            path = repoMetadata.getGroupId() + "/";
        }
        else if ( !repoMetadata.storedInGroupDirectory() && !repoMetadata.storedInArtifactVersionDirectory() )
        {
            path = repoMetadata.getGroupId() + "/" + repoMetadata.getArtifactId() + "/";
        }
        else if ( !repoMetadata.storedInGroupDirectory() && repoMetadata.storedInArtifactVersionDirectory() )
        {
            path = repoMetadata.getGroupId() + "/" + repoMetadata.getArtifactId() + "/" +
                repoMetadata.getBaseVersion() + "/";
        }

        if ( !repoMetadata.getRemoteFilename().equals( "" ) && repoMetadata.getRemoteFilename() != null )
        {
            path = path + repoMetadata.getRemoteFilename();
        }
        else
        {
            path = path + repoMetadata.getLocalFilename( repository );
        }
        doc.add( Field.Text( FLD_NAME, path ) );

        Versioning versioning = metadata.getVersioning();
        if ( versioning != null )
        {
            doc.add( Field.Text( FLD_LASTUPDATE, versioning.getLastUpdated() ) );
        }
        else
        {
            doc.add( Field.Text( FLD_LASTUPDATE, "" ) );
        }

        List plugins = metadata.getPlugins();
        String pluginAppended = "";
        for ( Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            Plugin plugin = (Plugin) iter.next();
            if ( plugin.getPrefix() != null && !plugin.getPrefix().equals( "" ) )
            {
                pluginAppended = plugin.getPrefix() + "\n";
            }
        }
        doc.add( Field.Text( FLD_PLUGINPREFIX, pluginAppended ) );
        doc.add( Field.Text( FLD_GROUPID, metadata.getGroupId() ) );

        if ( metadata.getArtifactId() != null && !metadata.getArtifactId().equals( "" ) )
        {
            doc.add( Field.Text( FLD_ARTIFACTID, metadata.getArtifactId() ) );
        }
        else
        {
            doc.add( Field.Text( FLD_ARTIFACTID, "" ) );
        }

        if ( metadata.getVersion() != null && !metadata.getVersion().equals( "" ) )
        {
            doc.add( Field.Text( FLD_VERSION, metadata.getVersion() ) );
        }
        else
        {
            doc.add( Field.Text( FLD_VERSION, "" ) );
        }
        doc.add( Field.Text( FLD_DOCTYPE, METADATA ) );
        doc.add( Field.Keyword( FLD_PACKAGING, "" ) );
        doc.add( Field.Text( FLD_SHA1, "" ) );
        doc.add( Field.Text( FLD_MD5, "" ) );
        doc.add( Field.Text( FLD_CLASSES, "" ) );
        doc.add( Field.Text( FLD_PACKAGES, "" ) );
        doc.add( Field.Text( FLD_FILES, "" ) );
        doc.add( Field.Keyword( FLD_LICENSE_URLS, "" ) );
        doc.add( Field.Keyword( FLD_DEPENDENCIES, "" ) );
        doc.add( Field.Keyword( FLD_PLUGINS_BUILD, "" ) );
        doc.add( Field.Keyword( FLD_PLUGINS_REPORT, "" ) );
        doc.add( Field.Keyword( FLD_PLUGINS_ALL, "" ) );

        try
        {
            deleteIfIndexed( repoMetadata );
            if ( !isOpen() )
            {
                open();
            }
            getIndexWriter().addDocument( doc );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error opening index", e );
        }
    }

    /**
     * @see org.apache.maven.repository.indexing.AbstractRepositoryIndex#deleteIfIndexed(Object)
     */
    public void deleteIfIndexed( Object object )
        throws RepositoryIndexException, IOException
    {
        if ( object instanceof RepositoryMetadata )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) object;
            if ( indexExists() )
            {
                validateIndex( FIELDS );
                deleteDocument( FLD_ID, (String) repoMetadata.getKey() );
            }
        }
        else
        {
            throw new RepositoryIndexException( "Object is not of type metadata." );
        }
    }
}
