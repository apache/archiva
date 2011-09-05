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
import org.apache.archiva.admin.repository.remote.RemoteRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * EditRemoteRepositoryAction
 *
 * @version $Id$
 */
@Controller( "editRemoteRepositoryAction" )
@Scope( "prototype" )
public class EditRemoteRepositoryAction
    extends AbstractRemoteRepositoriesAction
    implements Preparable
{
    /**
     * The model for this action.
     */
    private RemoteRepository repository;

    /**
     * The repository id to edit.
     */
    private String repoid;

    public void prepare()
        throws RepositoryAdminException
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = getRemoteRepositoryAdmin().getRemoteRepository( repoid );
        }
    }

    public String input()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Edit failure, unable to edit a repository with a blank repository id." );
            return ERROR;
        }

        return INPUT;
    }

    public String commit()
    {
        String result = SUCCESS;
        try
        {
            getRemoteRepositoryAdmin().updateRemoteRepository( getRepository(), getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "RepositoryAdminException: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }

    public RemoteRepository getRepository()
    {
        return repository;
    }

    public void setRepository( RemoteRepository repository )
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
