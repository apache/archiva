package org.apache.maven.repository.indexing.lucene;

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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.repository.indexing.RepositoryArtifactIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecord;

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
            close( indexWriter );
        }
    }

    private void close( IndexWriter indexWriter )
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
            throw new RepositoryIndexException( e.getMessage(), e );
        }
    }

    private Analyzer getAnalyzer()
    {
        // TODO: investigate why changed in original! Probably for MD5 and number querying.
        return new StandardAnalyzer();
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

                for ( Iterator artifacts = records.iterator(); artifacts.hasNext(); )
                {
                    RepositoryIndexRecord record = (RepositoryIndexRecord) artifacts.next();

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
                if ( indexReader != null )
                {
                    closeQuietly( indexReader );
                }
            }
        }
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
