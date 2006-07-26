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
import org.apache.lucene.index.Term;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class indexes the metadata in the repository.
 */
public class MetadataRepositoryIndex
    extends AbstractRepositoryIndex
{
    /**
     * Class Constructor
     *
     * @param indexPath  the path to the index
     * @param repository the repository where the metadata to be indexed is located
     */
    public MetadataRepositoryIndex( File indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        super( indexPath, repository );
    }

    /**
     * Index the contents of the specified RepositoryMetadata parameter object
     *
     * @param repoMetadata the metadata object to be indexed
     * @throws RepositoryIndexException
     */
    public void indexMetadata( RepositoryMetadata repoMetadata )
        throws RepositoryIndexException
    {
        indexMetadata( Collections.singletonList( repoMetadata ) );
    }

    /**
     * Index the metadata found within the provided list.  Deletes existing entries in the index first before
     * proceeding with the index additions.
     *
     * @param metadataList
     * @throws RepositoryIndexException
     */
    public void indexMetadata( List metadataList )
        throws RepositoryIndexException
    {
        try
        {
            deleteDocuments( getTermList( metadataList ) );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Failed to delete an index document", e );
        }

        addDocuments( getDocumentList( metadataList ) );
    }

    /**
     * Creates a list of Lucene Term object used in index deletion
     *
     * @param metadataList
     * @return List of Term object
     */
    private List getTermList( List metadataList )
    {
        List terms = new ArrayList();

        for ( Iterator metadata = metadataList.iterator(); metadata.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) metadata.next();

            terms.add( new Term( FLD_ID, (String) repoMetadata.getKey() ) );
        }

        return terms;
    }

    /**
     * Creates a list of Lucene documents
     *
     * @param metadataList
     * @return List of Lucene Documents
     */
    private List getDocumentList( List metadataList )
    {
        List docs = new ArrayList();

        for ( Iterator metadata = metadataList.iterator(); metadata.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) metadata.next();

            docs.add( createDocument( repoMetadata ) );
        }

        return docs;
    }

    /**
     * Creates a Lucene Document from a RepositoryMetadata; used for index additions
     *
     * @param repoMetadata
     * @return Lucene Document
     */
    private Document createDocument( RepositoryMetadata repoMetadata )
    {
        //get lastUpdated from Versioning (specified in Metadata object)
        //get pluginPrefixes from Plugin (spcified in Metadata object) -----> concatenate/append???
        //get the metadatapath: check where metadata is located, then concatenate the groupId,
        // artifactId, version based on its location
        Document doc = new Document();
        doc.add( createKeywordField( FLD_ID, (String) repoMetadata.getKey() ) );
        Metadata metadata = repoMetadata.getMetadata();

        doc.add( createTextField( FLD_NAME, repository.pathOfRemoteRepositoryMetadata( repoMetadata ) ) );

        Versioning versioning = metadata.getVersioning();
        if ( versioning != null )
        {
            doc.add( createTextField( FLD_LASTUPDATE, versioning.getLastUpdated() ) );
        }
        else
        {
            doc.add( createTextField( FLD_LASTUPDATE, "" ) );
        }

        List plugins = metadata.getPlugins();
        String pluginAppended = "";
        for ( Iterator iter = plugins.iterator(); iter.hasNext(); )
        {
            Plugin plugin = (Plugin) iter.next();
            if ( plugin.getPrefix() != null && !"".equals( plugin.getPrefix() ) )
            {
                pluginAppended = plugin.getPrefix() + "\n";
            }
        }
        doc.add( createTextField( FLD_PLUGINPREFIX, pluginAppended ) );

        if ( metadata.getGroupId() != null )
        {
            doc.add( createTextField( FLD_GROUPID, metadata.getGroupId() ) );
        }
        else
        {
            doc.add( createTextField( FLD_GROUPID, "" ) );
        }

        if ( metadata.getArtifactId() != null )
        {
            doc.add( createTextField( FLD_ARTIFACTID, metadata.getArtifactId() ) );
        }
        else
        {
            doc.add( createTextField( FLD_ARTIFACTID, "" ) );
        }

        if ( metadata.getVersion() != null )
        {
            doc.add( createTextField( FLD_VERSION, metadata.getVersion() ) );
        }
        else
        {
            doc.add( createTextField( FLD_VERSION, "" ) );
        }
        // TODO: do we need to add all these empty fields?
        doc.add( createTextField( FLD_DOCTYPE, METADATA ) );
        doc.add( createKeywordField( FLD_PACKAGING, "" ) );
        doc.add( createTextField( FLD_SHA1, "" ) );
        doc.add( createTextField( FLD_MD5, "" ) );
        doc.add( createTextField( FLD_CLASSES, "" ) );
        doc.add( createTextField( FLD_PACKAGES, "" ) );
        doc.add( createTextField( FLD_FILES, "" ) );
        doc.add( createKeywordField( FLD_LICENSE_URLS, "" ) );
        doc.add( createKeywordField( FLD_DEPENDENCIES, "" ) );
        doc.add( createKeywordField( FLD_PLUGINS_BUILD, "" ) );
        doc.add( createKeywordField( FLD_PLUGINS_REPORT, "" ) );
        doc.add( createKeywordField( FLD_PLUGINS_ALL, "" ) );
        return doc;
    }

    private static Field createTextField( String name, String value )
    {
        return new Field( name, value, Field.Store.YES, Field.Index.TOKENIZED );
    }

    private static Field createKeywordField( String name, String value )
    {
        return new Field( name, value, Field.Store.YES, Field.Index.UN_TOKENIZED );
    }
}
