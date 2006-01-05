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
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.digest.Digester;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * Class used to index Artifact objects in a specified repository
 *
 * @author Edwin Punzalan
 */
public class ArtifactRepositoryIndex
    extends AbstractRepositoryIndex
{
    protected static final String FLD_NAME = "name";

    protected static final String FLD_GROUPID = "groupId";

    protected static final String FLD_ARTIFACTID = "artifactId";

    protected static final String FLD_VERSION = "version";

    protected static final String FLD_SHA1 = "sha1";

    protected static final String FLD_MD5 = "md5";

    protected static final String FLD_CLASSES = "classes";

    protected static final String FLD_PACKAGES = "packages";

    protected static final String FLD_FILES = "files";

    private static final String[] FIELDS =
        {FLD_NAME, FLD_GROUPID, FLD_ARTIFACTID, FLD_VERSION, FLD_SHA1, FLD_MD5, FLD_CLASSES, FLD_PACKAGES, FLD_FILES};

    private Analyzer analyzer;

    private Digester digester;

    public ArtifactRepositoryIndex( String indexPath, ArtifactRepository repository, Digester digester )
        throws RepositoryIndexException
    {
        super( repository, indexPath );
        this.digester = digester;
    }

    /**
     * method to get the Analyzer used to create indices
     *
     * @return the Analyzer object used to create the artifact indices
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
     * method for collecting the available index fields usable for searching
     *
     * @return index field names
     */
    public String[] getIndexFields()
    {
        return FIELDS;
    }

    /**
     * generic method for indexing
     *
     * @param obj the object to be indexed by this indexer
     */
    public void index( Object obj )
        throws RepositoryIndexException
    {
        if ( obj instanceof Artifact )
        {
            indexArtifact( (Artifact) obj );
        }
        else
        {
            throw new RepositoryIndexException(
                "This instance of indexer cannot index instances of " + obj.getClass().getName() );
        }
    }

    /**
     * method to index a given artifact
     *
     * @param artifact the Artifact object to be indexed
     */
    public void indexArtifact( Artifact artifact )
        throws RepositoryIndexException
    {
        if ( !isOpen() )
        {
            throw new RepositoryIndexException( "Unable to add artifact index on a closed index" );
        }

        StringBuffer classes = new StringBuffer();
        StringBuffer packages = new StringBuffer();
        StringBuffer files = new StringBuffer();

        String sha1sum;
        String md5sum;
        ZipFile jar;
        try
        {
            sha1sum = digester.createChecksum( artifact.getFile(), Digester.SHA1 );
            md5sum = digester.createChecksum( artifact.getFile(), Digester.MD5 );
            jar = new ZipFile( artifact.getFile() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RepositoryIndexException( "Unable to create a checksum", e );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryIndexException( "Error reading from artifact file", e );
        }
        catch ( ZipException e )
        {
            throw new RepositoryIndexException( "Error reading from artifact file", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error reading from artifact file", e );
        }

        for ( Enumeration entries = jar.entries(); entries.hasMoreElements(); )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if ( addIfClassEntry( entry, classes ) )
            {
                addClassPackage( entry.getName(), packages );
            }
            addFile( entry, files );
        }

        //@todo should some of these fields be Keyword instead of Text ?
        Document doc = new Document();
        doc.add( Field.Text( FLD_NAME, artifact.getFile().getName() ) );
        doc.add( Field.Text( FLD_GROUPID, artifact.getGroupId() ) );
        doc.add( Field.Text( FLD_ARTIFACTID, artifact.getArtifactId() ) );
        doc.add( Field.Text( FLD_VERSION, artifact.getVersion() ) );
        doc.add( Field.Text( FLD_SHA1, sha1sum ) );
        doc.add( Field.Text( FLD_MD5, md5sum ) );
        doc.add( Field.Text( FLD_CLASSES, classes.toString() ) );
        doc.add( Field.Text( FLD_PACKAGES, packages.toString() ) );
        doc.add( Field.Text( FLD_FILES, files.toString() ) );

        try
        {
            getIndexWriter().addDocument( doc );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error opening index", e );
        }
    }

    private boolean addIfClassEntry( ZipEntry entry, StringBuffer classes )
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
                String classname = name.substring( idx, name.length() - 6 );
                classes.append( classname ).append( "\n" );
                isAdded = true;
            }
        }

        return isAdded;
    }

    private boolean addClassPackage( String name, StringBuffer packages )
    {
        boolean isAdded = false;

        int idx = name.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            String packageName = name.substring( 0, idx ).replace( '/', '.' ) + "\n";
            if ( packages.indexOf( packageName ) < 0 )
            {
                packages.append( packageName ).append( "\n" );
            }
            isAdded = true;
        }

        return isAdded;
    }

    private boolean addFile( ZipEntry entry, StringBuffer files )
    {
        String name = entry.getName();
        int idx = name.lastIndexOf( '/' );
        if ( idx >= 0 )
        {
            name = name.substring( idx + 1 );
        }

        boolean isAdded = false;

        if ( files.indexOf( name + "\n" ) < 0 )
        {
            files.append( name ).append( "\n" );
            isAdded = true;
        }

        return isAdded;
    }
}
