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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.repository.digest.Digester;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class to create index entries for a given pom in a repository
 *
 * @author Edwin Punzalan
 */
public class PomRepositoryIndex
    extends AbstractRepositoryIndex
{
    protected static final String FLD_GROUPID = "groupId";

    protected static final String FLD_ARTIFACTID = "artifactId";

    protected static final String FLD_VERSION = "version";

    protected static final String FLD_PACKAGING = "packaging";

    protected static final String FLD_LICENSE_URLS = "license_urls";

    protected static final String FLD_DEPENDENCIES = "dependencies";

    protected static final String FLD_PLUGINS_BUILD = "plugins_build";

    protected static final String FLD_PLUGINS_REPORT = "plugins_report";

    protected static final String FLD_PLUGINS_ALL = "plugins_all";

    protected static final String FLD_SHA1 = "sha1";

    protected static final String FLD_MD5 = "md5";

    private static final String[] FIELDS = {FLD_GROUPID, FLD_ARTIFACTID, FLD_VERSION, FLD_PACKAGING, FLD_LICENSE_URLS,
        FLD_DEPENDENCIES, FLD_PLUGINS_BUILD, FLD_PLUGINS_REPORT, FLD_PLUGINS_ALL};

    private Analyzer analyzer;

    private Digester digester;

    private ArtifactFactory artifactFactory;

    private static final List KEYWORD_FIELDS = Arrays.asList(
        new String[]{FLD_LICENSE_URLS, FLD_DEPENDENCIES, FLD_PLUGINS_BUILD, FLD_PLUGINS_REPORT, FLD_PLUGINS_ALL} );

    /**
     * Class Constructor
     *
     * @param indexPath       the path where the index is available or will be made available
     * @param repository      the repository where objects indexed by this class resides
     * @param digester        the digester to be used for generating checksums
     * @param artifactFactory the factory for building artifact objects
     * @throws RepositoryIndexException
     */
    public PomRepositoryIndex( String indexPath, ArtifactRepository repository, Digester digester,
                               ArtifactFactory artifactFactory )
        throws RepositoryIndexException
    {
        super( indexPath, repository, FIELDS );
        this.digester = digester;
        this.artifactFactory = artifactFactory;
    }

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
     * Method to create the index fields for a Model object into the index
     *
     * @param pom the Model object to be indexed
     * @throws RepositoryIndexException
     */
    public void indexPom( Model pom )
        throws RepositoryIndexException
    {
        if ( !isOpen() )
        {
            throw new RepositoryIndexException( "Unable to add pom index on a closed index" );
        }

        Document doc = new Document();
        doc.add( Field.Text( FLD_GROUPID, pom.getGroupId() ) );
        doc.add( Field.Text( FLD_ARTIFACTID, pom.getArtifactId() ) );
        doc.add( Field.Text( FLD_VERSION, pom.getVersion() ) );
        doc.add( Field.Keyword( FLD_PACKAGING, pom.getPackaging() ) );

        Artifact artifact =
            artifactFactory.createBuildArtifact( pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), "pom" );
        File pomFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );
        doc.add( Field.Text( FLD_SHA1, getChecksum( Digester.SHA1, pomFile.getAbsolutePath() ) ) );
        doc.add( Field.Text( FLD_MD5, getChecksum( Digester.MD5, pomFile.getAbsolutePath() ) ) );

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
            doc.add( Field.Text( FLD_PLUGINS_BUILD, "" ) );
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
            doc.add( Field.Text( FLD_PLUGINS_REPORT, "" ) );
        }

        if ( !hasPlugins )
        {
            doc.add( Field.Text( FLD_PLUGINS_ALL, "" ) );
        }

        try
        {
            getIndexWriter().addDocument( doc );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error opening index", e );
        }
    }

    /**
     * @see RepositoryIndex#isKeywordField(String)
     */
    public boolean isKeywordField( String field )
    {
        return KEYWORD_FIELDS.contains( field );
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
                    doc.add( Field.Keyword( FLD_LICENSE_URLS, url ) );
                }
            }
        }
        else
        {
            doc.add( Field.Keyword( FLD_LICENSE_URLS, "" ) );
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
                doc.add( Field.Keyword( FLD_DEPENDENCIES, id ) );
            }
        }
        else
        {
            doc.add( Field.Keyword( FLD_DEPENDENCIES, "" ) );
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
            doc.add( Field.Keyword( field, id ) );
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
            doc.add( Field.Keyword( field, id ) );
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
        catch ( IOException e )
        {
            throw new RepositoryIndexException( e.getMessage(), e );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RepositoryIndexException( e.getMessage(), e );
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
}
