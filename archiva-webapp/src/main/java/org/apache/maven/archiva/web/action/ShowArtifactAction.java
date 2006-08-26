package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork.ActionSupport;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.record.StandardArtifactIndexRecord;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Browse the repository.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="showArtifactAction"
 */
public class ShowArtifactAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory factory;

    private String groupId;

    private String artifactId;

    private String version;

    private Model model;

    private Collection dependencies;

    public String artifact()
        throws ConfigurationStoreException, IOException, XmlPullParserException, ProjectBuildingException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        MavenProject project = readProject();

        model = project.getModel();

        return SUCCESS;
    }

    public String dependencies()
        throws ConfigurationStoreException, IOException, XmlPullParserException, ProjectBuildingException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        MavenProject project = readProject();

        model = project.getModel();

        // TODO: should this be the whole set of artifacts, and be more like the maven dependencies report?

        List dependencies = new ArrayList();

        for ( Iterator i = project.getModel().getDependencies().iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();

            dependencies.add( new DependencyWrapper( dependency ) );
        }

        this.dependencies = dependencies;

        return SUCCESS;
    }

    public String dependees()
        throws ConfigurationStoreException, IOException, XmlPullParserException, ProjectBuildingException,
        RepositoryIndexException, RepositoryIndexSearchException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        MavenProject project = readProject();

        model = project.getModel();

        RepositoryArtifactIndex index = getIndex();

        String id = createId( groupId, artifactId, version );
        List records = index.search( new LuceneQuery( new TermQuery( new Term( "dependencies", id ) ) ) );

        Map dependees = new LinkedHashMap();

        for ( Iterator i = records.iterator(); i.hasNext(); )
        {
            StandardArtifactIndexRecord record = (StandardArtifactIndexRecord) i.next();

            String key = record.getGroupId() + ":" + record.getArtifactId();
            if ( dependees.containsKey( key ) )
            {
                DependencyWrapper wrapper = (DependencyWrapper) dependees.get( key );
                wrapper.addVersion( record.getVersion() );
            }
            else
            {
                DependencyWrapper wrapper = new DependencyWrapper( record );

                dependees.put( key, wrapper );
            }
        }

        dependencies = dependees.values();

        return SUCCESS;
    }

    private static String createId( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    private RepositoryArtifactIndex getIndex()
        throws ConfigurationStoreException, RepositoryIndexException
    {
        Configuration configuration = configurationStore.getConfigurationFromStore();
        File indexPath = new File( configuration.getIndexPath() );

        return factory.createStandardIndex( indexPath );
    }

    private MavenProject readProject()
        throws ConfigurationStoreException, ProjectBuildingException
    {
        Configuration configuration = configurationStore.getConfigurationFromStore();
        List repositories = repositoryFactory.createRepositories( configuration );

        Artifact artifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );
        // TODO: maybe we can decouple the assembly parts of the project builder from the repository handling to get rid of the temp repo
        ArtifactRepository localRepository = repositoryFactory.createLocalRepository( configuration );
        return projectBuilder.buildFromRepository( artifact, repositories, localRepository );
    }

    private boolean checkParameters()
    {
        boolean result = true;

        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            result = false;
        }

        else if ( StringUtils.isEmpty( artifactId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a artifact ID to browse" );
            result = false;
        }

        else if ( StringUtils.isEmpty( version ) )
        {
            // TODO: i18n
            addActionError( "You must specify a version to browse" );
            result = false;
        }
        return result;
    }

    public Model getModel()
    {
        return model;
    }

    public Collection getDependencies()
    {
        return dependencies;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public static class DependencyWrapper
    {
        private final String groupId;

        private final String artifactId;

        private List versions = new ArrayList();

        private String version;

        private String scope;

        private String classifier;

        public DependencyWrapper( StandardArtifactIndexRecord record )
        {
            this.groupId = record.getGroupId();

            this.artifactId = record.getArtifactId();

            addVersion( record.getVersion() );
        }

        public DependencyWrapper( Dependency dependency )
        {
            this.groupId = dependency.getGroupId();

            this.artifactId = dependency.getArtifactId();

            this.scope = dependency.getScope();

            this.classifier = dependency.getClassifier();

            addVersion( dependency.getVersion() );
        }

        public String getScope()
        {
            return scope;
        }

        public String getClassifier()
        {
            return classifier;
        }

        public void addVersion( String version )
        {
            versions.add( version );

            if ( versions.size() == 1 )
            {
                this.version = version;
            }
            else
            {
                this.version = null;
                // TODO: use version comparator!
                Collections.sort( versions );
            }
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public List getVersions()
        {
            return versions;
        }

        public String getVersion()
        {
            return version;
        }
    }
}
