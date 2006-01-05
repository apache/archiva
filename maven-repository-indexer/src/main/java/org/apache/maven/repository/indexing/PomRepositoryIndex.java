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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.Iterator;

/**
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

    private static final String[] FIELDS = {FLD_GROUPID, FLD_ARTIFACTID, FLD_VERSION, FLD_PACKAGING, FLD_LICENSE_URLS, FLD_DEPENDENCIES,
        FLD_PLUGINS_BUILD, FLD_PLUGINS_REPORT, FLD_PLUGINS_ALL};

    private Analyzer analyzer;

    public PomRepositoryIndex( ArtifactRepository repository, String indexPath )
        throws RepositoryIndexException
    {
        super( repository, indexPath );
    }

    public Analyzer getAnalyzer()
    {
        if ( analyzer == null )
        {
            analyzer = new SimpleAnalyzer();
        }

        return analyzer;
    }

    public void index( Object obj )
        throws RepositoryIndexException
    {
        if ( obj instanceof Model )
        {
            indexModel( (Model) obj );
        }
        else
        {
            throw new RepositoryIndexException(
                "This instance of indexer cannot index instances of " + obj.getClass().getName() );
        }
    }

    public String[] getIndexFields()
    {
        return FIELDS;
    }

    public void indexModel( Model pom )
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

        indexLicenseUrls( doc, pom.getLicenses().iterator() );
        indexDependencies( doc, pom.getDependencies().iterator() );
        indexPlugins( doc, FLD_PLUGINS_BUILD, pom.getBuild().getPlugins().iterator() );
        indexPlugins( doc, FLD_PLUGINS_REPORT, pom.getReporting().getPlugins().iterator() );
        indexPlugins( doc, FLD_PLUGINS_ALL, pom.getBuild().getPlugins().iterator() );
        indexPlugins( doc, FLD_PLUGINS_ALL, pom.getReporting().getPlugins().iterator() );

        try
        {
            getIndexWriter().addDocument( doc );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexException( "Error opening index", e );
        }
    }

    private void indexLicenseUrls( Document doc, Iterator licenses )
    {
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

    private void indexDependencies( Document doc, Iterator dependencies )
    {
        while ( dependencies.hasNext() )
        {
            Dependency dep = (Dependency) dependencies.next();
            String id = getId( dep.getGroupId(), dep.getArtifactId(), dep.getVersion() );
            doc.add( Field.Keyword( FLD_DEPENDENCIES, id ) );
        }
    }

    private void indexPlugins( Document doc, String field, Iterator plugins )
    {
        while ( plugins.hasNext() )
        {
            Plugin plugin = (Plugin) plugins.next();
            String id = getId( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
            doc.add( Field.Keyword( field, id ) );
        }
    }

    private String getId( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }
}
