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
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.repository.indexing.query.CompoundQuery;
import org.apache.maven.repository.indexing.query.CompoundQueryTerm;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.RangeQuery;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Collections;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Abstract Class to hold common codes for the different RepositoryIndexSearcher
 */
public class DefaultRepositoryIndexSearcher
    extends AbstractLogEnabled
    implements RepositoryIndexSearcher
{
    protected RepositoryIndex index;

    private ArtifactFactory factory;

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
     * Method to create a lucene Query object from a single query phrase
     *
     * @param field the index field name to search into
     * @param value the index field value to match the field with
     * @return a lucene Query object representing the query phrase field = value
     * @throws ParseException
     */
    private org.apache.lucene.search.Query createLuceneQuery( String field, String value )
        throws ParseException
    {
        org.apache.lucene.search.Query qry;
        if ( index.isKeywordField( field ) )
        {
            Term term = new Term( field, value );
            qry = new TermQuery( term );
        }
        else
        {
            QueryParser parser = new QueryParser( field, index.getAnalyzer() );
            qry = parser.parse( value );
        }
        return qry;
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
        org.apache.lucene.search.Query retVal;

        if ( query instanceof CompoundQuery )
        {
            BooleanQuery booleanQuery = new BooleanQuery();
            CompoundQuery compoundQuery = (CompoundQuery) query;
            List queries = compoundQuery.getQueries();
            for ( Iterator i = queries.iterator(); i.hasNext(); )
            {
                CompoundQueryTerm subquery = (CompoundQueryTerm) i.next();

                org.apache.lucene.search.Query luceneQuery = createLuceneQuery( subquery.getQuery() );

                booleanQuery.add( luceneQuery, subquery.isRequired(), subquery.isProhibited() );
            }
            retVal = booleanQuery;
        }
        else if ( query instanceof RangeQuery )
        {
            RangeQuery rq = (RangeQuery) query;
            List queries = rq.getQueries();
            Iterator iter = queries.iterator();
            Term begin = null, end = null;
            if ( queries.size() == 2 )
            {
                SinglePhraseQuery qry = (SinglePhraseQuery) iter.next();
                begin = new Term( qry.getField(), qry.getValue() );
                qry = (SinglePhraseQuery) iter.next();
                end = new Term( qry.getField(), qry.getValue() );
            }
            retVal = new org.apache.lucene.search.RangeQuery( begin, end, rq.isInclusive() );
        }
        else
        {
            SinglePhraseQuery singlePhraseQuery = (SinglePhraseQuery) query;
            retVal = createLuceneQuery( singlePhraseQuery.getField(), singlePhraseQuery.getValue() );
        }
        return retVal;
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
        List artifactList = new ArrayList();

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
    protected Object createSearchedObjectFromIndexDocument( Document doc )
        throws MalformedURLException, IOException, XmlPullParserException
    {
        String groupId, artifactId, version, name, packaging;

        if ( doc.get( index.FLD_DOCTYPE ).equals( index.ARTIFACT ) )
        {
            groupId = doc.get( ArtifactRepositoryIndex.FLD_GROUPID );
            artifactId = doc.get( ArtifactRepositoryIndex.FLD_ARTIFACTID );
            version = doc.get( ArtifactRepositoryIndex.FLD_VERSION );
            name = doc.get( ArtifactRepositoryIndex.FLD_NAME );
            packaging = name.substring( name.lastIndexOf( '.' ) + 1 );
            Artifact artifact = factory.createBuildArtifact( groupId, artifactId, version, packaging );
            String groupIdTemp = groupId.replace( '.', '/' );
            artifact.setFile( new File(
                index.getRepository().getBasedir() + groupIdTemp + "/" + artifactId + "/" + version + "/" + name ) );

            return artifact;
        }
        else if ( doc.get( index.FLD_DOCTYPE ).equals( index.POM ) )
        {
            groupId = doc.get( PomRepositoryIndex.FLD_GROUPID );
            artifactId = doc.get( PomRepositoryIndex.FLD_ARTIFACTID );
            version = doc.get( PomRepositoryIndex.FLD_VERSION );
            packaging = doc.get( PomRepositoryIndex.FLD_PACKAGING );

            return factory.createBuildArtifact( groupId, artifactId, version, packaging );
        }
        else if ( doc.get( index.FLD_DOCTYPE ).equals( index.METADATA ) )
        {
            List pathParts = new ArrayList();
            StringTokenizer st = new StringTokenizer( doc.get( MetadataRepositoryIndex.FLD_NAME ), "/\\" );
            while ( st.hasMoreTokens() )
            {
                pathParts.add( st.nextToken() );
            }

            Collections.reverse( pathParts );
            Iterator it = pathParts.iterator();
            String metadataFile = (String) it.next();
            String tmpDir = (String) it.next();

            String metadataType = "";
            if ( tmpDir.equals( doc.get( MetadataRepositoryIndex.FLD_GROUPID ) ) )
            {
                metadataType = MetadataRepositoryIndex.GROUP_METADATA;
            }
            else if ( tmpDir.equals( doc.get( MetadataRepositoryIndex.FLD_ARTIFACTID ) ) )
            {
                metadataType = MetadataRepositoryIndex.ARTIFACT_METADATA;
            }
            else
            {
                metadataType = MetadataRepositoryIndex.SNAPSHOT_METADATA;
            }

            RepositoryMetadata repoMetadata = null;
            repoMetadata = getMetadata( doc.get( MetadataRepositoryIndex.FLD_GROUPID ),
                                        doc.get( MetadataRepositoryIndex.FLD_ARTIFACTID ),
                                        doc.get( MetadataRepositoryIndex.FLD_VERSION ), metadataFile, metadataType );

            return repoMetadata;
        }

        return null;
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
     * @throws MalformedURLException
     * @throws IOException
     * @throws XmlPullParserException
     */
    private RepositoryMetadata getMetadata( String groupId, String artifactId, String version, String filename,
                                            String metadataType )
        throws MalformedURLException, IOException, XmlPullParserException
    {
        RepositoryMetadata repoMetadata = null;
        URL url;
        InputStream is = null;
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        //group metadata
        if ( metadataType.equals( MetadataRepositoryIndex.GROUP_METADATA ) )
        {
            url = new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new GroupRepositoryMetadata( groupId );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //artifact metadata
        else if ( metadataType.equals( MetadataRepositoryIndex.ARTIFACT_METADATA ) )
        {
            url = new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + artifactId + "/" +
                filename ).toURL();
            is = url.openStream();
            repoMetadata =
                new ArtifactRepositoryMetadata( factory.createBuildArtifact( groupId, artifactId, version, "jar" ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //snapshot/version metadata
        else if ( metadataType.equals( MetadataRepositoryIndex.SNAPSHOT_METADATA ) )
        {
            url = new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + artifactId + "/" +
                version + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new SnapshotArtifactRepositoryMetadata(
                factory.createBuildArtifact( groupId, artifactId, version, "jar" ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }

        return repoMetadata;
    }

}
