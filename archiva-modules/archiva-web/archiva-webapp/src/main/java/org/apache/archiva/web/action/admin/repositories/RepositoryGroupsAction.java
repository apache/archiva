package org.apache.archiva.web.action.admin.repositories;

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
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.web.util.ContextUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RepositoryGroupsAction
 */
@Controller( "repositoryGroupsAction" )
@Scope( "prototype" )
public class RepositoryGroupsAction
    extends AbstractRepositoriesAdminAction
    implements ServletRequestAware, Preparable
{

    @Inject
    private RepositoryGroupAdmin repositoryGroupAdmin;

    private RepositoryGroup repositoryGroup;

    private Map<String, RepositoryGroup> repositoryGroups;

    private Map<String, ManagedRepository> managedRepositories;

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
        throws RepositoryAdminException
    {

        repositoryGroup = new RepositoryGroup();
        repositoryGroups = getRepositoryGroupAdmin().getRepositoryGroupsAsMap();
        managedRepositories = getManagedRepositoryAdmin().getManagedRepositoriesAsMap();
        groupToRepositoryMap = getRepositoryGroupAdmin().getGroupToRepositoryMap();
    }

    public String addRepositoryGroup()
    {
        try
        {
            getRepositoryGroupAdmin().addRepositoryGroup( repositoryGroup, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }

        return SUCCESS;
    }

    public String addRepositoryToGroup()
    {
        try
        {
            getRepositoryGroupAdmin().addRepositoryToGroup( repoGroupId, repoId, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }
        return SUCCESS;
    }

    public String removeRepositoryFromGroup()
    {
        try
        {
            getRepositoryGroupAdmin().deleteRepositoryFromGroup( repoGroupId, repoId, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }
        return SUCCESS;
    }


    public RepositoryGroup getRepositoryGroup()
    {
        return repositoryGroup;
    }

    public void setRepositoryGroup( RepositoryGroup repositoryGroup )
    {
        this.repositoryGroup = repositoryGroup;
    }

    public Map<String, RepositoryGroup> getRepositoryGroups()
    {
        return repositoryGroups;
    }

    public void setRepositoryGroups( Map<String, RepositoryGroup> repositoryGroups )
    {
        this.repositoryGroups = repositoryGroups;
    }

    public Map<String, ManagedRepository> getManagedRepositories()
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

    public RepositoryGroupAdmin getRepositoryGroupAdmin()
    {
        return repositoryGroupAdmin;
    }

    public void setRepositoryGroupAdmin( RepositoryGroupAdmin repositoryGroupAdmin )
    {
        this.repositoryGroupAdmin = repositoryGroupAdmin;
    }
}
