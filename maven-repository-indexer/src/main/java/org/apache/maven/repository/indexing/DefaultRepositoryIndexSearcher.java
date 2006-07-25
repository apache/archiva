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
import org.apache.maven.artifact.repository.ArtifactRepository;
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
 * @plexus.component role="org.apache.maven.repository.indexing.RepositoryIndexSearcher"
 */
public class DefaultRepositoryIndexSearcher
    extends AbstractLogEnabled
    implements RepositoryIndexSearcher
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;

    public List search( Query query, RepositoryIndex index )
        throws RepositoryIndexSearchException
    {
        org.apache.lucene.search.Query luceneQuery;
        try
        {
            luceneQuery = query.createLuceneQuery( index );
        }
        catch ( ParseException e )
        {
            throw new RepositoryIndexSearchException( "Unable to construct query: " + e.getMessage(), e );
        }

        IndexSearcher searcher;
        try
        {
            searcher = new IndexSearcher( index.getIndexPath().getAbsolutePath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
        }

        List docs = new ArrayList();
        try
        {
            Hits hits = searcher.search( luceneQuery );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document doc = hits.doc( i );
                docs.add( createSearchedObjectFromIndexDocument( doc, index.getRepository() ) );
            }
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

    private RepositoryIndexSearchHit createSearchedObjectFromIndexDocument( Document doc,
                                                                            ArtifactRepository repository )
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

            artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

            // TODO: introduce strongly types search result!
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

            // TODO: introduce strongly types search result! Don't read the POM here, though - populate with the data from the index
            searchHit = new RepositoryIndexSearchHit( false, false, true );
            searchHit.setObject( readPom( pomArtifact, repository ) );
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
                    factory.createProjectArtifact( groupId, artifactId, version ) );
            }
            else if ( tmpDir.equals( artifactId ) )
            {
                repoMetadata =
                    new ArtifactRepositoryMetadata( factory.createProjectArtifact( groupId, artifactId, version ) );
            }
            else
            {
                repoMetadata = new GroupRepositoryMetadata( groupId );
            }

            // TODO: introduce strongly types search result! Don't read the metadata here, though - populate with the data from the index
            repoMetadata.setMetadata( readMetadata( repoMetadata, repository ) );

            searchHit = new RepositoryIndexSearchHit( false, true, false );
            searchHit.setObject( repoMetadata );
        }

        return searchHit;
    }

    private Metadata readMetadata( RepositoryMetadata repoMetadata, ArtifactRepository repository )
        throws RepositoryIndexSearchException
    {
        File file = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repoMetadata ) );

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

    private Model readPom( Artifact pomArtifact, ArtifactRepository repository )
        throws RepositoryIndexSearchException
    {
        File file = new File( repository.getBasedir(), repository.pathOf( pomArtifact ) );

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
