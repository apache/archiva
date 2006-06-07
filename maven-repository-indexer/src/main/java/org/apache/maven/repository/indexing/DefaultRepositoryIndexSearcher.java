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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.indexing.query.Query;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Implementation Class for searching through the index
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
        catch ( XmlPullParserException xe )
        {
            throw new RepositoryIndexSearchException( "Unable to parse metadata file: " + xe.getMessage(), xe );
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
     * @throws IOException
     */
    private List buildList( Hits hits )
        throws MalformedURLException, IOException, XmlPullParserException
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
        throws MalformedURLException, IOException, XmlPullParserException
    {
        RepositoryIndexSearchHit searchHit = null;

        // the document is of type artifact
        if ( doc.get( RepositoryIndex.FLD_DOCTYPE ).equals( RepositoryIndex.ARTIFACT ) )
        {
            String groupId = doc.get( RepositoryIndex.FLD_GROUPID );
            String artifactId = doc.get( RepositoryIndex.FLD_ARTIFACTID );
            String version = doc.get( RepositoryIndex.FLD_VERSION );
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
            InputStream is = new FileInputStream( new File( index.getRepository().getBasedir() +
                doc.get( RepositoryIndex.FLD_GROUPID ).replace( '.', '/' ) + "/" +
                doc.get( RepositoryIndex.FLD_ARTIFACTID ) + "/" + doc.get( RepositoryIndex.FLD_VERSION ) + "/" +
                doc.get( RepositoryIndex.FLD_ARTIFACTID ) + "-" + doc.get( RepositoryIndex.FLD_VERSION ) + ".pom" ) );
            MavenXpp3Reader reader = new MavenXpp3Reader();

            searchHit = new RepositoryIndexSearchHit( false, false, true );
            searchHit.setObject( reader.read( new InputStreamReader( is ) ) );

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
            Iterator it = pathParts.iterator();
            String metadataFile = (String) it.next();
            String tmpDir = (String) it.next();

            String metadataType;
            if ( tmpDir.equals( doc.get( RepositoryIndex.FLD_VERSION ) ) )
            {
                metadataType = MetadataRepositoryIndex.SNAPSHOT_METADATA;
            }
            else if ( tmpDir.equals( doc.get( RepositoryIndex.FLD_ARTIFACTID ) ) )
            {
                metadataType = MetadataRepositoryIndex.ARTIFACT_METADATA;
            }
            else
            {
                metadataType = MetadataRepositoryIndex.GROUP_METADATA;
            }

            RepositoryMetadata repoMetadata = getMetadata( doc.get( RepositoryIndex.FLD_GROUPID ),
                                                           doc.get( RepositoryIndex.FLD_ARTIFACTID ),
                                                           doc.get( RepositoryIndex.FLD_VERSION ), metadataFile,
                                                           metadataType );
            searchHit = new RepositoryIndexSearchHit( false, true, false );
            searchHit.setObject( repoMetadata );
        }

        return searchHit;
    }

    /**
     * Create RepositoryMetadata object.
     *
     * @param groupId      the groupId to be set
     * @param artifactId   the artifactId to be set
     * @param version      the version to be set
     * @param filename     the name of the metadata file
     * @param metadataType the type of RepositoryMetadata object to be created (GROUP, ARTIFACT or SNAPSHOT)
     * @return RepositoryMetadata
     * @throws IOException
     * @throws XmlPullParserException
     */
    private RepositoryMetadata getMetadata( String groupId, String artifactId, String version, String filename,
                                            String metadataType )
        throws IOException, XmlPullParserException
    {
        RepositoryMetadata repoMetadata = null;

        // TODO! file handles left open
        InputStream is;
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        //group metadata
        if ( metadataType.equals( MetadataRepositoryIndex.GROUP_METADATA ) )
        {
            is = new FileInputStream(
                new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + filename ) );
            repoMetadata = new GroupRepositoryMetadata( groupId );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //artifact metadata
        else if ( metadataType.equals( MetadataRepositoryIndex.ARTIFACT_METADATA ) )
        {
            is = new FileInputStream( new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" +
                artifactId + "/" + filename ) );
            repoMetadata =
                new ArtifactRepositoryMetadata( factory.createBuildArtifact( groupId, artifactId, version, "jar" ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //snapshot/version metadata
        else if ( metadataType.equals( MetadataRepositoryIndex.SNAPSHOT_METADATA ) )
        {
            is = new FileInputStream( new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" +
                artifactId + "/" + version + "/" + filename ) );
            repoMetadata = new SnapshotArtifactRepositoryMetadata(
                factory.createBuildArtifact( groupId, artifactId, version, "jar" ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }

        return repoMetadata;
    }

}
