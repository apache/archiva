package org.apache.maven.archiva.web.action.admin.repositories;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.audit.AuditEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.web.util.ContextUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RepositoryGroupsAction
 *
 * @version plexus.component role="com.opensymphony.xwork2.Action" role-hint="repositoryGroupsAction" instantiation-strategy="per-lookup"
 */
@Controller( "repositoryGroupsAction" )
@Scope( "prototype" )
public class RepositoryGroupsAction
    extends AbstractRepositoriesAdminAction
    implements ServletRequestAware, Preparable
{
    private RepositoryGroupConfiguration repositoryGroup;

    private Map<String, RepositoryGroupConfiguration> repositoryGroups;

    private Map<String, ManagedRepositoryConfiguration> managedRepositories;

    private Map<String, List<String>> groupToRepositoryMap;

    private String repoGroupId;

    private String repoId;

    /**
     * Used to construct the repository WebDAV URL in the repository action.
     */
    private String baseUrl;

    private static final Pattern REPO_GROUP_ID_PATTERN = Pattern.compile( "[A-Za-z0-9\\._\\-]+" );

    public void setServletRequest( HttpServletRequest request )
    {
        this.baseUrl = ContextUtils.getBaseURL( request, "repository" );
    }

    public void prepare()
    {
        Configuration config = archivaConfiguration.getConfiguration();

        repositoryGroup = new RepositoryGroupConfiguration();
        repositoryGroups = config.getRepositoryGroupsAsMap();
        managedRepositories = config.getManagedRepositoriesAsMap();
        groupToRepositoryMap = config.getGroupToRepositoryMap();
    }

    public String addRepositoryGroup()
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        String repoGroupId = repositoryGroup.getId();

        if ( repoGroupId == null || "".equals( repoGroupId.trim() ) )
        {
            addActionError( "Identifier field is required." );
            return ERROR;
        }

        if ( repoGroupId.length() > 100 )
        {
            addActionError( "Identifier [" + repoGroupId + "] is over the maximum limit of 100 characters" );
            return ERROR;
        }

        Matcher matcher = REPO_GROUP_ID_PATTERN.matcher( repoGroupId );
        if ( !matcher.matches() )
        {
            addActionError(
                "Invalid character(s) found in identifier. Only the following characters are allowed: alphanumeric, '.', '-' and '_'" );
            return ERROR;
        }

        if ( StringUtils.isBlank( repoGroupId ) )
        {
            addActionError( "You must enter a repository group id." );
            return ERROR;
        }

        if ( configuration.getRepositoryGroupsAsMap().containsKey( repoGroupId ) )
        {
            addActionError( "Unable to add new repository group with id [" + repoGroupId
                                + "], that id already exists as a repository group." );
            return ERROR;
        }
        else if ( configuration.getManagedRepositoriesAsMap().containsKey( repoGroupId ) )
        {
            addActionError( "Unable to add new repository group with id [" + repoGroupId
                                + "], that id already exists as a managed repository." );
            return ERROR;
        }
        else if ( configuration.getRemoteRepositoriesAsMap().containsKey( repoGroupId ) )
        {
            addActionError( "Unable to add new repository group with id [" + repoGroupId
                                + "], that id already exists as a remote repository." );
            return ERROR;
        }

        configuration.addRepositoryGroup( repositoryGroup );
        triggerAuditEvent( AuditEvent.ADD_REPO_GROUP + " " + repoGroupId );
        return saveConfiguration( configuration );
    }

    public String addRepositoryToGroup()
    {
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryGroupConfiguration group = config.findRepositoryGroupById( repoGroupId );

        validateRepository();

        if ( hasErrors() )
        {
            return ERROR;
        }

        if ( group.getRepositories().contains( repoId ) )
        {
            addActionError( "Repository with id [" + repoId + "] is already in the group" );
            return ERROR;
        }

        // remove the old repository group configuration
        config.removeRepositoryGroup( group );

        // save repository group configuration
        group.addRepository( repoId );
        config.addRepositoryGroup( group );

        triggerAuditEvent( repoId, null, AuditEvent.ADD_REPO_TO_GROUP + " " + repoGroupId );

        return saveConfiguration( config );
    }

    public String removeRepositoryFromGroup()
    {
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryGroupConfiguration group = config.findRepositoryGroupById( repoGroupId );

        validateRepository();

        if ( hasErrors() )
        {
            return ERROR;
        }

        if ( !group.getRepositories().contains( repoId ) )
        {
            addActionError( "No repository with id[" + repoId + "] found in the group" );
            return ERROR;
        }

        // remove the old repository group configuration
        config.removeRepositoryGroup( group );

        // save repository group configuration
        group.removeRepository( repoId );
        config.addRepositoryGroup( group );

        triggerAuditEvent( repoId, null, AuditEvent.DELETE_REPO_FROM_GROUP + " " + repoGroupId );

        return saveConfiguration( config );
    }

    public void validateRepository()
    {
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryGroupConfiguration group = config.findRepositoryGroupById( repoGroupId );
        ManagedRepositoryConfiguration repo = config.findManagedRepositoryById( repoId );

        if ( group == null )
        {
            addActionError( "A repository group with that id does not exist." );
        }

        if ( repo == null )
        {
            addActionError( "A repository with that id does not exist." );
        }
    }

    public RepositoryGroupConfiguration getRepositoryGroup()
    {
        return repositoryGroup;
    }

    public void setRepositoryGroup( RepositoryGroupConfiguration repositoryGroup )
    {
        this.repositoryGroup = repositoryGroup;
    }

    public Map<String, RepositoryGroupConfiguration> getRepositoryGroups()
    {
        return repositoryGroups;
    }

    public void setRepositoryGroups( Map<String, RepositoryGroupConfiguration> repositoryGroups )
    {
        this.repositoryGroups = repositoryGroups;
    }

    public Map<String, ManagedRepositoryConfiguration> getManagedRepositories()
    {
        return managedRepositories;
    }

    public Map<String, List<String>> getGroupToRepositoryMap()
    {
        return this.groupToRepositoryMap;
    }

    public String getRepoGroupId()
    {
        return repoGroupId;
    }

    public void setRepoGroupId( String repoGroupId )
    {
        this.repoGroupId = repoGroupId;
    }

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }
}
