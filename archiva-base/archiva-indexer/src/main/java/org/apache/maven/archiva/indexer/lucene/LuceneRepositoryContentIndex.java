package org.apache.maven.archiva.indexer.lucene;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexModifier;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.query.Query;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Lucene implementation of a repository index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneRepositoryContentIndex
    implements RepositoryContentIndex
{
    /**
     * The max field length for a field in a document.
     */
    private static final int MAX_FIELD_LENGTH = 40000;

    /**
     * The location of the index on the file system.
     */
    private File indexLocation;

    /**
     * The Lucene Index Handlers
     */
    private LuceneIndexHandlers indexHandlers;

    public LuceneRepositoryContentIndex( File indexDir, LuceneIndexHandlers handlers )
    {
        this.indexLocation = indexDir;
        this.indexHandlers = handlers;
    }

    public void indexRecords( Collection records )
        throws RepositoryIndexException
    {
        deleteRecords( records );

        addRecords( records );
    }

    public void modifyRecords( Collection records )
        throws RepositoryIndexException
    {
        IndexModifier indexModifier = null;
        try
        {
            indexModifier = new IndexModifier( indexLocation, indexHandlers.getAnalyzer(), !exists() );
            indexModifier.setMaxFieldLength( MAX_FIELD_LENGTH );

            for ( Iterator i = records.iterator(); i.hasNext(); )
            {
                LuceneRepositoryContentRecord record = (LuceneRepositoryContentRecord) i.next();

                if ( record != null )
                {
                    Term term = new Term( LuceneDocumentMaker.PRIMARY_KEY, record.getPrimaryKey() );

                    indexModifier.deleteDocuments( term );

                    Document document = indexHandlers.getConverter().convert( record );

                    indexModifier.addDocument( document );
                }
            }
            indexModifier.optimize();
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error updating index: " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( indexModifier );
        }
    }

    public void modifyRecord( LuceneRepositoryContentRecord record )
        throws RepositoryIndexException
    {
        IndexModifier indexModifier = null;
        try
        {
            indexModifier = new IndexModifier( indexLocation, indexHandlers.getAnalyzer(), !exists() );
            indexModifier.setMaxFieldLength( MAX_FIELD_LENGTH );

            if ( record != null )
            {
                Term term = new Term( LuceneDocumentMaker.PRIMARY_KEY, record.getPrimaryKey() );

                indexModifier.deleteDocuments( term );

                Document document = indexHandlers.getConverter().convert( record );

                indexModifier.addDocument( document );
            }
            indexModifier.optimize();
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error updating index: " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( indexModifier );
        }
    }

    private void addRecords( Collection records )
        throws RepositoryIndexException
    {
        IndexWriter indexWriter;
        try
        {
            indexWriter = new IndexWriter( indexLocation, indexHandlers.getAnalyzer(), !exists() );
            indexWriter.setMaxFieldLength( MAX_FIELD_LENGTH );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Unable to open index", e );
        }

        try
        {
            for ( Iterator i = records.iterator(); i.hasNext(); )
            {
                LuceneRepositoryContentRecord record = (LuceneRepositoryContentRecord) i.next();

                if ( record != null )
                {
                    Document document = indexHandlers.getConverter().convert( record );

                    indexWriter.addDocument( document );
                }
            }

            indexWriter.optimize();
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Failed to add an index document", e );
        }
        finally
        {
            closeQuietly( indexWriter );
        }
    }

    public void deleteRecords( Collection records )
        throws RepositoryIndexException
    {
        if ( exists() )
        {
            IndexReader indexReader = null;
            try
            {
                indexReader = IndexReader.open( indexLocation );

                for ( Iterator i = records.iterator(); i.hasNext(); )
                {
                    LuceneRepositoryContentRecord record = (LuceneRepositoryContentRecord) i.next();

                    if ( record != null )
                    {
                        Term term = new Term( LuceneDocumentMaker.PRIMARY_KEY, record.getPrimaryKey() );

                        indexReader.deleteDocuments( term );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new RepositoryIndexException( "Error deleting document: " + e.getMessage(), e );
            }
            finally
            {
                closeQuietly( indexReader );
            }
        }
    }

    public Collection getAllRecords()
        throws RepositoryIndexSearchException
    {
        return search( new LuceneQuery( new MatchAllDocsQuery() ) );
    }

    public Collection getAllRecordKeys()
        throws RepositoryIndexException
    {
        return getAllFieldValues( LuceneDocumentMaker.PRIMARY_KEY );
    }

    private List getAllFieldValues( String fieldName )
        throws RepositoryIndexException
    {
        List keys = new ArrayList();

        if ( exists() )
        {
            IndexReader indexReader = null;
            TermEnum terms = null;
            try
            {
                indexReader = IndexReader.open( indexLocation );

                terms = indexReader.terms( new Term( fieldName, "" ) );
                while ( fieldName.equals( terms.term().field() ) )
                {
                    keys.add( terms.term().text() );

                    if ( !terms.next() )
                    {
                        break;
                    }
                }
            }
            catch ( IOException e )
            {
                throw new RepositoryIndexException( "Error deleting document: " + e.getMessage(), e );
            }
            finally
            {
                closeQuietly( indexReader );
                closeQuietly( terms );
            }
        }
        return keys;
    }

    //    public List getAllGroupIds() throws RepositoryIndexException
    //    {
    //        return getAllFieldValues( StandardIndexRecordFields.GROUPID_EXACT );
    //    }
    //
    //    public List getArtifactIds( String groupId ) throws RepositoryIndexSearchException
    //    {
    //        return searchField( new TermQuery( new Term( StandardIndexRecordFields.GROUPID_EXACT, groupId ) ),
    //                            StandardIndexRecordFields.ARTIFACTID );
    //    }
    //
    //    public List getVersions( String groupId, String artifactId ) throws RepositoryIndexSearchException
    //    {
    //        BooleanQuery query = new BooleanQuery();
    //        query.add( new TermQuery( new Term( StandardIndexRecordFields.GROUPID_EXACT, groupId ) ),
    //                   BooleanClause.Occur.MUST );
    //        query.add( new TermQuery( new Term( StandardIndexRecordFields.ARTIFACTID_EXACT, artifactId ) ),
    //                   BooleanClause.Occur.MUST );
    //
    //        return searchField( query, StandardIndexRecordFields.VERSION );
    //    }

    //    private List searchField( org.apache.lucene.search.Query luceneQuery, String fieldName )
    //        throws RepositoryIndexSearchException
    //    {
    //        Set results = new LinkedHashSet();
    //
    //        IndexSearcher searcher;
    //        try
    //        {
    //            searcher = new IndexSearcher( indexLocation.getAbsolutePath() );
    //        }
    //        catch ( IOException e )
    //        {
    //            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
    //        }
    //
    //        try
    //        {
    //            Hits hits = searcher.search( luceneQuery );
    //            for ( int i = 0; i < hits.length(); i++ )
    //            {
    //                Document doc = hits.doc( i );
    //
    //                results.add( doc.get( fieldName ) );
    //            }
    //        }
    //        catch ( IOException e )
    //        {
    //            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
    //        }
    //        finally
    //        {
    //            closeQuietly( searcher );
    //        }
    //        return new ArrayList( results );
    //    }

    public boolean exists()
        throws RepositoryIndexException
    {
        if ( IndexReader.indexExists( indexLocation ) )
        {
            return true;
        }
        else if ( !indexLocation.exists() )
        {
            return false;
        }
        else if ( indexLocation.isDirectory() )
        {
            if ( indexLocation.listFiles().length > 1 )
            {
                throw new RepositoryIndexException( indexLocation + " is not a valid index directory." );
            }
            else
            {
                return false;
            }
        }
        else
        {
            throw new RepositoryIndexException( indexLocation + " is not a directory." );
        }
    }

    public List search( Query query )
        throws RepositoryIndexSearchException
    {
        LuceneQuery lQuery = (LuceneQuery) query;

        org.apache.lucene.search.Query luceneQuery = lQuery.getLuceneQuery();

        IndexSearcher searcher;
        try
        {
            searcher = new IndexSearcher( indexLocation.getAbsolutePath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
        }

        List records = new ArrayList();
        try
        {
            Hits hits = searcher.search( luceneQuery );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document doc = hits.doc( i );

                records.add( indexHandlers.getConverter().convert( doc ) );
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
        }
        catch ( ParseException e )
        {
            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( searcher );
        }

        return records;
    }

    public QueryParser getQueryParser()
    {
        return this.indexHandlers.getQueryParser();
    }

    private static void closeQuietly( IndexSearcher searcher )
    {
        try
        {
            if ( searcher != null )
            {
                searcher.close();
            }
        }
        catch ( IOException e )
        {
            // ignore
        }
    }

    private static void closeQuietly( TermEnum terms )
        throws RepositoryIndexException
    {
        if ( terms != null )
        {
            try
            {
                terms.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    private static void closeQuietly( IndexWriter indexWriter )
        throws RepositoryIndexException
    {
        try
        {
            if ( indexWriter != null )
            {
                indexWriter.close();
            }
        }
        catch ( IOException e )
        {
            // write should compain if it can't be closed, data probably not persisted
            throw new RepositoryIndexException( e.getMessage(), e );
        }
    }

    private static void closeQuietly( IndexModifier indexModifier )
    {
        if ( indexModifier != null )
        {
            try
            {
                indexModifier.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    private static void closeQuietly( IndexReader reader )
    {
        try
        {
            if ( reader != null )
            {
                reader.close();
            }
        }
        catch ( IOException e )
        {
            // ignore
        }
    }

    public File getIndexDirectory()
    {
        return this.indexLocation;
    }

    public String getId()
    {
        return this.indexHandlers.getId();
    }
}
