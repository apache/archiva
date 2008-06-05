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

import com.opensymphony.xwork.Preparable;
import com.opensymphony.xwork.Validateable;

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
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="editManagedRepositoryAction"
 */
public class EditManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable, Validateable
{
    /**
     * The model for this action.
     */
    private ManagedRepositoryConfiguration repository;

    private String repoid;
    
    private final String action = "editRepository";

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
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
        return save();
    }
    
    public String commit()
    {   
        ManagedRepositoryConfiguration existingConfig =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repository.getId() );
        
        // check if the location was changed
        if( !StringUtils.equalsIgnoreCase( existingConfig.getLocation().trim(), repository.getLocation().trim() ) )
        {
            File dir = new File( repository.getLocation() );
            if( dir.exists() )
            {
                return CONFIRM;
            }
        }
        
        return save();
    }
    
    private String save()
    {
        // Ensure that the fields are valid.
        Configuration configuration = archivaConfiguration.getConfiguration();
        
        // We are in edit mode, remove the old repository configuration.
        removeRepository( repository.getId(), configuration );

        // Save the repository configuration.
        String result;
        try
        {
            addRepository( repository, configuration );
            addRepositoryRoles( repository );
            result = saveConfiguration( configuration );
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
    
    @Override
    public void validate()
    {
        CronExpressionValidator validator = new CronExpressionValidator();

        if ( !validator.validate( repository.getRefreshCronExpression() ) )
        {
            addFieldError( "repository.refreshCronExpression", "Invalid cron expression." );
        }
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
    
    public String getAction()
    {
        return action;
    }
}
