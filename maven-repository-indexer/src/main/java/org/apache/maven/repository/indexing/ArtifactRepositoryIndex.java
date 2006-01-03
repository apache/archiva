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
 * @plexus.component role="org.apache.maven.repository.indexing.RepositoryIndex" role-hint="artifact" instantiation-strategy="per-lookup"
 * @todo I think we should merge with Abstract*. Don't see that there'd be multiple implementations based on this
 * @todo I think we should instantiate this based on a repository from a factory instead of making it a component of its own
 */
public class ArtifactRepositoryIndex
    extends AbstractRepositoryIndex
{
    private static final String NAME = "name";

    private static final String GROUPID = "groupId";

    private static final String ARTIFACTID = "artifactId";

    private static final String VERSION = "version";

    private static final String SHA1 = "sha1";

    private static final String MD5 = "md5";

    private static final String CLASSES = "classes";

    private static final String PACKAGES = "packages";

    private static final String FILES = "files";

    private static final String[] FIELDS = {NAME, GROUPID, ARTIFACTID, VERSION, SHA1, MD5, CLASSES, PACKAGES, FILES};

    private Analyzer analyzer;

    /** @plexus.requirement */
    private Digester digester;

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
        doc.add( Field.Text( NAME, artifact.getFile().getName() ) );
        doc.add( Field.Text( GROUPID, artifact.getGroupId() ) );
        doc.add( Field.Text( ARTIFACTID, artifact.getArtifactId() ) );
        doc.add( Field.Text( VERSION, artifact.getVersion() ) );
        doc.add( Field.Text( SHA1, sha1sum ) );
        doc.add( Field.Text( MD5, md5sum ) );
        doc.add( Field.Text( CLASSES, classes.toString() ) );
        doc.add( Field.Text( PACKAGES, packages.toString() ) );
        doc.add( Field.Text( FILES, files.toString() ) );

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
