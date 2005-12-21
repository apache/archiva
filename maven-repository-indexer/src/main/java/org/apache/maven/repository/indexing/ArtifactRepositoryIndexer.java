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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 *
 * @author Edwin Punzalan
 */
public class ArtifactRepositoryIndexer
    extends AbstractRepositoryIndexer
{
    private static final String NAME = "name";
    private static final String GROUPID = "groupId";
    private static final String ARTIFACTID = "artifactId";
    private static final String VERSION = "version";
    private static final String SHA1 = "sha1";
    private static final String MD5 = "md5";
    private static final String CLASSES = "classes";
    private static final String PACKAGES = "packages";
    
    private static final String[] FIELDS = { NAME, GROUPID, ARTIFACTID, VERSION, SHA1, MD5, CLASSES, PACKAGES };
    
    ArtifactRepository repository;
    
    public ArtifactRepositoryIndexer( ArtifactRepository repository, String path )
        throws RepositoryIndexerException
    {
        this.repository = repository;
        indexPath = path;
        validateIndex();
    }

    public void addArtifactIndex( Artifact artifact )
        throws RepositoryIndexerException
    {
        try
        {
            getIndexWriter();

            Document doc = new Document();
            doc.add( Field.Text( NAME, repository.pathOf( artifact ) ) );
            doc.add( Field.Text( GROUPID, artifact.getGroupId() ) );
            doc.add( Field.Text( ARTIFACTID, artifact.getArtifactId() ) );
            doc.add( Field.Text( VERSION, artifact.getVersion() ) );
            doc.add( Field.Text( SHA1, getSha1( artifact ) ) );
            doc.add( Field.Text( MD5, getMd5( artifact ) ) );
            doc.add( Field.Text( CLASSES, getClasses( artifact ) ) );
            doc.add( Field.Text( PACKAGES, getPackages( artifact ) ) );
            indexWriter.addDocument( doc );
        }
        catch( Exception e )
        {
            throw new RepositoryIndexerException( e );
        }
    }

    public void optimize()
        throws RepositoryIndexerException
    {
        try
        {
            indexWriter.optimize();
        }
        catch ( IOException ioe )
        {
            throw new RepositoryIndexerException( "Failed to optimize index", ioe );
        }
    }

    private String getSha1( Artifact artifact )
        throws FileNotFoundException, IOException, NoSuchAlgorithmException
    {
        FileInputStream fIn = new FileInputStream( artifact.getFile() );
        return new String( getChecksum( fIn, "SHA-1" ) );
    }
    
    private String getMd5( Artifact artifact )
        throws FileNotFoundException, IOException, NoSuchAlgorithmException
    {
        FileInputStream fIn = new FileInputStream( artifact.getFile() );
        return new String( getChecksum( fIn, "MD5" ) );
    }

    private byte[] getChecksum( InputStream inStream, String algorithm )
        throws IOException, NoSuchAlgorithmException
    {
        byte[] buffer = new byte[ 256 ];
        MessageDigest complete = MessageDigest.getInstance( algorithm );
        int numRead;
        do
        {
            numRead = inStream.read( buffer );
            if ( numRead > 0 )
            {
                complete.update( buffer, 0, numRead );
            }
        }
        while ( numRead != -1 );
        inStream.close();

        return complete.digest();
    }

    private String getClasses( Artifact artifact )
        throws IOException, ZipException
    {
        StringBuffer sb = new StringBuffer();

        ZipFile jar = new ZipFile( artifact.getFile() );
        for( Enumeration en = jar.entries(); en.hasMoreElements(); )
        {
            ZipEntry e = ( ZipEntry ) en.nextElement();
            String name = e.getName();
            if( name.endsWith( ".class") )
            {
                // TODO verify if class is public or protected
                // TODO skipp all inner classes for now
                if( name.lastIndexOf( "$" ) == -1)
                {
                    int idx = name.lastIndexOf( '/' );
                    if ( idx < 0 ) idx = 0;
                    sb.append( name.substring( idx, name.length() - 6 ) ).append( "\n" );
                }
            }
        }

        return sb.toString();
    }

    private String getPackages( Artifact artifact )
        throws IOException, ZipException
    {
        StringBuffer sb = new StringBuffer();

        ZipFile jar = new ZipFile( artifact.getFile() );
        for( Enumeration en = jar.entries(); en.hasMoreElements(); )
        {
            ZipEntry e = ( ZipEntry ) en.nextElement();
            String name = e.getName();
            //only include packages with accompanying classes
            if ( name.endsWith( ".class" ) )
            {
                int idx = name.lastIndexOf( '/' );
                if ( idx > 0 )
                {
                    String packageName = name.substring( 0, idx ).replace( '/', '.' ) + "\n";
                    if ( sb.indexOf( packageName ) < 0 )
                    {
                        sb.append( packageName ).append( "\n" );
                    }
                }
            }
        }

        return sb.toString();
    }

    private void validateIndex()
        throws RepositoryIndexerException
    {
        try
        {
            getIndexReader();
            Collection fields = indexReader.getFieldNames();
            for( int idx=0; idx<FIELDS.length; idx++ )
            {
                if ( !fields.contains( FIELDS[ idx ] ) )
                {
                    throw new RepositoryIndexerException( "The Field " + FIELDS[ idx ] + " does not exist in index path " +
                            indexPath + "." );
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexerException( e );
        }
    }
}
