package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.rest.api.model.RepositoryGroup;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "repositoryGroupService#rest" )
public class DefaultRepositoryGroupService
    extends AbstractRestService
    implements RepositoryGroupService
{

    @Inject
    private RepositoryGroupAdmin repositoryGroupAdmin;

    public List<RepositoryGroup> getRepositoriesGroups()
        throws ArchivaRestServiceException
    {
        try
        {
            List<RepositoryGroup> repositoriesGroups = new ArrayList<RepositoryGroup>();
            for ( org.apache.archiva.admin.model.group.RepositoryGroup repoGroup : repositoryGroupAdmin.getRepositoriesGroups() )
            {
                repositoriesGroups.add(
                    new RepositoryGroup( repoGroup.getId(), new ArrayList<String>( repoGroup.getRepositories() ) ) );
            }
            return repositoriesGroups;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public RepositoryGroup getRepositoryGroup( String repositoryGroupId )
        throws ArchivaRestServiceException
    {
        for ( RepositoryGroup repositoryGroup : getRepositoriesGroups() )
        {
            if ( StringUtils.equals( repositoryGroupId, repositoryGroup.getId() ) )
            {
                return repositoryGroup;
            }
        }
        return null;
    }

    public Boolean addRepositoryGroup( RepositoryGroup repoGroup )
        throws ArchivaRestServiceException
    {
        try
        {
            return repositoryGroupAdmin.addRepositoryGroup(
                new org.apache.archiva.admin.model.group.RepositoryGroup( repoGroup.getId(), new ArrayList<String>(
                    repoGroup.getRepositories() ) ), getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean updateRepositoryGroup( RepositoryGroup repoGroup )
        throws ArchivaRestServiceException
    {
        try
        {
            return repositoryGroupAdmin.updateRepositoryGroup(
                new org.apache.archiva.admin.model.group.RepositoryGroup( repoGroup.getId(), new ArrayList<String>(
                    repoGroup.getRepositories() ) ), getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean deleteRepositoryGroup( String repositoryGroupId )
        throws ArchivaRestServiceException
    {
        try
        {
            return repositoryGroupAdmin.deleteRepositoryGroup( repositoryGroupId, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean addRepositoryToGroup( String repositoryGroupId, String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {
            return repositoryGroupAdmin.addRepositoryToGroup( repositoryGroupId, repositoryId, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean deleteRepositoryFromGroup( String repositoryGroupId, String repositoryId )
        throws ArchivaRestServiceException
    {
        try
        {
            return repositoryGroupAdmin.deleteRepositoryFromGroup( repositoryGroupId, repositoryId,
                                                                   getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }
}
