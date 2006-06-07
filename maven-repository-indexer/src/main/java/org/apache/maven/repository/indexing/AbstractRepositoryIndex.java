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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.zip.ZipEntry;

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

    private Analyzer analyzer;

    /**
     * Class constructor
     *
     * @param indexPath
     * @param repository
     */
    protected AbstractRepositoryIndex( String indexPath, ArtifactRepository repository )
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
            if ( indexExists() )
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
     * @return true if the index already exists
     * @throws RepositoryIndexException
     */
    protected boolean indexExists()
        throws RepositoryIndexException
    {
        File indexDir = new File( indexPath );

        if ( IndexReader.indexExists( indexDir ) )
        {
            return true;
        }
        else if ( !indexDir.exists() )
        {
            return false;
        }
        else if ( indexDir.isDirectory() )
        {
            if ( indexDir.listFiles().length > 1 )
            {
                throw new RepositoryIndexException( indexPath + " is not a valid index directory." );
            }
            else
            {
                return false;
            }
        }
        else
        {
            throw new RepositoryIndexException( indexPath + " is not a directory." );
        }
    }

    /**
     * Checks if the object has already been indexed and deletes it if it is.
     *
     * @param object the object to be indexed.
     * @throws RepositoryIndexException
     * @throws IOException
     */
    abstract void deleteIfIndexed( Object object )
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

    /**
     * Method to test a zip entry if it is a java class, and adds it to the classes buffer
     *
     * @param entry   the zip entry to test for java class
     * @param classes the String buffer to add the java class if the test result as true
     * @return true if the zip entry is a java class and was successfully added to the buffer
     */
    protected boolean addIfClassEntry( ZipEntry entry, StringBuffer classes )
    {
        boolean isAdded = false;

        String name = entry.getName();
        if ( name.endsWith( ".class" ) )
        {
            // TODO verify if class is public or protected
            if ( name.lastIndexOf( "$" ) == -1 )
            {
                int idx = name.lastIndexOf( '/' );
                if ( idx < 0 )
                {
                    idx = 0;
                }
                String classname = name.substring( idx + 1, name.length() - 6 );
                classes.append( classname ).append( "\n" );
                isAdded = true;
            }
        }

        return isAdded;
    }

    private static class ArtifactRepositoryIndexAnalyzer
        extends Analyzer
    {
        private Analyzer defaultAnalyzer;

        /**
         * constructor to for this analyzer
         *
         * @param defaultAnalyzer the analyzer to use as default for the general fields of the artifact indeces
         */
        ArtifactRepositoryIndexAnalyzer( Analyzer defaultAnalyzer )
        {
            this.defaultAnalyzer = defaultAnalyzer;
        }

        /**
         * Method called by lucence during indexing operations
         *
         * @param fieldName the field name that the lucene object is currently processing
         * @param reader    a Reader object to the index stream
         * @return an analyzer to specific to the field name or the default analyzer if none is present
         */
        public TokenStream tokenStream( String fieldName, Reader reader )
        {
            TokenStream tokenStream;

            if ( RepositoryIndex.FLD_VERSION.equals( fieldName ) || RepositoryIndex.FLD_LASTUPDATE.equals( fieldName ) )
            {
                tokenStream = new VersionTokenizer( reader );
            }
            else
            {
                tokenStream = defaultAnalyzer.tokenStream( fieldName, reader );
            }

            return tokenStream;
        }
    }

    /**
     * Class used to tokenize an artifact's version.
     */
    private static class VersionTokenizer
        extends CharTokenizer
    {
        /**
         * Constructor with the required reader to the index stream
         *
         * @param reader the Reader object of the index stream
         */
        VersionTokenizer( Reader reader )
        {
            super( reader );
        }

        /**
         * method that lucene calls to check tokenization of a stream character
         *
         * @param character char currently being processed
         * @return true if the char is a token, false if the char is a stop char
         */
        protected boolean isTokenChar( char character )
        {
            return character != '.' && character != '-';
        }
    }
}
