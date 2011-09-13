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
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.group.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

/**
 * DeleteRepositoryGroupAction
 */
@Controller( "deleteRepositoryGroupAction" )
@Scope( "prototype" )
public class DeleteRepositoryGroupAction
    extends AbstractRepositoriesAdminAction
    implements Preparable
{
    private RepositoryGroup repositoryGroup;

    @Inject
    private RepositoryGroupAdmin repositoryGroupAdmin;

    private String repoGroupId;

    public void prepare()
        throws RepositoryAdminException
    {

        if ( StringUtils.isNotBlank( repoGroupId ) )
        {
            this.repositoryGroup = repositoryGroupAdmin.getRepositoryGroup( repoGroupId );
        }
    }

    public String confirmDelete()
    {
        if ( StringUtils.isBlank( repoGroupId ) )
        {
            addActionError( "Unable to delete repository group: repository id was blank." );
            return ERROR;
        }

        return INPUT;
    }

    public String delete()
    {

        try
        {
            RepositoryGroup group = repositoryGroupAdmin.getRepositoryGroup( repoGroupId );
            if ( group == null )
            {
                addActionError( "A repository group with that id does not exist." );
                return ERROR;
            }

            repositoryGroupAdmin.deleteRepositoryGroup( repoGroupId, getAuditInformation() );
            return SUCCESS;
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "error occured " + e.getMessage() );
            return ERROR;
        }
    }

    public RepositoryGroup getRepositoryGroup()
    {
        return repositoryGroup;
    }

    public void setRepositoryGroup( RepositoryGroup repositoryGroup )
    {
        this.repositoryGroup = repositoryGroup;
    }

    public String getRepoGroupId()
    {
        return repoGroupId;
    }

    public void setRepoGroupId( String repoGroupId )
    {
        this.repoGroupId = repoGroupId;
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
