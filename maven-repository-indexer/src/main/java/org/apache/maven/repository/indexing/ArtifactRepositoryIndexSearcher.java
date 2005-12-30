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
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class searches the index for existing artifacts that contains the
 * specified query string.
 *
 * @plexus.component role="org.apache.maven.repository.indexing.RepositoryIndexSearcher" role-hint="artifact"
 */
public class ArtifactRepositoryIndexSearcher
    implements RepositoryIndexSearcher
{
    private static final String NAME = "name";

    private static final String GROUPID = "groupId";

    private static final String ARTIFACTID = "artifactId";

    private static final String VERSION = "version";

    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;

    /**
     * Search the artifact that contains the query string in the specified
     * search field.
     *
     * @param queryString
     * @param searchField
     */
    public List search( RepositoryIndex index, String queryString, String searchField )
        throws RepositoryIndexSearchException
    {
        List artifactList = new ArrayList();

        try
        {
            IndexSearcher searcher = new IndexSearcher( index.getIndexPath() );
            QueryParser parser = new QueryParser( searchField, index.getAnalyzer() );
            Query qry = parser.parse( queryString );
            Hits hits = searcher.search( qry );

            for ( int i = 0; i < hits.length(); i++ )
            {
                Document doc = hits.doc( i );

                String groupId = doc.get( GROUPID );
                String artifactId = doc.get( ARTIFACTID );
                String version = doc.get( VERSION );
                String name = doc.get( NAME );
                String packaging = name.substring( name.lastIndexOf( '.' ) + 1 );
                Artifact artifact = factory.createBuildArtifact( groupId, artifactId, version, packaging );

                artifactList.add( artifact );
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( e.getMessage(), e );
        }
        catch ( ParseException e )
        {
            throw new RepositoryIndexSearchException( "Error parsing query: " + e.getMessage() );
        }

        return artifactList;
    }
}
