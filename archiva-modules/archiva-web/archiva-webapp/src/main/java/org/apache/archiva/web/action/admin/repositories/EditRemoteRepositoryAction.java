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
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexException;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexScheduler;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

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

    private boolean now, fullDownload;

    @Inject
    private DownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

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

    public String downloadRemoteIndex()
    {
        try
        {
            downloadRemoteIndexScheduler.scheduleDownloadRemote( repoid, now, fullDownload );
        }
        catch ( DownloadRemoteIndexException e )
        {
            addActionError( "DownloadRemoteIndexException: " + e.getMessage() );
            return INPUT;
        }
        return SUCCESS;
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

    public boolean isNow()
    {
        return now;
    }

    public void setNow( boolean now )
    {
        this.now = now;
    }

    public boolean isFullDownload()
    {
        return fullDownload;
    }

    public void setFullDownload( boolean fullDownload )
    {
        this.fullDownload = fullDownload;
    }
}
