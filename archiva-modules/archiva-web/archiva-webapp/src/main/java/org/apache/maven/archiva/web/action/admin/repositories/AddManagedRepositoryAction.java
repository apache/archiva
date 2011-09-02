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
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.redback.components.scheduler.CronExpressionValidator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.io.File;

/**
 * AddManagedRepositoryAction
 *
 * @version $Id$
 */
@Controller( "addManagedRepositoryAction" )
@Scope( "prototype" )
public class AddManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable, Validateable
{

    private ManagedRepository repository;

    private boolean stageNeeded;

    private String action = "addRepository";

    public void prepare()
    {
        this.repository = new ManagedRepository();
        this.repository.setReleases( false );
        this.repository.setScanned( false );
        this.repository.setBlockRedeployments( false );
    }

    public String input()
    {
        this.repository.setReleases( true );
        this.repository.setScanned( true );
        this.repository.setBlockRedeployments( true );

        return INPUT;
    }

    public String confirmAdd()
    {
        return save();
    }

    public String commit()
    {
        repository.setLocation( getManagedRepositoryAdmin().removeExpressions( repository.getLocation() ) );

        File location = new File( repository.getLocation() );
        if ( location.exists() )
        {
            return CONFIRM;
        }

        return save();
    }

    private String save()
    {
        String result = SUCCESS;
        try
        {
            getManagedRepositoryAdmin().addManagedRepository( repository, stageNeeded, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Check your server logs, Repository Administration Exception: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }

    // FIXME olamy dupe with admin repo componennt
    @Override
    public void validate()
    {
        Configuration config = archivaConfiguration.getConfiguration();

        CronExpressionValidator validator = new CronExpressionValidator();
        String repoId = repository.getId();

        if ( config.getManagedRepositoriesAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
                + "], that id already exists as a managed repository." );
        }
        else if ( config.getRemoteRepositoriesAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
                + "], that id already exists as a remote repository." );
        }
        else if ( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
                + "], that id already exists as a repository group." );
        }
        else if ( repoId.toLowerCase().contains( "stage" ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
                + "], repository id cannot contains word stage" );
        }

        if ( !validator.validate( repository.getCronExpression() ) )
        {
            addFieldError( "repository.refreshCronExpression", "Invalid cron expression." );
        }

        // trim all unecessary trailing/leading white-spaces; always put this statement before the closing braces(after all validation).
        trimAllRequestParameterValues();
    }

    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( repository.getId() ) )
        {
            repository.setId( repository.getId().trim() );
        }

        if ( StringUtils.isNotEmpty( repository.getName() ) )
        {
            repository.setName( repository.getName().trim() );
        }

        if ( StringUtils.isNotEmpty( repository.getLocation() ) )
        {
            repository.setLocation( repository.getLocation().trim() );
        }

        if ( StringUtils.isNotEmpty( repository.getIndexDirectory() ) )
        {
            repository.setIndexDirectory( repository.getIndexDirectory().trim() );
        }
    }

    public ManagedRepository getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepository repository )
    {
        this.repository = repository;
    }


    public void setStageNeeded( boolean stageNeeded )
    {
        this.stageNeeded = stageNeeded;
    }

    public String getAction()
    {
        return action;
    }
}
