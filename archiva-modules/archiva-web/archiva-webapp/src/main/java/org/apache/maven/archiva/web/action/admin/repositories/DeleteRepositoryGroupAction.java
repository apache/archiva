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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.repository.audit.AuditEvent;

/**
 * DeleteRepositoryGroupAction
 * 
 * @version
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="deleteRepositoryGroupAction" instantiation-strategy="per-lookup"
 */
public class DeleteRepositoryGroupAction 
    extends AbstractRepositoriesAdminAction
    implements Preparable
{
    private RepositoryGroupConfiguration repositoryGroup;

    private String repoGroupId;
	
    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoGroupId ) )
        {
            this.repositoryGroup = archivaConfiguration.getConfiguration().findRepositoryGroupById( repoGroupId );
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
        Configuration config = archivaConfiguration.getConfiguration();

        RepositoryGroupConfiguration group = config.findRepositoryGroupById( repoGroupId );
        if ( group == null )
        {
            addActionError( "A repository group with that id does not exist." );
            return ERROR;
        }
		
        config.removeRepositoryGroup( group );
        triggerAuditEvent( AuditEvent.DELETE_REPO_GROUP + " " + repoGroupId );
        return saveConfiguration( config );
    }
	
    public RepositoryGroupConfiguration getRepositoryGroup()
    {
        return repositoryGroup;
    }

    public void setRepositoryGroup( RepositoryGroupConfiguration repositoryGroup )
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
}
