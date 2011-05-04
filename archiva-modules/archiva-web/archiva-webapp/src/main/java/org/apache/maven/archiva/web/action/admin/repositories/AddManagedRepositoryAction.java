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
import org.apache.archiva.audit.AuditEvent;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;

import java.io.File;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;

/**
 * AddManagedRepositoryAction 
 *
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="addManagedRepositoryAction" instantiation-strategy="per-lookup"
 */
public class AddManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable, Validateable
{
    /**
     * The model for this action.
     */
    private ManagedRepositoryConfiguration repository;

    private boolean stageNeeded;

    private String action = "addRepository";

    public void prepare()
    {
        this.repository = new ManagedRepositoryConfiguration();
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
        repository.setLocation( removeExpressions( repository.getLocation() ) );

        File location = new File( repository.getLocation() );
        if( location.exists() )
        {
            return CONFIRM;
        }

        return save();
    }

    private String save()
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        String result;
        try
        {
            addRepository( repository, configuration );
            triggerAuditEvent( repository.getId(), null, AuditEvent.ADD_MANAGED_REPO );
            addRepositoryRoles( repository );

            if ( stageNeeded )
            {
                ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig();

                addRepository( stagingRepository, configuration );
                triggerAuditEvent( stagingRepository.getId(), null, AuditEvent.ADD_MANAGED_REPO );
                addRepositoryRoles( stagingRepository );

            }

            result = saveConfiguration( configuration );
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Role Manager Exception: " + e.getMessage() );
            result = INPUT;
        }
        catch ( IOException e )
        {
            addActionError( "Role Manager Exception: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }

    private ManagedRepositoryConfiguration getStageRepoConfig()
    {
        ManagedRepositoryConfiguration stagingRepository = new ManagedRepositoryConfiguration();
        stagingRepository.setId( repository.getId() + "-stage" );
        stagingRepository.setLayout( repository.getLayout() );
        stagingRepository.setName( repository.getName() + "-stage" );
        stagingRepository.setBlockRedeployments( repository.isBlockRedeployments() );
        stagingRepository.setDaysOlder( repository.getDaysOlder() );
        stagingRepository.setDeleteReleasedSnapshots( repository.isDeleteReleasedSnapshots() );
        stagingRepository.setIndexDir( repository.getIndexDir() );
        String path = repository.getLocation();
        int lastIndex = path.lastIndexOf( '/' );
        stagingRepository.setLocation( path.substring( 0, lastIndex ) + "/" + stagingRepository.getId() );
        stagingRepository.setRefreshCronExpression( repository.getRefreshCronExpression() );
        stagingRepository.setReleases( repository.isReleases() );
        stagingRepository.setRetentionCount( repository.getRetentionCount() );
        stagingRepository.setScanned( repository.isScanned() );
        stagingRepository.setSnapshots( repository.isSnapshots() );
        return stagingRepository;
    }

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
        else if( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
               + "], that id already exists as a repository group." );
        }
        else if ( repoId.toLowerCase().contains( "stage" ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId +
                "], rpository  id cannot contains word stage" );
        }

        if ( !validator.validate( repository.getRefreshCronExpression() ) )
        {
            addFieldError( "repository.refreshCronExpression", "Invalid cron expression." );
        }

        // trim all unecessary trailing/leading white-spaces; always put this statement before the closing braces(after all validation).
        trimAllRequestParameterValues();
    }

    private void trimAllRequestParameterValues()
    {
        if(StringUtils.isNotEmpty(repository.getId()))
        {
            repository.setId(repository.getId().trim());
        }

        if(StringUtils.isNotEmpty(repository.getName()))
        {
            repository.setName(repository.getName().trim());
        }

        if(StringUtils.isNotEmpty(repository.getLocation()))
        {
            repository.setLocation(repository.getLocation().trim());
        }
        
        if(StringUtils.isNotEmpty(repository.getIndexDir()))
        {
            repository.setIndexDir(repository.getIndexDir().trim());
        }
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
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
