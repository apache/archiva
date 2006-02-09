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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Abstract class for RepositoryIndexers
 *
 * @author Edwin Punzalan
 */
public abstract class AbstractRepositoryIndex
    implements RepositoryIndex
{
    private String indexPath;

    private boolean indexOpen;

    private IndexWriter indexWriter;

    protected ArtifactRepository repository;

    protected boolean indexExists;

    private Analyzer analyzer;

    /**
     * Class constructor
     *
     * @param indexPath
     * @param repository
     * @throws RepositoryIndexException
     */
    protected AbstractRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException
    {
        this.repository = repository;
        this.indexPath = indexPath;
    }

    /**
     * Method to open the IndexWriter
     *
     * @throws RepositoryIndexException
     */
    public void open()
        throws RepositoryIndexException
    {
        try
        {
            if ( indexExists )
            {
                indexWriter = new IndexWriter( indexPath, getAnalyzer(), false );
            }
            else
            {
                indexWriter = new IndexWriter( indexPath, getAnalyzer(), true );
            }
        }
        catch ( IOException ie )
        {
            throw new RepositoryIndexException( ie );
        }
        indexOpen = true;
    }

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#optimize()
     */
    public void optimize()
        throws RepositoryIndexException
    {
        if ( !indexOpen )
        {
            throw new RepositoryIndexException( "Unable to optimize index on a closed index" );
        }

        try
        {
            indexWriter.optimize();
        }
        catch ( IOException ioe )
        {
            throw new RepositoryIndexException( "Failed to optimize index", ioe );
        }
    }

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#isOpen()
     */
    public boolean isOpen()
    {
        return indexOpen;
    }

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#close()
     */
    public void close()
        throws RepositoryIndexException
    {
        try
        {
            if ( indexWriter != null )
            {
                indexWriter.close();
                indexWriter = null;
            }

            indexOpen = false;
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( e.getMessage(), e );
        }
    }

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#getIndexPath()
     */
    public String getIndexPath()
    {
        return indexPath;
    }

    /**
     * Method to retrieve the lucene IndexWriter used in creating/updating the index
     *
     * @return the lucene IndexWriter object used to update the index
     * @throws IOException
     */
    protected IndexWriter getIndexWriter()
        throws IOException
    {
        if ( indexWriter == null )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), false );
        }
        return indexWriter;
    }

    /**
     * method for validating an index directory
     *
     * @param indexFields
     * @throws RepositoryIndexException if the given indexPath is not valid for this type of RepositoryIndex
     */
    protected void validateIndex( String[] indexFields )
        throws RepositoryIndexException, IOException
    {
        IndexReader indexReader = IndexReader.open( indexPath );
        try
        {
            if ( indexReader.numDocs() > 0 )
            {
                Collection fields = indexReader.getFieldNames();
                for ( int idx = 0; idx < indexFields.length; idx++ )
                {
                    if ( !fields.contains( indexFields[idx] ) )
                    {
                        throw new RepositoryIndexException(
                            "The Field " + indexFields[idx] + " does not exist in index " + indexPath + "." );
                    }
                }
            }
        }
        finally
        {
            indexReader.close();
        }
    }

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#getRepository()
     */
    public ArtifactRepository getRepository()
    {
        return repository;
    }

    /**
     * Delete the document(s) that contains the specified value on the specified field.
     *
     * @param field
     * @param value
     * @throws RepositoryIndexException
     * @throws IOException
     */
    protected void deleteDocument( String field, String value )
        throws RepositoryIndexException, IOException
    {
        IndexReader indexReader = null;
        try
        {
            indexReader = IndexReader.open( indexPath );
            indexReader.delete( new Term( field, value ) );
        }
        catch ( IOException ie )
        {
            throw new RepositoryIndexException( indexPath + "is not a valid directory." );
        }
        finally
        {
            if ( indexReader != null )
            {
                indexReader.close();
            }
        }
    }

    /**
     * Check if the index already exists.
     *
     * @throws IOException
     * @throws RepositoryIndexException
     */
    protected void checkIfIndexExists()
        throws IOException, RepositoryIndexException
    {
        File indexDir = new File( indexPath );

        if ( IndexReader.indexExists( indexDir ) )
        {
            indexExists = true;
        }
        else if ( !indexDir.exists() )
        {
            indexExists = false;
        }
        else if ( indexDir.isDirectory() )
        {
            throw new RepositoryIndexException( indexPath + " is not a valid index directory." );
        }
        else
        {
            throw new RepositoryIndexException( indexPath + " is not a directory." );
        }
    }

    /**
     * Checks if the object has already been indexed.
     *
     * @param object the object to be indexed.
     * @throws RepositoryIndexException
     * @throws IOException
     */
    abstract void isIndexed( Object object )
        throws RepositoryIndexException, IOException;

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#getAnalyzer()
     */
    public Analyzer getAnalyzer()
    {
        if ( analyzer == null )
        {
            analyzer = new ArtifactRepositoryIndexAnalyzer( new SimpleAnalyzer() );
        }

        return analyzer;
    }

    /**
     * @see RepositoryIndex#isKeywordField(String)
     */
    public boolean isKeywordField( String field )
    {
        return KEYWORD_FIELDS.contains( field );
    }
}
