package org.apache.maven.archiva.web.action;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.MetadataResolverException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.security.AccessDeniedException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;

/**
 * Browse the repository.
 *
 * TODO change name to ShowVersionedAction to conform to terminology.
 *
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="showArtifactAction" instantiation-strategy="per-lookup"
 */
public class ShowArtifactAction
    extends PlexusActionSupport
    implements Validateable
{
    /* .\ Not Exposed \._____________________________________________ */

    /**
     * @plexus.requirement
     */
    private UserRepositories userRepositories;

    /**
     * @plexus.requirement
     */
    private MetadataResolver metadataResolver;

    /* .\ Exposed Output Objects \.__________________________________ */

    private String groupId;

    private String artifactId;

    private String version;

    private String repositoryId;

    /**
     * The model of this versioned project.
     */
    private ProjectVersionMetadata model;

    /**
     * The list of artifacts that depend on this versioned project.
     */
    private List<ProjectVersionReference> dependees;

    private List<MailingList> mailingLists;

    private List<Dependency> dependencies;

    private List<String> snapshotVersions;

    /**
     * Show the versioned project information tab.
     * TODO: Change name to 'project' - we are showing project versions here, not specific artifact information (though
     * that is rendered in the download box).
     */
    public String artifact()
    {
        // In the future, this should be replaced by the repository grouping mechanism, so that we are only making
        // simple resource requests here and letting the resolver take care of it
        ProjectVersionMetadata versionMetadata = null;
        snapshotVersions = new ArrayList<String>();
        for ( String repoId : getObservableRepos() )
        {
            if ( versionMetadata == null )
            {
                // TODO: though we have a simple mapping now, do we want to support paths like /1.0-20090111.123456-1/
                //   again by mapping it to /1.0-SNAPSHOT/? Currently, the individual versions are not supported as we
                //   are only displaying the project's single version.

                // we don't want the implementation being that intelligent - so another resolver to do the
                // "just-in-time" nature of picking up the metadata (if appropriate for the repository type) is used
                try
                {
                    versionMetadata = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
                }
                catch ( MetadataResolverException e )
                {
                    addActionError( "Error occurred resolving metadata for project: " + e.getMessage() );
                    return ERROR;
                }
                if ( versionMetadata != null )
                {
                    repositoryId = repoId;

                    snapshotVersions.addAll(
                        metadataResolver.getArtifactVersions( repoId, groupId, artifactId, versionMetadata.getId() ) );
                    snapshotVersions.remove( version );
                }
            }
        }

        if ( versionMetadata == null )
        {
            addActionError( "Artifact not found" );
            return ERROR;
        }
        model = versionMetadata;

        return SUCCESS;
    }

    /**
     * Show the artifact information tab.
     */
    public String dependencies()
    {
        ProjectVersionMetadata versionMetadata = null;
        for ( String repoId : getObservableRepos() )
        {
            if ( versionMetadata == null )
            {
                try
                {
                    versionMetadata = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
                }
                catch ( MetadataResolverException e )
                {
                    addActionError( "Error occurred resolving metadata for project: " + e.getMessage() );
                    return ERROR;
                }
            }
        }

        if ( versionMetadata == null )
        {
            addActionError( "Artifact not found" );
            return ERROR;
        }
        model = versionMetadata;

        this.dependencies = model.getDependencies();

        return SUCCESS;
    }

    /**
     * Show the mailing lists information tab.
     */
    public String mailingLists()
    {
        ProjectVersionMetadata versionMetadata = null;
        for ( String repoId : getObservableRepos() )
        {
            if ( versionMetadata == null )
            {
                try
                {
                    versionMetadata = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
                }
                catch ( MetadataResolverException e )
                {
                    addActionError( "Error occurred resolving metadata for project: " + e.getMessage() );
                    return ERROR;
                }
            }
        }

        if ( versionMetadata == null )
        {
            addActionError( "Artifact not found" );
            return ERROR;
        }
        model = versionMetadata;

        this.mailingLists = model.getMailingLists();

        return SUCCESS;
    }

    /**
     * Show the reports tab.
     */
    public String reports()
    {
        // TODO: hook up reports on project - this.reports = artifactsDatabase.findArtifactResults( groupId, artifactId,
        // version );

        return SUCCESS;
    }

    /**
     * Show the dependees (other artifacts that depend on this project) tab.
     */
    public String dependees()
    {
        ProjectVersionMetadata versionMetadata = null;
        for ( String repoId : getObservableRepos() )
        {
            if ( versionMetadata == null )
            {
                try
                {
                    versionMetadata = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
                }
                catch ( MetadataResolverException e )
                {
                    addActionError( "Error occurred resolving metadata for project: " + e.getMessage() );
                    return ERROR;
                }
            }
        }

        if ( versionMetadata == null )
        {
            addActionError( "Artifact not found" );
            return ERROR;
        }
        model = versionMetadata;

        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();
        // TODO: what if we get duplicates across repositories?
        for ( String repoId : getObservableRepos() )
        {
            // TODO: what about if we want to see this irrespective of version?
            references.addAll( metadataResolver.getProjectReferences( repoId, groupId, artifactId, version ) );
        }

        this.dependees = references;

        // TODO: may need to note on the page that references will be incomplete if the other artifacts are not yet stored in the content repository
        // (especially in the case of pre-population import)

        return SUCCESS;
    }

    /**
     * Show the dependencies of this versioned project tab.
     */
    public String dependencyTree()
    {
        // temporarily use this as we only need the model for the tag to perform, but we should be resolving the
        // graph here instead

        // TODO: may need to note on the page that tree will be incomplete if the other artifacts are not yet stored in the content repository
        // (especially in the case of pre-population import)

        return artifact();
    }

    private List<String> getObservableRepos()
    {
        try
        {
            return userRepositories.getObservableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
            // TODO: pass this onto the screen.
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    @Override
    public void validate()
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            addActionError( "You must specify a group ID to browse" );
        }

        if ( StringUtils.isBlank( artifactId ) )
        {
            addActionError( "You must specify a artifact ID to browse" );
        }

        if ( StringUtils.isBlank( version ) )
        {
            addActionError( "You must specify a version to browse" );
        }
    }

    public ProjectVersionMetadata getModel()
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

    public List<MailingList> getMailingLists()
    {
        return mailingLists;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public List<ProjectVersionReference> getDependees()
    {
        return dependees;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public List<String> getSnapshotVersions()
    {
        return snapshotVersions;
    }

    public MetadataResolver getMetadataResolver()
    {
        return metadataResolver;
    }
}
