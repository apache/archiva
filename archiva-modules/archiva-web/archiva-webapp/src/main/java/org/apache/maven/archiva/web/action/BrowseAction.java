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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Browse the repository.
 *
 * @todo implement repository selectors (all or specific repository)
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="browseAction" instantiation-strategy="per-lookup"
 */
public class BrowseAction
    extends AbstractRepositoryBasedAction
{
    /**
     * @plexus.requirement
     */
    private MetadataResolver metadataResolver;

    private String groupId;

    private String artifactId;

    private String repositoryId;

    private ProjectVersionMetadata sharedModel;

    private Collection<String> namespaces;

    private Collection<String> projectIds;

    private Collection<String> projectVersions;

    public String browse()
    {
        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        Set<String> namespaces = new LinkedHashSet<String>();
        for ( String repoId : selectedRepos )
        {
            Collection<String> rootNamespaces = metadataResolver.getRootNamespaces( repoId );
            // TODO: this logic should be optional, particularly remembering we want to keep this code simple
            //       it is located here to avoid the content repository implementation needing to do too much for what
            //       is essentially presentation code
            for ( String n : rootNamespaces )
            {
                // TODO: check performance of this
                namespaces.add( collapseNamespaces( repoId, n ) );
            }
        }

        this.namespaces = getSortedList( namespaces );
        return SUCCESS;
    }

    private String collapseNamespaces( String repoId, String n )
    {
        Collection<String> subNamespaces = metadataResolver.getNamespaces( repoId, n );
        if ( subNamespaces.size() != 1 )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( n + " is not collapsible as it has sub-namespaces: " + subNamespaces );
            }
            return n;
        }
        else
        {
            Collection<String> projects = metadataResolver.getProjects( repoId, n );
            if ( projects != null && !projects.isEmpty() )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( n + " is not collapsible as it has projects" );
                }
                return n;
            }
            else
            {
                return collapseNamespaces( repoId, n + "." + subNamespaces.iterator().next() );
            }
        }
    }

    public String browseGroup()
    {
        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        Set<String> namespaces = new LinkedHashSet<String>();
        Set<String> projects = new LinkedHashSet<String>();
        for ( String repoId : selectedRepos )
        {
            Collection<String> childNamespaces = metadataResolver.getNamespaces( repoId, groupId );
            // TODO: this logic should be optional, particularly remembering we want to keep this code simple
            //       it is located here to avoid the content repository implementation needing to do too much for what
            //       is essentially presentation code
            for ( String n : childNamespaces )
            {
                // TODO: check performance of this
                namespaces.add( collapseNamespaces( repoId, groupId + "." + n ) );
            }

            projects.addAll( metadataResolver.getProjects( repoId, groupId ) );
        }

        this.namespaces = getSortedList( namespaces );
        this.projectIds = getSortedList( projects );
        return SUCCESS;
    }

    private ArrayList<String> getSortedList( Set<String> set )
    {
        ArrayList<String> list = new ArrayList<String>( set );
        Collections.sort( list );
        return list;
    }

    public String browseArtifact()
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

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        Set<String> versions = new LinkedHashSet<String>();
        for ( String repoId : selectedRepos )
        {
            versions.addAll( metadataResolver.getProjectVersions( repoId, groupId, artifactId ) );
        }

        // TODO: sort by known version ordering method
        this.projectVersions = new ArrayList<String>( versions );

        populateSharedModel( selectedRepos, versions );

        return SUCCESS;
    }

    private void populateSharedModel( Collection<String> selectedRepos, Collection<String> projectVersions )
    {
        sharedModel = new ProjectVersionMetadata();

        MavenProjectFacet mavenFacet = new MavenProjectFacet();
        mavenFacet.setGroupId( groupId );
        mavenFacet.setArtifactId( artifactId );
        sharedModel.addFacet( mavenFacet );

        boolean isFirstVersion = true;

        for ( String version : projectVersions )
        {
            ProjectVersionMetadata versionMetadata = null;
            for ( String repoId : selectedRepos )
            {
                if ( versionMetadata == null )
                {
                    versionMetadata = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
                }
            }

            if ( versionMetadata == null )
            {
                continue;
            }

            if ( isFirstVersion )
            {
                sharedModel = versionMetadata;
                sharedModel.setId( null );
            }
            else
            {
                MavenProjectFacet versionMetadataMavenFacet =
                    (MavenProjectFacet) versionMetadata.getFacet( MavenProjectFacet.FACET_ID );
                if ( versionMetadataMavenFacet != null )
                {
                    if ( mavenFacet.getPackaging() != null && !StringUtils.equalsIgnoreCase( mavenFacet.getPackaging(),
                                                                                             versionMetadataMavenFacet.getPackaging() ) )
                    {
                        mavenFacet.setPackaging( null );
                    }
                }

                if ( sharedModel.getName() != null &&
                    !StringUtils.equalsIgnoreCase( sharedModel.getName(), versionMetadata.getName() ) )
                {
                    sharedModel.setName( "" );
                }

                if ( sharedModel.getDescription() != null &&
                    !StringUtils.equalsIgnoreCase( sharedModel.getDescription(), versionMetadata.getDescription() ) )
                {
                    sharedModel.setDescription( null );
                }

                if ( sharedModel.getIssueManagement() != null && versionMetadata.getIssueManagement() != null &&
                    !StringUtils.equalsIgnoreCase( sharedModel.getIssueManagement().getUrl(),
                                                   versionMetadata.getIssueManagement().getUrl() ) )
                {
                    sharedModel.setIssueManagement( null );
                }

                if ( sharedModel.getCiManagement() != null && versionMetadata.getCiManagement() != null &&
                    !StringUtils.equalsIgnoreCase( sharedModel.getCiManagement().getUrl(),
                                                   versionMetadata.getCiManagement().getUrl() ) )
                {
                    sharedModel.setCiManagement( null );
                }

                if ( sharedModel.getOrganization() != null && versionMetadata.getOrganization() != null &&
                    !StringUtils.equalsIgnoreCase( sharedModel.getOrganization().getName(),
                                                   versionMetadata.getOrganization().getName() ) )
                {
                    sharedModel.setOrganization( null );
                }

                if ( sharedModel.getUrl() != null &&
                    !StringUtils.equalsIgnoreCase( sharedModel.getUrl(), versionMetadata.getUrl() ) )
                {
                    sharedModel.setUrl( null );
                }
            }

            isFirstVersion = false;
        }
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

    public Collection<String> getNamespaces()
    {
        return namespaces;
    }

    public String getRepositoryId()
    {

        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {

        this.repositoryId = repositoryId;
    }

    public ProjectVersionMetadata getSharedModel()
    {
        return sharedModel;
    }

    public MetadataResolver getMetadataResolver()
    {
        return metadataResolver;
    }

    public Collection<String> getProjectIds()
    {
        return projectIds;
    }

    public Collection<String> getProjectVersions()
    {
        return projectVersions;
    }
}
