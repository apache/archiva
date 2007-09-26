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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.IOException;

/**
 * DeleteManagedRepositoryAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="deleteManagedRepositoryAction"
 */
public class DeleteManagedRepositoryAction
    extends AbstractManagedRepositoriesAction
    implements Preparable
{
    private ManagedRepositoryConfiguration repository;

    private String repoid;

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoid );
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
        if ( existingRepository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        String result = SUCCESS;

        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();
            removeRepository( repoid, configuration );
            result = saveConfiguration( configuration );

            if ( result.equals( SUCCESS ) )
            {
                cleanupRepositoryData( existingRepository );

                if ( deleteContents )
                {
                    removeContents( existingRepository );
                }
            }
        }
        catch ( IOException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }

        return result;
    }

    private void cleanupRepositoryData( ManagedRepositoryConfiguration cleanupRepository )
        throws RoleManagerException
    {
        removeRepositoryRoles( cleanupRepository );

        // TODO: [MRM-382] Remove index from artifacts of deleted managed repositories.

        // TODO: [MRM-265] After removing a managed repository - Browse/Search still see it
        
        // TODO: [MRM-520] Proxy Connectors are not deleted with the deletion of a Repository.
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
