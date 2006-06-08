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
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.indexing.query.Query;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Implementation Class for searching through the index.
 *
 * @todo this is not a component, but extends ALE, meaning logging will throw an exception! -- should be a component
 */
public class DefaultRepositoryIndexSearcher
    extends AbstractLogEnabled
    implements RepositoryIndexSearcher
{
    protected RepositoryIndex index;

    private ArtifactFactory factory;

    private List artifactList;

    /**
     * Constructor
     *
     * @param index the index object
     */
    protected DefaultRepositoryIndexSearcher( RepositoryIndex index, ArtifactFactory factory )
    {
        this.index = index;
        this.factory = factory;
    }

    /**
     * @see RepositoryIndexSearcher#search(org.apache.maven.repository.indexing.query.Query)
     */
    public List search( Query query )
        throws RepositoryIndexSearchException
    {
        artifactList = new ArrayList();
        org.apache.lucene.search.Query luceneQuery;
        try
        {
            luceneQuery = createLuceneQuery( query );
        }
        catch ( ParseException e )
        {
            throw new RepositoryIndexSearchException( "Unable to construct query: " + e.getMessage(), e );
        }

        IndexSearcher searcher;
        try
        {
            searcher = new IndexSearcher( index.getIndexPath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
        }

        List docs;
        try
        {
            Hits hits = searcher.search( luceneQuery );
            docs = buildList( hits );
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
        }
        finally
        {
            try
            {
                searcher.close();
            }
            catch ( IOException e )
            {
                getLogger().error( "Unable to close index searcher", e );
            }
        }

        return docs;
    }

    /**
     * Method to create a lucene Query object by converting a prepared Query object
     *
     * @param query the prepared Query object to be converted into a lucene Query object
     * @return a lucene Query object to represent the passed Query object
     * @throws ParseException
     */
    private org.apache.lucene.search.Query createLuceneQuery( Query query )
        throws ParseException
    {
        return query.createLuceneQuery( index );
    }

    /**
     * Create a list of artifact objects from the result set.
     *
     * @param hits the search result set
     * @return List
     */
    private List buildList( Hits hits )
        throws RepositoryIndexSearchException, IOException
    {
        for ( int i = 0; i < hits.length(); i++ )
        {
            Document doc = hits.doc( i );
            artifactList.add( createSearchedObjectFromIndexDocument( doc ) );
        }

        return artifactList;
    }

    /**
     * Method for creating the object to be returned for the search
     *
     * @param doc the index document where the object field values will be retrieved from
     * @return Object
     */
    protected RepositoryIndexSearchHit createSearchedObjectFromIndexDocument( Document doc )
        throws RepositoryIndexSearchException
    {
        RepositoryIndexSearchHit searchHit = null;

        // the document is of type artifact
        String groupId = doc.get( RepositoryIndex.FLD_GROUPID );
        String artifactId = doc.get( RepositoryIndex.FLD_ARTIFACTID );
        String version = doc.get( RepositoryIndex.FLD_VERSION );
        if ( doc.get( RepositoryIndex.FLD_DOCTYPE ).equals( RepositoryIndex.ARTIFACT ) )
        {
            String packaging = doc.get( RepositoryIndex.FLD_PACKAGING );
            Artifact artifact = factory.createBuildArtifact( groupId, artifactId, version, packaging );

            artifact.setFile(
                new File( index.getRepository().getBasedir(), index.getRepository().pathOf( artifact ) ) );

            Map map = new HashMap();
            map.put( RepositoryIndex.ARTIFACT, artifact );
            map.put( RepositoryIndex.FLD_CLASSES, doc.get( RepositoryIndex.FLD_CLASSES ) );
            map.put( RepositoryIndex.FLD_PACKAGES, doc.get( RepositoryIndex.FLD_PACKAGES ) );
            map.put( RepositoryIndex.FLD_FILES, doc.get( RepositoryIndex.FLD_FILES ) );
            map.put( RepositoryIndex.FLD_MD5, doc.get( RepositoryIndex.FLD_MD5 ) );
            map.put( RepositoryIndex.FLD_SHA1, doc.get( RepositoryIndex.FLD_SHA1 ) );
            map.put( RepositoryIndex.FLD_PACKAGING, doc.get( RepositoryIndex.FLD_PACKAGING ) );

            searchHit = new RepositoryIndexSearchHit( true, false, false );
            searchHit.setObject( map );
        }
        // the document is of type model
        else if ( doc.get( RepositoryIndex.FLD_DOCTYPE ).equals( RepositoryIndex.POM ) )
        {
            Artifact pomArtifact = factory.createProjectArtifact( groupId, artifactId, version );

            searchHit = new RepositoryIndexSearchHit( false, false, true );
            searchHit.setObject( readPom( pomArtifact ) );
        }
        // the document is of type metadata
        else if ( doc.get( RepositoryIndex.FLD_DOCTYPE ).equals( RepositoryIndex.METADATA ) )
        {
            List pathParts = new ArrayList();
            StringTokenizer st = new StringTokenizer( doc.get( RepositoryIndex.FLD_NAME ), "/\\" );
            while ( st.hasMoreTokens() )
            {
                pathParts.add( st.nextToken() );
            }

            Collections.reverse( pathParts );
            String tmpDir = (String) pathParts.get( 1 );

            RepositoryMetadata repoMetadata;

            if ( tmpDir.equals( version ) )
            {
                repoMetadata = new SnapshotArtifactRepositoryMetadata(
                    factory.createBuildArtifact( groupId, artifactId, version, "jar" ) );
            }
            else if ( tmpDir.equals( artifactId ) )
            {
                repoMetadata = new ArtifactRepositoryMetadata(
                    factory.createBuildArtifact( groupId, artifactId, version, "jar" ) );
            }
            else
            {
                repoMetadata = new GroupRepositoryMetadata( groupId );
            }

            repoMetadata.setMetadata( readMetadata( repoMetadata ) );

            searchHit = new RepositoryIndexSearchHit( false, true, false );
            searchHit.setObject( repoMetadata );
        }

        return searchHit;
    }

    /**
     * Create RepositoryMetadata object.
     *
     * @return RepositoryMetadata
     */
    private Metadata readMetadata( RepositoryMetadata repoMetadata )
        throws RepositoryIndexSearchException
    {
        File file = new File( index.getRepository().getBasedir(),
                              index.getRepository().pathOfRemoteRepositoryMetadata( repoMetadata ) );

        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        FileReader reader = null;
        try
        {
            reader = new FileReader( file );
            return metadataReader.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryIndexSearchException( "Unable to find metadata file: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to read metadata file: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException xe )
        {
            throw new RepositoryIndexSearchException( "Unable to parse metadata file: " + xe.getMessage(), xe );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Create RepositoryMetadata object.
     *
     * @return RepositoryMetadata
     */
    private Model readPom( Artifact pomArtifact )
        throws RepositoryIndexSearchException
    {
        File file = new File( index.getRepository().getBasedir(), index.getRepository().pathOf( pomArtifact ) );

        MavenXpp3Reader r = new MavenXpp3Reader();

        FileReader reader = null;
        try
        {
            reader = new FileReader( file );
            return r.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryIndexSearchException( "Unable to find requested POM: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to read POM: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException xe )
        {
            throw new RepositoryIndexSearchException( "Unable to parse POM: " + xe.getMessage(), xe );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

}
