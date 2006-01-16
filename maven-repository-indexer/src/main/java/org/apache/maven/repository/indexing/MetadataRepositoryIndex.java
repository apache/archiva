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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.Plugin;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * This class indexes the metadata in the repository.
 */
public class MetadataRepositoryIndex
        extends AbstractRepositoryIndex
{
    private static final String FLD_LASTUPDATE = "lastUpdate";

    private static final String FLD_PLUGINPREFIX = "pluginPrefix";

    private static final String FLD_METADATAPATH = "path";

    private static final String FLD_GROUPID = "groupId";

    private static final String FLD_ARTIFACTID = "artifactId";

    private static final String FLD_VERSION = "version";

    private static final String[] FIELDS = {FLD_METADATAPATH, FLD_PLUGINPREFIX, FLD_LASTUPDATE,
            FLD_GROUPID, FLD_ARTIFACTID, FLD_VERSION};

    /**
     * Constructor
     * @param indexPath the path to the index
     * @param repository the repository where the metadata to be indexed is located
     * @throws RepositoryIndexException
     */
    public MetadataRepositoryIndex( String indexPath, ArtifactRepository repository )
            throws RepositoryIndexException
    {
        super( indexPath, repository, FIELDS );
    }

    /**
     * Get the field names to be used in the index
     * @return array of strings
     */
    public String[] getIndexFields()
    {
        return FIELDS;
    }

    /**
     * Returns the analyzer used for indexing
     * @return Analyzer object
     */
    public Analyzer getAnalyzer()
    {
        return new StandardAnalyzer();
    }

    /**
     * Index the paramater object
     * @param obj
     * @throws RepositoryIndexException
     */
    public void index( Object obj ) throws RepositoryIndexException
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
     * @param repoMetadata the metadata object to be indexed
     * @throws RepositoryIndexException
     */
    private void indexMetadata( RepositoryMetadata repoMetadata ) throws RepositoryIndexException
    {
         if ( !isOpen() )
        {
            throw new RepositoryIndexException( "Unable to add artifact index on a closed index" );
        }

        //get lastUpdated from Versioning (specified in Metadata object)
        //get pluginPrefixes from Plugin (spcified in Metadata object) -----> concatenate/append???
        //get the metadatapath: check where metadata is located, then concatenate the groupId,
        // artifactId, version based on its location
        Document doc = new Document();
        String path = "";

        if( repoMetadata.storedInGroupDirectory() && !repoMetadata.storedInArtifactVersionDirectory())
        {
            path = repoMetadata.getGroupId() + "/";
        }
        else if(!repoMetadata.storedInGroupDirectory() && !repoMetadata.storedInArtifactVersionDirectory())
        {
           path = repoMetadata.getGroupId() + "/" + repoMetadata.getArtifactId() + "/";
        }
        else if(!repoMetadata.storedInGroupDirectory() && repoMetadata.storedInArtifactVersionDirectory())
        {
           path = repoMetadata.getGroupId() + "/" + repoMetadata.getArtifactId() + "/" + repoMetadata.getBaseVersion() + "/";
        }

        //@todo use localfilename or remotefilename to get the path???
        path = path + repoMetadata.getRemoteFilename();
        doc.add( Field.Text( FLD_METADATAPATH, path) );

        Metadata metadata = repoMetadata.getMetadata();
        Versioning versioning = metadata.getVersioning();
        if( versioning != null )
        {
            doc.add( Field.Text( FLD_LASTUPDATE, versioning.getLastUpdated() ) );
        }

        List plugins = metadata.getPlugins();
        String pluginAppended = "";
        for( Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            Plugin plugin = (Plugin) iter.next();
            if( plugin.getPrefix() != null && !plugin.getPrefix().equals("") )
            {
                pluginAppended = plugin.getPrefix() + " ";
            }
        }
        doc.add( Field.Text( FLD_PLUGINPREFIX, pluginAppended ) );
        doc.add( Field.UnIndexed( FLD_GROUPID, metadata.getGroupId() ) );

        if( metadata.getArtifactId() != null && !metadata.getArtifactId().equals("") )
        {
            doc.add( Field.UnIndexed( FLD_ARTIFACTID, metadata.getArtifactId() ) );
        }
        if( metadata.getVersion() != null && !metadata.getVersion().equals("") )
        {
            doc.add( Field.UnIndexed( FLD_VERSION, metadata.getVersion() ) );
        }

        try
        {
            getIndexWriter().addDocument( doc );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error opening index", e );
        }
    }

    public boolean isKeywordField( String field ){
           return false;
    }
}
