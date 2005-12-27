package org.apache.maven.repository.indexing;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 * Abstract class for RepositoryIndexers
 *
 * @author Edwin Punzalan
 */
public abstract class AbstractRepositoryIndex
    implements RepositoryIndex
{
    protected String indexPath;
    protected boolean indexOpen;
    protected IndexReader indexReader;
    protected IndexWriter indexWriter;
    
    /**
     * method to encapsulate the optimize() method for lucene
     */
    public void optimize()
        throws RepositoryIndexException
    {
        if ( !isOpen() )
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
     * method used to query the index status
     *
     * @param true if the index is open.
     */
    public boolean isOpen()
    {
        return indexOpen;
    }
    
    /**
     * method used to close all open streams to the index directory
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

            if ( indexReader != null )
            {
                indexReader.close();
                indexReader = null;
            }

            indexOpen = false;
        }
        catch ( Exception e )
        {
            throw new RepositoryIndexException( e );
        }
    }

    /**
     * method for opening the index directory for indexing operations
     */
    public void open( String indexPath )
        throws RepositoryIndexException
    {
        try
        {
            this.indexPath = indexPath;
            validateIndex();
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( e );
        }
    }
    
    public String getIndexPath()
    {
        return indexPath;
    }

    protected void getIndexWriter()
        throws IOException
    {
        if ( indexWriter == null )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), false );
        }
    }

    protected void getIndexReader()
        throws IOException
    {
        if ( indexReader == null )
        {
            indexReader = IndexReader.open( indexPath );
        }
    }
    
    /**
     * method for validating an index directory
     * 
     * @throws RepositoryIndexException if the given indexPath is not valid for this type of RepositoryIndex
     */
    protected void validateIndex()
        throws RepositoryIndexException, IOException
    {
        File indexDir = new File( indexPath );
        if ( IndexReader.indexExists( indexDir ) )
        {
            getIndexReader();
            if ( indexReader.numDocs() > 0 )
            {
                Collection fields = indexReader.getFieldNames();
                String[] indexFields = getIndexFields();
                for( int idx=0; idx<indexFields.length; idx++ )
                {
                    if ( !fields.contains( indexFields[ idx ] ) )
                    {
                        throw new RepositoryIndexException( "The Field " + indexFields[ idx ] + " does not exist in " +
                                "index path " + indexPath + "." );
                    }
                }
            }
            else
            {
                System.out.println("Skipping index field validations for empty index." );
            }
        }
        else if ( !indexDir.exists() )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), true );
            System.out.println( "New index directory created in: " + indexDir.getAbsolutePath() );
        }
        else if ( indexDir.isDirectory() )
        {
            throw new RepositoryIndexException( indexPath + " is not a valid index directory." );
        }
        else
        {
            throw new RepositoryIndexException( indexPath + " is not a directory." );
        }

        indexOpen = true;
    }
}
