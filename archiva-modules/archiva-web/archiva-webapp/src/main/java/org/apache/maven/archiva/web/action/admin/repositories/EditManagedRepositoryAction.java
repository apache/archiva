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
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;

import java.io.File;
import java.io.IOException;

/**
 * AddManagedRepositoryAction
 *
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="editManagedRepositoryAction" instantiation-strategy="per-lookup"
 */
public class EditManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable, Validateable
{
    /**
     * The model for this action.
     */
    private ManagedRepositoryConfiguration repository;

    private ManagedRepositoryConfiguration stagingRepository;

    private String repoid;

    private final String action = "editRepository";

    private boolean stageNeeded;

    /**
     * @plexus.requirement
     */
    private RepositoryStatisticsManager repositoryStatisticsManager;

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
            stagingRepository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid + "-stage" );
        }
        else if ( repository != null )
        {
            repository.setReleases( false );
            repository.setScanned( false );
        }
    }

    public String input()
    {
        if ( repository == null )
        {
            addActionError( "Edit failure, unable to edit a repository with a blank repository id." );
            return ERROR;
        }

        return INPUT;
    }

    public String confirmUpdate()
    {
        // location was changed
        return save( true );
    }

    public String commit()
    {
        ManagedRepositoryConfiguration existingConfig =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repository.getId() );
        boolean resetStats = false;

        // check if the location was changed
        if ( !StringUtils.equalsIgnoreCase( existingConfig.getLocation().trim(), repository.getLocation().trim() ) )
        {
            resetStats = true;

            File dir = new File( repository.getLocation() );
            if ( dir.exists() )
            {
                return CONFIRM;
            }
        }

        return save( resetStats );
    }

    private String save( boolean resetStats )
    {
        // Ensure that the fields are valid.
        Configuration configuration = archivaConfiguration.getConfiguration();

        // We are in edit mode, remove the old repository configuration.
        removeRepository( repository.getId(), configuration );
        if ( stagingRepository != null )
        {
            removeRepository( stagingRepository.getId(), configuration );
        }

        // Save the repository configuration.
        String result;
        try
        {
            addRepository( repository, configuration );
            triggerAuditEvent( repository.getId(), null, AuditEvent.MODIFY_MANAGED_REPO );
            addRepositoryRoles( repository );

            //update changes of the staging repo
            if ( stageNeeded )
            {

                stagingRepository = getStageRepoConfig( configuration );
                addRepository( stagingRepository, configuration );
                addRepositoryRoles( stagingRepository );

            }
            //delete staging repo when we dont need it
            if ( !stageNeeded )
            {
                stagingRepository = getStageRepoConfig(configuration);
                removeRepository( stagingRepository.getId(), configuration );
                removeContents( stagingRepository );
                removeRepositoryRoles( stagingRepository );
            }

            result = saveConfiguration( configuration );
            if ( resetStats )
            {
                resetStatistics();
            }
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Role Manager Exception: " + e.getMessage() );
            result = ERROR;
        }

        return result;
    }

    private ManagedRepositoryConfiguration getStageRepoConfig( Configuration configuration )
    {
        for ( ManagedRepositoryConfiguration repoConf : configuration.getManagedRepositories() )
        {
            if ( repoConf.getId().equals( repository.getId() + "-stage" ) )
            {
                stagingRepository = repoConf;
                removeRepository( repoConf .getId() , configuration);
                updateStagingRepository( stagingRepository );
                return stagingRepository;
            }
        }

        stagingRepository = new ManagedRepositoryConfiguration();
        updateStagingRepository( stagingRepository );

        return stagingRepository;
    }

    private void updateStagingRepository( ManagedRepositoryConfiguration stagingRepository )
    {
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
    }

    @Override
    public void validate()
    {
        CronExpressionValidator validator = new CronExpressionValidator();

        if ( !validator.validate( repository.getRefreshCronExpression() ) )
        {
            addFieldError( "repository.refreshCronExpression", "Invalid cron expression." );
        }
    }

    private void resetStatistics()
    {
        repositoryStatisticsManager.deleteStatistics( repository.getId() );
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public boolean isStageNeeded()
    {
        return stageNeeded;
    }

    public void setStageNeeded( boolean stageNeeded )
    {
        this.stageNeeded = stageNeeded;
    }

    public String getAction()
    {
        return action;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }

    public ManagedRepositoryConfiguration getStagingRepository()
    {
        return stagingRepository;
    }

    public void setStagingRepository( ManagedRepositoryConfiguration stagingRepository )
    {
        this.stagingRepository = stagingRepository;
    }
}
