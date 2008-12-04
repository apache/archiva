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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Lucene implementation of a repository index.
 *
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
    
    private final ManagedRepositoryConfiguration repository;

    public LuceneRepositoryContentIndex( ManagedRepositoryConfiguration repository, File indexDir, LuceneIndexHandlers handlers )
    {
        this.repository = repository;
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
        synchronized( repository )
        {
            IndexWriter indexWriter = null;
            try
            {
                indexWriter = new IndexWriter( indexLocation, indexHandlers.getAnalyzer(), !exists() );
                indexWriter.setMaxFieldLength( MAX_FIELD_LENGTH );
    
                for ( Iterator i = records.iterator(); i.hasNext(); )
                {
                    LuceneRepositoryContentRecord record = (LuceneRepositoryContentRecord) i.next();
    
                    if ( record != null )
                    {
                        Term term = new Term( LuceneDocumentMaker.PRIMARY_KEY, record.getPrimaryKey() );
    
                        indexWriter.deleteDocuments( term );
    
                        Document document = indexHandlers.getConverter().convert( record );
    
                        indexWriter.addDocument( document );
                    }
                }
                indexWriter.optimize();
            }
            catch ( IOException e )
            {
                throw new RepositoryIndexException( "Error updating index: " + e.getMessage(), e );
            }
            finally
            {
                closeQuietly( indexWriter );
            }
        }
    }

    public void modifyRecord( LuceneRepositoryContentRecord record )
        throws RepositoryIndexException
    {
        synchronized( repository )
        {
            IndexWriter indexWriter = null;
            try
            {
                indexWriter = new IndexWriter( indexLocation, indexHandlers.getAnalyzer(), !exists() );
                indexWriter.setMaxFieldLength( MAX_FIELD_LENGTH );
    
                if ( record != null )
                {
                    Term term = new Term( LuceneDocumentMaker.PRIMARY_KEY, record.getPrimaryKey() );
    
                    indexWriter.deleteDocuments( term );
    
                    Document document = indexHandlers.getConverter().convert( record );
    
                    indexWriter.addDocument( document );
                }
                indexWriter.optimize();
            }
            catch ( IOException e )
            {
                throw new RepositoryIndexException( "Error updating index: " + e.getMessage(), e );
            }
            finally
            {
                closeQuietly( indexWriter );
            }
        }
    }
        

    private void addRecords( Collection records )
        throws RepositoryIndexException
    {
        synchronized( repository )
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
    }

    public void deleteRecords( Collection records )
        throws RepositoryIndexException
    {
        synchronized( repository )
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
    }
    
    public void deleteRecord( LuceneRepositoryContentRecord record )
        throws RepositoryIndexException
    {
        synchronized( repository )
        {
            if ( exists() )
            {
                IndexReader indexReader = null;
                try
                {
                    indexReader = IndexReader.open( indexLocation );    
                    
                    if ( record != null )
                    {
                        Term term = new Term( LuceneDocumentMaker.PRIMARY_KEY, record.getPrimaryKey() );
                        
                        indexReader.deleteDocuments( term );                            
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
    }
    
    
    public Collection getAllRecordKeys()
        throws RepositoryIndexException
    {
        return getAllFieldValues( LuceneDocumentMaker.PRIMARY_KEY );
    }

    private List getAllFieldValues( String fieldName )
        throws RepositoryIndexException
    {
        synchronized( repository )
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
    }
    
    public Searchable getSearchable()
        throws RepositoryIndexSearchException
    {
        try
        {
            IndexSearcher searcher = new IndexSearcher( indexLocation.getAbsolutePath() );
            return searcher;
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to open index: " + e.getMessage(), e );
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

    public QueryParser getQueryParser()
    {
        return this.indexHandlers.getQueryParser();
    }

    public static void closeSearchable( Searchable searchable )
    {
        if( searchable != null )
        {
            try
            {
                searchable.close();
            }
            catch ( IOException e )
            {
                // Ignore
            }
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

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }
    
    public Analyzer getAnalyzer()
    {
        return this.indexHandlers.getAnalyzer();
    }
    
    public LuceneEntryConverter getEntryConverter()
    {
        return this.indexHandlers.getConverter();
    }
}
