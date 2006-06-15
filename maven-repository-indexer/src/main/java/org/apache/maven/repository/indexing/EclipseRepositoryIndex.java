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
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.digest.Digester;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author Edwin Punzalan
 */
public class EclipseRepositoryIndex
    extends AbstractRepositoryIndex
{
    private static final String JAR_NAME = "j";

    private static final String JAR_SIZE = "s";

    private static final String JAR_DATE = "d";

    private static final String NAMES = "c";

    private static final String MD5 = "m";

    private Digester digester;


    /**
     * Class constructor
     *
     * @param indexPath  the path where the lucene index will be created/updated.
     * @param repository the repository where the indexed artifacts are located
     * @param digester   the digester object to generate the checksum strings
     */
    public EclipseRepositoryIndex( File indexPath, ArtifactRepository repository, Digester digester )
        throws RepositoryIndexException
    {
        super( indexPath, repository );

        this.digester = digester;
    }

    /**
     * @see AbstractRepositoryIndex#getAnalyzer()
     */
    public Analyzer getAnalyzer()
    {
        return new EclipseIndexAnalyzer( new SimpleAnalyzer() );
    }

    /**
     * Indexes the artifacts inside the provided list
     *
     * @param artifactList
     * @throws RepositoryIndexException
     */
    public void indexArtifacts( List artifactList )
        throws RepositoryIndexException
    {
        List docs = new ArrayList();

        for ( Iterator artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            Artifact artifact = (Artifact) artifacts.next();

            Document doc = createDocument( artifact );

            if ( doc != null )
            {
                docs.add( doc );
            }
        }

        addDocuments( docs );
    }

    /**
     * Method to index a given artifact for use with the eclipse plugin
     *
     * @param artifact the Artifact object to be indexed
     * @throws RepositoryIndexException
     */
    public void indexArtifact( Artifact artifact )
        throws RepositoryIndexException
    {
        Document doc = createDocument( artifact );
        if ( doc != null )
        {
            addDocuments( Collections.singletonList( doc ) );
        }
    }

    /**
     * Creates a Lucene Document from an artifact; used for index additions
     *
     * @param artifact
     * @return
     * @throws RepositoryIndexException
     */
    private Document createDocument( Artifact artifact )
        throws RepositoryIndexException
    {
        Document doc = null;

        File artifactFile = artifact.getFile();
        if ( artifactFile != null && artifactFile.getName().endsWith( ".jar" ) && artifactFile.exists() )
        {
            String md5;
            try
            {
                md5 = digester.createChecksum( artifactFile, "MD5" );
            }
            catch ( FileNotFoundException e )
            {
                throw new RepositoryIndexException( "Unable to compute checksum.", e );
            }
            catch ( NoSuchAlgorithmException e )
            {
                throw new RepositoryIndexException( "Unable to compute checksum.", e );
            }
            catch ( IOException e )
            {
                throw new RepositoryIndexException( "Unable to compute checksum.", e );
            }

            StringBuffer classes;
            try
            {
                // TODO: improve
                classes = new StringBuffer();
                if ( "jar".equals( artifact.getType() ) )
                {
                    ZipFile jar = new ZipFile( artifact.getFile() );

                    for ( Enumeration entries = jar.entries(); entries.hasMoreElements(); )
                    {
                        ZipEntry entry = (ZipEntry) entries.nextElement();
                        addIfClassEntry( entry, classes );
                    }
                }
            }
            catch ( ZipException e )
            {
                throw new RepositoryIndexException( "Error reading from artifact file: " + artifact.getFile(), e );
            }
            catch ( IOException e )
            {
                throw new RepositoryIndexException( "Error reading from artifact file", e );
            }

            doc = new Document();
            doc.add( Field.Text( MD5, md5 ) );
            doc.add( Field.Text( JAR_NAME, artifactFile.getName() ) );
            doc.add( Field.Text( JAR_DATE, DateField.timeToString( artifactFile.lastModified() ) ) );
            doc.add( Field.Text( JAR_SIZE, Long.toString( artifactFile.length() ) ) );
            doc.add( Field.Text( NAMES, classes.toString() ) );
        }

        return doc;
    }

    /**
     * method to create an archived copy of the index contents
     *
     * @return File object to the archive
     * @throws IOException
     */
    public File getCompressedCopy()
        throws IOException
    {
        File indexPath = getIndexPath();
        String name = indexPath.getName();

        File outputFile = new File( indexPath.getParent(), name + ".zip" );
        FileUtils.fileDelete( outputFile.getAbsolutePath() );

        ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( outputFile ) );
        zos.setLevel( 9 );

        File[] files = indexPath.listFiles();
        try
        {
            for ( int i = 0; i < files.length; i++ )
            {
                writeFile( zos, files[i] );
            }
        }
        finally
        {
            zos.close();
        }

        return outputFile;
    }

    private static void writeFile( ZipOutputStream zos, File file )
        throws IOException
    {
        ZipEntry e = new ZipEntry( file.getName() );
        zos.putNextEntry( e );

        FileInputStream is = new FileInputStream( file );
        try
        {
            IOUtil.copy( is, zos );
        }
        finally
        {
            is.close();
        }
        zos.flush();

        zos.closeEntry();
    }

    /**
     * Class used to analyze the lucene index
     */
    private static class EclipseIndexAnalyzer
        extends Analyzer
    {
        private Analyzer defaultAnalyzer;

        /**
         * constructor to for this analyzer
         *
         * @param defaultAnalyzer the analyzer to use as default for the general fields of the artifact indeces
         */
        EclipseIndexAnalyzer( Analyzer defaultAnalyzer )
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

            if ( "s".equals( fieldName ) )
            {
                tokenStream = new EclipseIndexTokenizer( reader );
            }
            else
            {
                tokenStream = defaultAnalyzer.tokenStream( fieldName, reader );
            }

            return tokenStream;
        }
    }

    /**
     * Class used to tokenize the eclipse index
     */
    private static class EclipseIndexTokenizer
        extends CharTokenizer
    {
        /**
         * Constructor with the required reader to the index stream
         *
         * @param reader the Reader object of the index stream
         */
        EclipseIndexTokenizer( Reader reader )
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
            return true;
        }
    }
}
