package org.apache.maven.archiva.manager.web.action;

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
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.List;

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

    private String groupId;

    private String artifactId;

    private String version;

    private Model model;

    public String execute()
        throws ConfigurationStoreException, IOException, XmlPullParserException, ProjectBuildingException
    {
        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        if ( StringUtils.isEmpty( artifactId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a artifact ID to browse" );
            return ERROR;
        }

        if ( StringUtils.isEmpty( version ) )
        {
            // TODO: i18n
            addActionError( "You must specify a version to browse" );
            return ERROR;
        }

        Configuration configuration = configurationStore.getConfigurationFromStore();
        List repositories = repositoryFactory.createRepositories( configuration );

        Artifact artifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );
        // TODO: maybe we can decouple the assembly parts of the project builder from the repository handling to get rid of the temp repo
        ArtifactRepository localRepository = repositoryFactory.createLocalRepository( configuration );
        MavenProject project = projectBuilder.buildFromRepository( artifact, repositories, localRepository );

        model = project.getModel();

        return SUCCESS;
    }

    public Model getModel()
    {
        return model;
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
}
