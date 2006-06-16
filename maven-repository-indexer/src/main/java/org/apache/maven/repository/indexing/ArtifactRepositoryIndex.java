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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.digest.DigesterException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * Class used to index Artifact objects in a specific repository
 *
 * @author Edwin Punzalan
 */
public class ArtifactRepositoryIndex
    extends AbstractRepositoryIndex
{
    private Digester digester;

    /**
     * Class constructor
     *
     * @param indexPath  the path where the lucene index will be created/updated.
     * @param repository the repository where the indexed artifacts are located
     * @param digester   the digester object to generate the checksum strings
     */
    public ArtifactRepositoryIndex( File indexPath, ArtifactRepository repository, Digester digester )
        throws RepositoryIndexException
    {
        super( indexPath, repository );
        this.digester = digester;
    }

    /**
     * Indexes the artifacts found within the specified list.  Deletes existing indices for the same artifacts first,
     * before proceeding on adding them into the index.
     *
     * @param artifactList
     * @throws RepositoryIndexException
     */
    public void indexArtifacts( List artifactList )
        throws RepositoryIndexException
    {
        try
        {
            deleteDocuments( getTermList( artifactList ) );
        }
        catch( IOException e )
        {
            throw new RepositoryIndexException( "Failed to delete an index document", e );
        }

        addDocuments( getDocumentList( artifactList ) );
    }

    /**
     * Creates a list of Lucene Term object used in index deletion
     *
     * @param artifactList
     * @return List of Term object
     */
    private List getTermList( List artifactList )
    {
        List list = new ArrayList();

        for ( Iterator artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            Artifact artifact = (Artifact) artifacts.next();

            list.add( new Term( FLD_ID, ARTIFACT + ":" + artifact.getId() ) );
        }

        return list;
    }

    /**
     * Creates a list of Lucene documents, used for index additions
     *
     * @param artifactList
     * @return
     * @throws RepositoryIndexException
     */
    private List getDocumentList( List artifactList )
        throws RepositoryIndexException
    {
        List list = new ArrayList();

        for ( Iterator artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            Artifact artifact = (Artifact) artifacts.next();

            list.add( createDocument( artifact ) );
        }

        return list;
    }

    /**
     * Method to index a given artifact
     *
     * @param artifact the Artifact object to be indexed
     * @throws RepositoryIndexException
     */
    public void indexArtifact( Artifact artifact )
        throws RepositoryIndexException
    {
        indexArtifacts( Collections.singletonList( artifact ) );
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
        StringBuffer classes = new StringBuffer();
        StringBuffer packages = new StringBuffer();
        StringBuffer files = new StringBuffer();

        String sha1sum;
        String md5sum;
        try
        {
            sha1sum = digester.createChecksum( artifact.getFile(), Digester.SHA1 );
            md5sum = digester.createChecksum( artifact.getFile(), Digester.MD5 );
        }
        catch ( DigesterException e )
        {
            throw new RepositoryIndexException( "Unable to create a checksum", e );
        }

        try
        {
            // TODO: improve
            if ( "jar".equals( artifact.getType() ) )
            {
                ZipFile jar = new ZipFile( artifact.getFile() );

                for ( Enumeration entries = jar.entries(); entries.hasMoreElements(); )
                {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if ( addIfClassEntry( entry, classes ) )
                    {
                        addClassPackage( entry.getName(), packages );
                    }
                    addFile( entry, files );
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

        Document doc = new Document();
        doc.add( Field.Keyword( FLD_ID, ARTIFACT + ":" + artifact.getId() ) );
        doc.add( Field.Text( FLD_NAME, artifact.getFile().getName() ) );
        doc.add( Field.Text( FLD_GROUPID, artifact.getGroupId() ) );
        doc.add( Field.Text( FLD_ARTIFACTID, artifact.getArtifactId() ) );
        doc.add( Field.Text( FLD_VERSION, artifact.getVersion() ) );
        doc.add( Field.Text( FLD_SHA1, sha1sum ) );
        doc.add( Field.Text( FLD_MD5, md5sum ) );
        doc.add( Field.Text( FLD_CLASSES, classes.toString() ) );
        doc.add( Field.Text( FLD_PACKAGES, packages.toString() ) );
        doc.add( Field.Text( FLD_FILES, files.toString() ) );
        doc.add( Field.UnIndexed( FLD_DOCTYPE, ARTIFACT ) );
        doc.add( Field.Text( FLD_LASTUPDATE, "" ) );
        doc.add( Field.Text( FLD_PLUGINPREFIX, "" ) );
        doc.add( Field.Keyword( FLD_LICENSE_URLS, "" ) );
        doc.add( Field.Keyword( FLD_DEPENDENCIES, "" ) );
        doc.add( Field.Keyword( FLD_PLUGINS_REPORT, "" ) );
        doc.add( Field.Keyword( FLD_PLUGINS_BUILD, "" ) );
        doc.add( Field.Keyword( FLD_PLUGINS_ALL, "" ) );
        int i = artifact.getFile().getName().lastIndexOf( '.' );
        doc.add( Field.Text( FLD_PACKAGING, artifact.getFile().getName().substring( i + 1 ) ) );

        return doc;
    }


    /**
     * Method to add a class package to the buffer of packages
     *
     * @param name     the complete path name of the class
     * @param packages the packages buffer
     */
    private void addClassPackage( String name, StringBuffer packages )
    {
        int idx = name.lastIndexOf( '/' );
        if ( idx > 0 )
        {
            String packageName = name.substring( 0, idx ).replace( '/', '.' ) + "\n";
            if ( packages.indexOf( packageName ) < 0 )
            {
                packages.append( packageName ).append( "\n" );
            }
        }
    }

    /**
     * Method to add the zip entry as a file list
     *
     * @param entry the zip entry to be added
     * @param files the buffer of files to update
     */
    private void addFile( ZipEntry entry, StringBuffer files )
    {
        String name = entry.getName();
        int idx = name.lastIndexOf( '/' );
        if ( idx >= 0 )
        {
            name = name.substring( idx + 1 );
        }

        if ( files.indexOf( name + "\n" ) < 0 )
        {
            files.append( name ).append( "\n" );
        }
    }
}
