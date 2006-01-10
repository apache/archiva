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

    /**
     * Class constructor
     *
     * @param indexPath
     * @param repository
     * @param indexFields
     * @throws RepositoryIndexException
     */
    protected AbstractRepositoryIndex( String indexPath, ArtifactRepository repository, String[] indexFields )
        throws RepositoryIndexException
    {
        this.repository = repository;
        this.indexPath = indexPath;

        try
        {
            validateIndex( indexFields );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( e );
        }
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
    private void validateIndex( String[] indexFields )
        throws RepositoryIndexException, IOException
    {
        File indexDir = new File( indexPath );
        if ( IndexReader.indexExists( indexDir ) )
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
        else if ( !indexDir.exists() )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), true );
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

    /**
     * @see org.apache.maven.repository.indexing.RepositoryIndex#getRepository()
     */
    public ArtifactRepository getRepository()
    {
        return repository;
    }
}
