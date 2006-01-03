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

    private IndexReader indexReader;

    private IndexWriter indexWriter;

    /**
     * method to encapsulate the optimize() method for lucene
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
     * method used to query the index status
     *
     * @return true if the index is open.
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
        catch ( IOException e )
        {
            throw new RepositoryIndexException( e.getMessage(), e );
        }
    }

    /**
     * method for opening the index directory for indexing operations
     */
    protected void open( String indexPath )
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

    protected IndexWriter getIndexWriter()
        throws IOException
    {
        if ( indexWriter == null )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), false );
        }
        return indexWriter;
    }

    private IndexReader getIndexReader()
        throws IOException
    {
        if ( indexReader == null )
        {
            indexReader = IndexReader.open( indexPath );
        }
        return indexReader;
    }

    /**
     * method for validating an index directory
     *
     * @throws RepositoryIndexException if the given indexPath is not valid for this type of RepositoryIndex
     */
    private void validateIndex()
        throws RepositoryIndexException, IOException
    {
        File indexDir = new File( indexPath );
        if ( IndexReader.indexExists( indexDir ) )
        {
            IndexReader indexReader = getIndexReader();
            if ( indexReader.numDocs() > 0 )
            {
                Collection fields = indexReader.getFieldNames();
                String[] indexFields = getIndexFields();
                for ( int idx = 0; idx < indexFields.length; idx++ )
                {
                    if ( !fields.contains( indexFields[idx] ) )
                    {
                        throw new RepositoryIndexException(
                            "The Field " + indexFields[idx] + " does not exist in " + "index path " + indexPath + "." );
                    }
                }
            }
            else
            {
                //getLogger().info( "Skipping index field validations for empty index." );
            }
        }
        else if ( !indexDir.exists() )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), true );
            //getLogger().info( "New index directory created in: " + indexDir.getAbsolutePath() );
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
