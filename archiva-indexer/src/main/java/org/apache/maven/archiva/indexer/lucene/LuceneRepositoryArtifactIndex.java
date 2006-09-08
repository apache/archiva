package org.apache.maven.archiva.indexer.lucene;

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
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexModifier;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.query.Query;
import org.apache.maven.archiva.indexer.record.MinimalIndexRecordFields;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecord;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.archiva.indexer.record.StandardIndexRecordFields;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lucene implementation of a repository index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneRepositoryArtifactIndex
    implements RepositoryArtifactIndex
{
    /**
     * The location of the index on the file system.
     */
    private File indexLocation;

    /**
     * Convert repository records to Lucene documents.
     */
    private LuceneIndexRecordConverter converter;

    private static final String FLD_PK = "pk";

    private static Analyzer luceneAnalyzer = new LuceneAnalyzer();

    private static long lastUpdatedTime = 0;

    public LuceneRepositoryArtifactIndex( File indexPath, LuceneIndexRecordConverter converter )
    {
        this.indexLocation = indexPath;
        this.converter = converter;
    }

    public void indexRecords( Collection records )
        throws RepositoryIndexException
    {
        deleteRecords( records );

        addRecords( records );
    }

    private void addRecords( Collection records )
        throws RepositoryIndexException
    {
        IndexWriter indexWriter;
        try
        {
            indexWriter = new IndexWriter( indexLocation, getAnalyzer(), !exists() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Unable to open index", e );
        }

        try
        {
            for ( Iterator i = records.iterator(); i.hasNext(); )
            {
                RepositoryIndexRecord record = (RepositoryIndexRecord) i.next();

                if ( record != null )
                {
                    Document document = converter.convert( record );
                    document.add(
                        new Field( FLD_PK, record.getPrimaryKey(), Field.Store.NO, Field.Index.UN_TOKENIZED ) );

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
            lastUpdatedTime = System.currentTimeMillis();
        }
    }

    public static Analyzer getAnalyzer()
    {
        return luceneAnalyzer;
    }

    private static class LuceneAnalyzer
        extends Analyzer
    {
        private static final Analyzer STANDARD = new StandardAnalyzer();

        public TokenStream tokenStream( String field, final Reader reader )
        {
            // do not tokenize field called 'element'
            if ( StandardIndexRecordFields.DEPENDENCIES.equals( field ) )
            {
                return new CharTokenizer( reader )
                {
                    protected boolean isTokenChar( char c )
                    {
                        return c != '\n';
                    }
                };
            }
            else if ( StandardIndexRecordFields.FILES.equals( field ) )
            {
                return new CharTokenizer( reader )
                {
                    protected boolean isTokenChar( char c )
                    {
                        return c != '\n' && c != '/';
                    }
                };
            }
            else
            if ( StandardIndexRecordFields.CLASSES.equals( field ) || MinimalIndexRecordFields.CLASSES.equals( field ) )
            {
                return new CharTokenizer( reader )
                {
                    protected boolean isTokenChar( char c )
                    {
                        return c != '\n' && c != '.';
                    }

                    protected char normalize( char c )
                    {
                        return Character.toLowerCase( c );
                    }
                };
            }
            else if ( StandardIndexRecordFields.GROUPID.equals( field ) )
            {
                return new CharTokenizer( reader )
                {
                    protected boolean isTokenChar( char c )
                    {
                        return c != '.';
                    }

                    protected char normalize( char c )
                    {
                        return Character.toLowerCase( c );
                    }
                };
            }
            else if ( StandardIndexRecordFields.VERSION.equals( field ) ||
                StandardIndexRecordFields.BASE_VERSION.equals( field ) )
            {
                return new CharTokenizer( reader )
                {
                    protected boolean isTokenChar( char c )
                    {
                        return c != '-';
                    }
                };
            }
            else if ( StandardIndexRecordFields.FILENAME.equals( field ) ||
                MinimalIndexRecordFields.FILENAME.equals( field ) )
            {
                return new CharTokenizer( reader )
                {
                    protected boolean isTokenChar( char c )
                    {
                        return c != '-' && c != '.' && c != '/';
                    }
                };
            }
            else
            {
                // use standard analyzer
                return STANDARD.tokenStream( field, reader );
            }
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
                    RepositoryIndexRecord record = (RepositoryIndexRecord) i.next();

                    if ( record != null )
                    {
                        Term term = new Term( FLD_PK, record.getPrimaryKey() );

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
        return getAllFieldValues( FLD_PK );
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

    public void indexArtifacts( List artifacts, RepositoryIndexRecordFactory factory )
        throws RepositoryIndexException
    {
        IndexModifier indexModifier = null;
        try
        {
            indexModifier = new IndexModifier( indexLocation, getAnalyzer(), !exists() );

            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                RepositoryIndexRecord record = factory.createRecord( artifact );

                if ( record != null )
                {
                    Term term = new Term( FLD_PK, record.getPrimaryKey() );

                    indexModifier.deleteDocuments( term );

                    Document document = converter.convert( record );
                    document.add(
                        new Field( FLD_PK, record.getPrimaryKey(), Field.Store.NO, Field.Index.UN_TOKENIZED ) );

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
            lastUpdatedTime = System.currentTimeMillis();
        }
    }

    public List getAllGroupIds()
        throws RepositoryIndexException
    {
        return getAllFieldValues( StandardIndexRecordFields.GROUPID_EXACT );
    }

    public List getArtifactIds( String groupId )
        throws RepositoryIndexSearchException
    {
        return searchField( new TermQuery( new Term( StandardIndexRecordFields.GROUPID_EXACT, groupId ) ),
                            StandardIndexRecordFields.ARTIFACTID );
    }

    public List getVersions( String groupId, String artifactId )
        throws RepositoryIndexSearchException
    {
        BooleanQuery query = new BooleanQuery();
        query.add( new TermQuery( new Term( StandardIndexRecordFields.GROUPID_EXACT, groupId ) ),
                   BooleanClause.Occur.MUST );
        query.add( new TermQuery( new Term( StandardIndexRecordFields.ARTIFACTID_EXACT, artifactId ) ),
                   BooleanClause.Occur.MUST );

        return searchField( query, StandardIndexRecordFields.VERSION );
    }

    public long getLastUpdatedTime()
    {
        return lastUpdatedTime;
    }

    private List searchField( org.apache.lucene.search.Query luceneQuery, String fieldName )
        throws RepositoryIndexSearchException
    {
        Set results = new LinkedHashSet();

        IndexSearcher searcher;
        try
        {
            searcher = new IndexSearcher( indexLocation.getAbsolutePath() );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
        }

        try
        {
            Hits hits = searcher.search( luceneQuery );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document doc = hits.doc( i );

                results.add( doc.get( fieldName ) );
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to search index: " + e.getMessage(), e );
        }
        finally
        {
            closeQuietly( searcher );
        }
        return new ArrayList( results );
    }

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

                records.add( converter.convert( doc ) );
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
}
