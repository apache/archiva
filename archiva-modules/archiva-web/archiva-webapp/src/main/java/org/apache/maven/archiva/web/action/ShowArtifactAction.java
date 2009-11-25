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
import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.MailingList;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.model.VersionedReference;
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
     * @plexus.requirement role-hint="default"
     */
    private RepositoryBrowsing repoBrowsing;

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
    private ArchivaProjectModel model;

    /**
     * The list of artifacts that depend on this versioned project.
     */
    private List<ArchivaProjectModel> dependees;

    private List<MailingList> mailingLists;

    private List<Dependency> dependencies;

    private List<String> snapshotVersions;

    /**
     * Show the versioned project information tab. TODO: Change name to 'project'
     */
    public String artifact()
    {
        // In the future, this should be replaced by the repository grouping mechanism, so that we are only making
        // simple resource requests here and letting the resolver take care of it
        ProjectBuildMetadata build = null;
        snapshotVersions = new ArrayList<String>();
        for ( String repoId : getObservableRepos() )
        {
            if ( build == null )
            {
                // we don't really want the implementation being that intelligent - so another resolver to do the
                // "just-in-time" nature of picking up the metadata (if appropriate for the repository type) is used
                build = metadataResolver.getProjectBuild( repoId, groupId, artifactId, version );
                if ( build != null )
                {
                    repositoryId = repoId;      
                }
            }

            snapshotVersions.addAll( metadataResolver.getArtifactVersions( repoId, groupId, artifactId, version ) );
            snapshotVersions.remove( version );
        }

        if ( build == null )
        {
            addActionError( "Artifact not found" );
            return ERROR;
        }

        // TODO: eventually, move to just use the metadata directly, with minimal JSP changes, mostly for Maven specifics
        model = new ArchivaProjectModel();
        MavenProjectFacet projectFacet = (MavenProjectFacet) build.getFacet( MavenProjectFacet.FACET_ID );
        model.setGroupId( projectFacet.getGroupId() );
        model.setArtifactId( projectFacet.getArtifactId() );
        model.setPackaging( projectFacet.getPackaging() );
        if ( projectFacet.getParent() != null )
        {
            VersionedReference parent = new VersionedReference();
            parent.setGroupId( projectFacet.getParent().getGroupId() );
            parent.setArtifactId( projectFacet.getParent().getArtifactId() );
            parent.setVersion( projectFacet.getParent().getVersion() );
            model.setParentProject( parent );
        }

        model.setVersion( build.getId() );
        model.setDescription( build.getDescription() );
        model.setName( build.getName() );
        model.setUrl( build.getUrl() );
        if ( build.getOrganization() != null )
        {
            Organization organization = new Organization();
            organization.setName( build.getOrganization().getName() );
            organization.setUrl( build.getOrganization().getUrl() );
            model.setOrganization( organization );
        }
        if ( build.getCiManagement() != null )
        {
            CiManagement ci = new CiManagement();
            ci.setSystem( build.getCiManagement().getSystem() );
            ci.setUrl( build.getCiManagement().getUrl() );
            model.setCiManagement( ci );
        }
        if ( build.getIssueManagement() != null )
        {
            IssueManagement issueManagement = new IssueManagement();
            issueManagement.setSystem( build.getIssueManagement().getSystem() );
            issueManagement.setUrl( build.getIssueManagement().getUrl() );
            model.setIssueManagement( issueManagement );
        }
        if ( build.getScm() != null )
        {
            Scm scm = new Scm();
            scm.setConnection( build.getScm().getConnection() );
            scm.setDeveloperConnection( build.getScm().getDeveloperConnection() );
            scm.setUrl( build.getScm().getUrl() );
            model.setScm( scm );
        }
        if ( build.getLicenses() != null )
        {
            for ( org.apache.archiva.metadata.model.License l : build.getLicenses() )
            {
                License license = new License();
                license.setName( l.getName() );
                license.setUrl( l.getUrl() );
                model.addLicense( license );
            }
        }

        return SUCCESS;
    }

    /**
     * Show the artifact information tab.
     */
    public String dependencies()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        this.model = repoBrowsing.selectVersion( getPrincipal(), getObservableRepos(), groupId, artifactId, version );

        this.dependencies = model.getDependencies();

        return SUCCESS;
    }

    /**
     * Show the mailing lists information tab.
     */
    public String mailingLists()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        this.model = repoBrowsing.selectVersion( getPrincipal(), getObservableRepos(), groupId, artifactId, version );
        this.mailingLists = model.getMailingLists();

        return SUCCESS;
    }

    /**
     * Show the reports tab.
     */
    public String reports()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        // TODO: hook up reports on project - this.reports = artifactsDatabase.findArtifactResults( groupId, artifactId,
        // version );

        return SUCCESS;
    }

    /**
     * Show the dependees (other artifacts that depend on this project) tab.
     */
    public String dependees()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        this.model = repoBrowsing.selectVersion( getPrincipal(), getObservableRepos(), groupId, artifactId, version );

        this.dependees = repoBrowsing.getUsedBy( getPrincipal(), getObservableRepos(), groupId, artifactId, version );

        return SUCCESS;
    }

    /**
     * Show the dependencies of this versioned project tab.
     */
    public String dependencyTree()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        this.model = repoBrowsing.selectVersion( getPrincipal(), getObservableRepos(), groupId, artifactId, version );

        return SUCCESS;
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

    public ArchivaProjectModel getModel()
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

    public List<ArchivaProjectModel> getDependees()
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

    public void setSnapshotVersions( List<String> snapshotVersions )
    {
        this.snapshotVersions = snapshotVersions;
    }

    public MetadataResolver getMetadataRepository()
    {
        return metadataResolver;
    }
}
