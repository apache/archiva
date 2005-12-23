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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 *
 * @author Edwin Punzalan
 */
public abstract class AbstractRepositoryIndexer
    implements RepositoryIndexer
{
    protected String indexPath;
    protected boolean indexOpen;
    protected IndexReader indexReader;
    protected IndexWriter indexWriter;
    
    public void optimize()
        throws RepositoryIndexerException
    {
        if ( !isOpen() )
        {
            throw new RepositoryIndexerException( "Unable to optimize index on a closed index" );
        }

        try
        {
            indexWriter.optimize();
        }
        catch ( IOException ioe )
        {
            throw new RepositoryIndexerException( "Failed to optimize index", ioe );
        }
    }

    public boolean isOpen()
    {
        return indexOpen;
    }
    
    public void close() 
        throws RepositoryIndexerException
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
            throw new RepositoryIndexerException( e );
        }
    }

    public void open()
        throws RepositoryIndexerException
    {
        try
        {
            validateIndex();
        }
        catch ( Exception e )
        {
            throw new RepositoryIndexerException( e );
        }
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
    
    protected Analyzer getAnalyzer()
    {
        return new ArtifactRepositoryIndexAnalyzer( new SimpleAnalyzer() );
    }

    protected void validateIndex()
        throws RepositoryIndexerException
    {
        File indexDir = new File( indexPath );
        if ( indexDir.exists() )
        {
            if ( indexDir.isDirectory() )
            {
                if ( indexDir.listFiles().length > 1 )
                {
                    try
                    {
                        getIndexReader();
                        Collection fields = indexReader.getFieldNames();
                        String[] indexFields = getIndexFields();
                        for( int idx=0; idx<indexFields.length; idx++ )
                        {
                            if ( !fields.contains( indexFields[ idx ] ) )
                            {
                                throw new RepositoryIndexerException( "The Field " + indexFields[ idx ] + " does not exist in " +
                                        "index path " + indexPath + "." );
                            }
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new RepositoryIndexerException( e );
                    }
                }
                else
                {
                    System.out.println( "Skipping validation of an empty index in: " + indexDir.getAbsolutePath() );
                }
            }
            else
            {
                throw new RepositoryIndexerException( "Specified index path is not a directory: " + 
                                                      indexDir.getAbsolutePath() );
            }
        }
        else
        {
            try
            {
                indexWriter = new IndexWriter( indexPath, getAnalyzer(), true );
                System.out.println( "New index directory created in: " + indexDir.getAbsolutePath() );
            }
            catch( Exception e )
            {
                throw new RepositoryIndexerException( e );
            }
        }

        indexOpen = true;
    }
}
