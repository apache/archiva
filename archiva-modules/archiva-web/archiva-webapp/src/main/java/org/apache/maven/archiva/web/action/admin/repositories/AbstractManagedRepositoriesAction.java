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

import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.taskqueue.TaskQueueException;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Abstract ManagedRepositories Action.
 * <p/>
 * Place for all generic methods used in Managed Repository Administration.
 *
 * @version $Id$
 */
public abstract class AbstractManagedReposigettoriesAction
    extends AbstractRepositoriesAdminAction
{
    /**
     * plexus.requirement role-hint="default"
     */
    @Inject
    protected RoleManager roleManager;

    /**
     * Plexus registry to read the configuration from.
     * <p/>
     * plexus.requirement role-hint="commons-configuration"
     */
    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;
    
    /**
     * plexus.requirement role="org.apache.archiva.scheduler.ArchivaTaskScheduler" role-hint="repository"
     */
    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    public static final String CONFIRM = "confirm";

    public RoleManager getRoleManager()
    {
        return roleManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }

    public void setRegistry( Registry registry )
    {
        this.registry = registry;
    }
    
    public void setRepositoryTaskScheduler( RepositoryArchivaTaskScheduler repositoryTaskScheduler )
    {
        this.repositoryTaskScheduler = repositoryTaskScheduler;
    }

    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws IOException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        if ( !file.exists() || !file.isDirectory() )
        {
            throw new IOException(
                "Unable to add repository - no write access, can not create the root directory: " + file );
        }

        configuration.addManagedRepository( repository );

    }

    protected void addRepositoryRoles( ManagedRepositoryConfiguration newRepository )
        throws RoleManagerException
    {
        String repoId = newRepository.getId();

        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic

        if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }
    }

    protected void removeContents( ManagedRepositoryConfiguration existingRepository )
        throws IOException
    {
        File dir = new File( existingRepository.getLocation() );
        if ( dir.exists() && !FileUtils.deleteQuietly( dir ) )
        {
            throw new IOException( "Cannot delete repository " + dir );
        }
    }

    protected void removeRepository( String repoId, Configuration configuration )
    {
        ManagedRepositoryConfiguration toremove = configuration.findManagedRepositoryById( repoId );
        if ( toremove != null )
        {
            configuration.removeManagedRepository( toremove );
        }
    }

    protected void removeRepositoryRoles( ManagedRepositoryConfiguration existingRepository )
        throws RoleManagerException
    {
        String repoId = existingRepository.getId();

        if ( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }

        if ( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        log.debug( "removed user roles associated with repository {}", repoId );
    }

    protected String removeExpressions( String directory )
    {
        String value = StringUtils.replace( directory, "${appserver.base}",
                                            registry.getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
                                     registry.getString( "appserver.home", "${appserver.home}" ) );
        return value;
    } 
    
    //MRM-1342 Repository statistics report doesn't appear to be working correctly
    //provide a method to scan repository
    protected void executeRepositoryScanner( String repoId )
        throws TaskQueueException
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoId );
        
        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repoId ) )
        {
            repositoryTaskScheduler.queueTask( task );
        }
    }
}
