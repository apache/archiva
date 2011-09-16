package org.apache.archiva.web.action;

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

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Browse the repository.
 *
 * @todo implement repository selectors (all or specific repository)
 */
@Controller( "browseAction" )
@Scope( "prototype" )
public class BrowseAction
    extends AbstractRepositoryBasedAction
{
    private String groupId;

    private String artifactId;

    private String repositoryId;

    private ProjectVersionMetadata sharedModel;

    private Collection<String> namespaces;

    private Collection<String> projectIds;

    private Collection<String> projectVersions;

    public String browse()
        throws MetadataResolutionException
    {
        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        Set<String> namespaces = new LinkedHashSet<String>();

        // TODO: this logic should be optional, particularly remembering we want to keep this code simple
        //       it is located here to avoid the content repository implementation needing to do too much for what
        //       is essentially presentation code
        Set<String> namespacesToCollapse;
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();
            namespacesToCollapse = new LinkedHashSet<String>();
            for ( String repoId : selectedRepos )
            {
                namespacesToCollapse.addAll( metadataResolver.resolveRootNamespaces( repositorySession, repoId ) );
            }

            for ( String n : namespacesToCollapse )
            {
                // TODO: check performance of this
                namespaces.add( collapseNamespaces( repositorySession, metadataResolver, selectedRepos, n ) );
            }
        }
        finally
        {
            repositorySession.close();
        }

        this.namespaces = getSortedList( namespaces );
        return SUCCESS;
    }

    private String collapseNamespaces( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                       Collection<String> repoIds, String n )
        throws MetadataResolutionException
    {
        Set<String> subNamespaces = new LinkedHashSet<String>();
        for ( String repoId : repoIds )
        {
            subNamespaces.addAll( metadataResolver.resolveNamespaces( repositorySession, repoId, n ) );
        }
        if ( subNamespaces.size() != 1 )
        {
            log.debug( "{} is not collapsible as it has sub-namespaces: {}", n, subNamespaces );
            return n;
        }
        else
        {
            for ( String repoId : repoIds )
            {
                Collection<String> projects = metadataResolver.resolveProjects( repositorySession, repoId, n );
                if ( projects != null && !projects.isEmpty() )
                {
                    log.debug( "{} is not collapsible as it has projects", n );
                    return n;
                }
            }
            return collapseNamespaces( repositorySession, metadataResolver, repoIds,
                                       n + "." + subNamespaces.iterator().next() );
        }
    }

    public String browseGroup()
        throws MetadataResolutionException
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

        Set<String> projects = new LinkedHashSet<String>();

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        Set<String> namespaces;
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> namespacesToCollapse = new LinkedHashSet<String>();
            for ( String repoId : selectedRepos )
            {
                namespacesToCollapse.addAll( metadataResolver.resolveNamespaces( repositorySession, repoId, groupId ) );

                projects.addAll( metadataResolver.resolveProjects( repositorySession, repoId, groupId ) );
            }

            // TODO: this logic should be optional, particularly remembering we want to keep this code simple
            //       it is located here to avoid the content repository implementation needing to do too much for what
            //       is essentially presentation code
            namespaces = new LinkedHashSet<String>();
            for ( String n : namespacesToCollapse )
            {
                // TODO: check performance of this
                namespaces.add(
                    collapseNamespaces( repositorySession, metadataResolver, selectedRepos, groupId + "." + n ) );
            }
        }
        finally
        {
            repositorySession.close();
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
        throws MetadataResolutionException
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

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();

            Set<String> versions = new LinkedHashSet<String>();
            for ( String repoId : selectedRepos )
            {
                versions.addAll(
                    metadataResolver.resolveProjectVersions( repositorySession, repoId, groupId, artifactId ) );
            }

            // TODO: sort by known version ordering method
            this.projectVersions = new ArrayList<String>( versions );

            populateSharedModel( repositorySession, metadataResolver, selectedRepos, versions );
        }
        finally
        {
            repositorySession.close();
        }

        return SUCCESS;
    }

    private void populateSharedModel( RepositorySession repositorySession, MetadataResolver metadataResolver,
                                      Collection<String> selectedRepos, Collection<String> projectVersions )
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
                    try
                    {
                        versionMetadata =
                            metadataResolver.resolveProjectVersion( repositorySession, repoId, groupId, artifactId,
                                                                    version );
                    }
                    catch ( MetadataResolutionException e )
                    {
                        log.error(
                            "Skipping invalid metadata while compiling shared model for " + groupId + ":" + artifactId
                                + " in repo " + repoId + ": " + e.getMessage() );
                    }
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

                if ( sharedModel.getName() != null && !StringUtils.equalsIgnoreCase( sharedModel.getName(),
                                                                                     versionMetadata.getName() ) )
                {
                    sharedModel.setName( "" );
                }

                if ( sharedModel.getDescription() != null && !StringUtils.equalsIgnoreCase(
                    sharedModel.getDescription(), versionMetadata.getDescription() ) )
                {
                    sharedModel.setDescription( null );
                }

                if ( sharedModel.getIssueManagement() != null && versionMetadata.getIssueManagement() != null
                    && !StringUtils.equalsIgnoreCase( sharedModel.getIssueManagement().getUrl(),
                                                      versionMetadata.getIssueManagement().getUrl() ) )
                {
                    sharedModel.setIssueManagement( null );
                }

                if ( sharedModel.getCiManagement() != null && versionMetadata.getCiManagement() != null
                    && !StringUtils.equalsIgnoreCase( sharedModel.getCiManagement().getUrl(),
                                                      versionMetadata.getCiManagement().getUrl() ) )
                {
                    sharedModel.setCiManagement( null );
                }

                if ( sharedModel.getOrganization() != null && versionMetadata.getOrganization() != null
                    && !StringUtils.equalsIgnoreCase( sharedModel.getOrganization().getName(),
                                                      versionMetadata.getOrganization().getName() ) )
                {
                    sharedModel.setOrganization( null );
                }

                if ( sharedModel.getUrl() != null && !StringUtils.equalsIgnoreCase( sharedModel.getUrl(),
                                                                                    versionMetadata.getUrl() ) )
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

    public Collection<String> getProjectIds()
    {
        return projectIds;
    }

    public Collection<String> getProjectVersions()
    {
        return projectVersions;
    }
}
