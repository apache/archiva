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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.digest.DigesterException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
        catch ( IOException e )
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

            if ( "pom".equals( artifact.getType() ) )
            {
                list.add( new Term( FLD_ID, POM + ":" + artifact.getId() ) );
            }
        }

        return list;
    }

    /**
     * Creates a list of Lucene documents, used for index additions
     *
     * @param artifactList
     * @return
     */
    private List getDocumentList( List artifactList )
    {
        List list = new ArrayList();

        for ( Iterator artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            Artifact artifact = (Artifact) artifacts.next();

            try
            {
                list.add( createDocument( artifact ) );
            }
            catch ( RepositoryIndexException e )
            {
                // TODO: log the problem and record it as a repository error
                // We log the problem, but do not add the document to the list to be added to the index
            }

            if ( "pom".equals( artifact.getType() ) )
            {
                try
                {
                    Model model = new MavenXpp3Reader().read( new FileReader( artifact.getFile() ) );

                    list.add( createDocument( artifact, model ) );
                }
                catch ( IOException e )
                {
                    // TODO: log the problem and record it as a repository error
                    // We log the problem, but do not add the document to the list to be added to the index
                }
                catch ( XmlPullParserException e )
                {
                    // TODO: log the problem and record it as a repository error
                    // We log the problem, but do not add the document to the list to be added to the index
                }
                catch ( RepositoryIndexException e )
                {
                    // TODO: log the problem and record it as a repository error
                    // We log the problem, but do not add the document to the list to be added to the index
                }
            }
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
        doc.add( createKeywordField( FLD_ID, ARTIFACT + ":" + artifact.getId() ) );
        doc.add( createTextField( FLD_NAME, artifact.getFile().getName() ) );
        doc.add( createTextField( FLD_GROUPID, artifact.getGroupId() ) );
        doc.add( createTextField( FLD_ARTIFACTID, artifact.getArtifactId() ) );
        doc.add( createTextField( FLD_VERSION, artifact.getVersion() ) );
        doc.add( createTextField( FLD_SHA1, sha1sum ) );
        doc.add( createTextField( FLD_MD5, md5sum ) );
        doc.add( createTextField( FLD_CLASSES, classes.toString() ) );
        doc.add( createTextField( FLD_PACKAGES, packages.toString() ) );
        doc.add( createTextField( FLD_FILES, files.toString() ) );
        doc.add( createUnindexedField( FLD_DOCTYPE, ARTIFACT ) );
        doc.add( createTextField( FLD_LASTUPDATE, "" ) );
        doc.add( createTextField( FLD_PLUGINPREFIX, "" ) );
        doc.add( createKeywordField( FLD_LICENSE_URLS, "" ) );
        doc.add( createKeywordField( FLD_DEPENDENCIES, "" ) );
        doc.add( createKeywordField( FLD_PLUGINS_REPORT, "" ) );
        doc.add( createKeywordField( FLD_PLUGINS_BUILD, "" ) );
        doc.add( createKeywordField( FLD_PLUGINS_ALL, "" ) );
        int i = artifact.getFile().getName().lastIndexOf( '.' );
        doc.add( createTextField( FLD_PACKAGING, artifact.getFile().getName().substring( i + 1 ) ) );

        return doc;
    }

    private static Field createUnindexedField( String name, String value )
    {
        return new Field( name, value, Field.Store.YES, Field.Index.NO );
    }

    private static Field createTextField( String name, String value )
    {
        return new Field( name, value, Field.Store.YES, Field.Index.TOKENIZED );
    }

    private static Field createKeywordField( String name, String value )
    {
        return new Field( name, value, Field.Store.YES, Field.Index.UN_TOKENIZED );
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

    public List enumerateGroupIds()
        throws IOException
    {
        IndexReader indexReader = IndexReader.open( getIndexPath() );

        Set groups = new HashSet();

        try
        {
            for ( int i = 0; i < indexReader.numDocs(); i++ )
            {
                Document doc = indexReader.document( i );
                groups.add( doc.getField( FLD_GROUPID ).stringValue() );
            }
        }
        finally
        {
            indexReader.close();
        }

        List sortedGroups = new ArrayList( groups );
        Collections.sort( sortedGroups );
        return sortedGroups;
    }

    public List getArtifacts( String groupId )
        throws IOException
    {
        IndexReader indexReader = IndexReader.open( getIndexPath() );

        Set artifactIds = new HashSet();

        try
        {
            for ( int i = 0; i < indexReader.numDocs(); i++ )
            {
                Document doc = indexReader.document( i );
                if ( doc.getField( FLD_GROUPID ).stringValue().equals( groupId ) )
                {
                    artifactIds.add( doc.getField( FLD_ARTIFACTID ).stringValue() );
                }
            }
        }
        finally
        {
            indexReader.close();
        }

        List sortedArtifactIds = new ArrayList( artifactIds );
        Collections.sort( sortedArtifactIds );
        return sortedArtifactIds;
    }

    public List getVersions( String groupId, String artifactId )
        throws IOException
    {
        IndexReader indexReader = IndexReader.open( getIndexPath() );

        Set versions = new HashSet();

        try
        {
            for ( int i = 0; i < indexReader.numDocs(); i++ )
            {
                Document doc = indexReader.document( i );
                if ( doc.getField( FLD_GROUPID ).stringValue().equals( groupId ) &&
                    doc.getField( FLD_ARTIFACTID ).stringValue().equals( artifactId ) )
                {
                    // DefaultArtifactVersion is used for correct ordering
                    versions.add( new DefaultArtifactVersion( doc.getField( FLD_VERSION ).stringValue() ) );
                }
            }
        }
        finally
        {
            indexReader.close();
        }

        List sortedVersions = new ArrayList( versions );
        Collections.sort( sortedVersions );
        return sortedVersions;
    }

    /**
     * Creates a Lucene Document from a Model; used for index additions
     *
     * @param pom
     * @return
     * @throws RepositoryIndexException
     */
    private Document createDocument( Artifact artifact, Model pom )
        throws RepositoryIndexException
    {
        String version = pom.getVersion();
        if ( version == null )
        {
            // It was inherited
            version = pom.getParent().getVersion();
            // TODO: do we need to use the general inheritence mechanism or do we only want to search within those defined in this pom itself?
            // I think searching just this one is adequate, and it is only necessary to inherit the version and group ID [BP]
        }

        String groupId = pom.getGroupId();
        if ( groupId == null )
        {
            groupId = pom.getParent().getGroupId();
        }

        Document doc = new Document();
        doc.add( createKeywordField( FLD_ID, POM + ":" + artifact.getId() ) );
        doc.add( createTextField( FLD_GROUPID, groupId ) );
        doc.add( createTextField( FLD_ARTIFACTID, pom.getArtifactId() ) );
        doc.add( createTextField( FLD_VERSION, version ) );
        doc.add( createKeywordField( FLD_PACKAGING, pom.getPackaging() ) );

        File pomFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );
        doc.add( createTextField( FLD_SHA1, getChecksum( Digester.SHA1, pomFile.getAbsolutePath() ) ) );
        doc.add( createTextField( FLD_MD5, getChecksum( Digester.MD5, pomFile.getAbsolutePath() ) ) );

        indexLicenseUrls( doc, pom );
        indexDependencies( doc, pom );

        boolean hasPlugins = false;
        if ( pom.getBuild() != null && pom.getBuild().getPlugins() != null && pom.getBuild().getPlugins().size() > 0 )
        {
            hasPlugins = true;
            indexPlugins( doc, FLD_PLUGINS_BUILD, pom.getBuild().getPlugins().iterator() );
            indexPlugins( doc, FLD_PLUGINS_ALL, pom.getBuild().getPlugins().iterator() );
        }
        else
        {
            doc.add( createTextField( FLD_PLUGINS_BUILD, "" ) );
        }

        if ( pom.getReporting() != null && pom.getReporting().getPlugins() != null &&
            pom.getReporting().getPlugins().size() > 0 )
        {
            hasPlugins = true;
            indexReportPlugins( doc, FLD_PLUGINS_REPORT, pom.getReporting().getPlugins().iterator() );
            indexReportPlugins( doc, FLD_PLUGINS_ALL, pom.getReporting().getPlugins().iterator() );
        }
        else
        {
            doc.add( createTextField( FLD_PLUGINS_REPORT, "" ) );
        }

        if ( !hasPlugins )
        {
            doc.add( createTextField( FLD_PLUGINS_ALL, "" ) );
        }
        doc.add( createUnindexedField( FLD_DOCTYPE, POM ) );
        // TODO: do we need to add all these empty fields?
        doc.add( createTextField( FLD_PLUGINPREFIX, "" ) );
        doc.add( createTextField( FLD_LASTUPDATE, "" ) );
        doc.add( createTextField( FLD_NAME, "" ) );
        doc.add( createTextField( FLD_CLASSES, "" ) );
        doc.add( createKeywordField( FLD_PACKAGES, "" ) );
        doc.add( createTextField( FLD_FILES, "" ) );
        return doc;
    }

    /**
     * Method to index license urls found inside the passed pom
     *
     * @param doc the index object to create the fields for the license urls
     * @param pom the Model object to be indexed
     */
    private void indexLicenseUrls( Document doc, Model pom )
    {
        List licenseList = pom.getLicenses();
        if ( licenseList != null && licenseList.size() > 0 )
        {
            Iterator licenses = licenseList.iterator();
            while ( licenses.hasNext() )
            {
                License license = (License) licenses.next();
                String url = license.getUrl();
                if ( StringUtils.isNotEmpty( url ) )
                {
                    doc.add( createKeywordField( FLD_LICENSE_URLS, url ) );
                }
            }
        }
        else
        {
            doc.add( createKeywordField( FLD_LICENSE_URLS, "" ) );
        }
    }

    /**
     * Method to index declared dependencies found inside the passed pom
     *
     * @param doc the index object to create the fields for the dependencies
     * @param pom the Model object to be indexed
     */
    private void indexDependencies( Document doc, Model pom )
    {
        List dependencyList = pom.getDependencies();
        if ( dependencyList != null && dependencyList.size() > 0 )
        {
            Iterator dependencies = dependencyList.iterator();
            while ( dependencies.hasNext() )
            {
                Dependency dep = (Dependency) dependencies.next();
                String id = getId( dep.getGroupId(), dep.getArtifactId(), dep.getVersion() );
                doc.add( createKeywordField( FLD_DEPENDENCIES, id ) );
            }
        }
        else
        {
            doc.add( createKeywordField( FLD_DEPENDENCIES, "" ) );
        }
    }

    /**
     * Method to index plugins to a specified index field
     *
     * @param doc     the index object to create the fields for the plugins
     * @param field   the index field to store the passed plugin
     * @param plugins the iterator to the list of plugins to be indexed
     */
    private void indexPlugins( Document doc, String field, Iterator plugins )
    {
        while ( plugins.hasNext() )
        {
            Plugin plugin = (Plugin) plugins.next();
            String id = getId( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
            doc.add( createKeywordField( field, id ) );
        }
    }

    /**
     * Method to index report plugins to a specified index field
     *
     * @param doc     the index object to create the fields for the report plugins
     * @param field   the index field to store the passed report plugin
     * @param plugins the iterator to the list of report plugins to be indexed
     */
    private void indexReportPlugins( Document doc, String field, Iterator plugins )
    {
        while ( plugins.hasNext() )
        {
            ReportPlugin plugin = (ReportPlugin) plugins.next();
            String id = getId( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
            doc.add( createKeywordField( field, id ) );
        }
    }

    /**
     * Method to generate the computed checksum of an existing file using the specified algorithm.
     *
     * @param algorithm the algorithm to be used to generate the checksum
     * @param file      the file to match the generated checksum
     * @return a string representing the checksum
     * @throws RepositoryIndexException
     */
    private String getChecksum( String algorithm, String file )
        throws RepositoryIndexException
    {
        try
        {
            return digester.createChecksum( new File( file ), algorithm );
        }
        catch ( DigesterException e )
        {
            throw new RepositoryIndexException( "Failed to create checksum", e );
        }
    }

    /**
     * Method to create the unique artifact id to represent the artifact in the repository
     *
     * @param groupId    the artifact groupId
     * @param artifactId the artifact artifactId
     * @param version    the artifact version
     * @return the String id to uniquely represent the artifact
     */
    private String getId( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    public void deleteArtifact( Artifact artifact )
        throws IOException, RepositoryIndexException
    {
        deleteDocuments( getTermList( Collections.singletonList( artifact ) ) );
    }
}
