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
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * DeleteManagedRepositoryAction
 *
 * @version $Id$
 *          plexus.component role="com.opensymphony.xwork2.Action" role-hint="deleteManagedRepositoryAction" instantiation-strategy="per-lookup"
 */
@Controller( "deleteManagedRepositoryAction" )
@Scope( "prototype" )
public class DeleteManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable
{

    /**
     * FIXME we must manipulate beans from repo admin api
     * The model for this action.
     */
    private ManagedRepositoryConfiguration repository;

    private ManagedRepositoryConfiguration stagingRepository;

    private String repoid;

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
            this.stagingRepository =
                archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid + "-stage" );
        }
    }

    public String confirmDelete()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Unable to delete managed repository: repository id was blank." );
            return ERROR;
        }

        return INPUT;
    }

    public String deleteEntry()
    {
        return deleteRepository( false );
    }

    public String deleteContents()
    {
        return deleteRepository( true );
    }

    private String deleteRepository( boolean deleteContents )
    {
        ManagedRepositoryConfiguration existingRepository = repository;
        ManagedRepositoryConfiguration attachedStagingRepo = stagingRepository;
        if ( existingRepository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        String result = SUCCESS;

        try
        {
            getManagedRepositoryAdmin().deleteManagedRepository( existingRepository.getId(), getAuditInformation(),
                                                                 deleteContents );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError(
                "Unable to delete repository, content may already be partially removed: " + e.getMessage() );
            log.error( e.getMessage(), e );
            result = ERROR;
        }
        return result;
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }
}
