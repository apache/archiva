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
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.IOException;

/**
 * EditRemoteRepositoryAction 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="editRemoteRepositoryAction" 
 */
public class EditRemoteRepositoryAction
    extends AbstractRemoteRepositoriesAction
    implements Preparable
{
    /**
     * The model for this action.
     */
    private RemoteRepositoryConfiguration repository;

    /**
     * The repository id to edit.
     */
    private String repoid;

    public void prepare()
    {
        String id = repoid;
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = archivaConfiguration.getConfiguration().findRemoteRepositoryById( id );
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
        Configuration configuration = archivaConfiguration.getConfiguration();
        
        // We are in edit mode, remove the old repository configuration.
        removeRepository( repository.getId(), configuration );

        // Save the repository configuration.
        String result;
        try
        {
            addRepository( repository, configuration );
            result = saveConfiguration( configuration );
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
            result = INPUT;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Role Manager Exception: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }
    
    public RemoteRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( RemoteRepositoryConfiguration repository )
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
