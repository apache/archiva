package org.apache.maven.archiva.web.util;

/*
* Copyright 2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.security.rbac.Operation;
import org.codehaus.plexus.security.rbac.Permission;
import org.codehaus.plexus.security.rbac.RBACManager;
import org.codehaus.plexus.security.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.security.rbac.RbacStoreException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.rbac.Role;
import org.codehaus.plexus.security.rbac.UserAssignment;

/**
 * DefaultRoleManager:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id:$
 * @plexus.component role="org.apache.maven.archiva.web.util.RoleManager"
 * role-hint="default"
 */
public class DefaultRoleManager
    implements RoleManager, Initializable
{

    /**
     * @plexus.requirement
     */
    private RBACManager manager;

    private boolean initialized;

    public void initialize()
        throws InitializationException
    {

        // initialize the operations

        if ( !manager.operationExists( "add-repository" ) )
        {
            Operation operation = manager.createOperation( "add-repository" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "edit-repository" ) )
        {
            Operation operation = manager.createOperation( "edit-repository" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "delete-repository" ) )
        {
            Operation operation = manager.createOperation( "delete-repository" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "edit-configuration" ) )
        {
            Operation operation = manager.createOperation( "edit-configuration" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "run-indexer" ) )
        {
            Operation operation = manager.createOperation( "run-indexer" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "regenerate-index" ) )
        {
            Operation operation = manager.createOperation( "regenerate-index" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "get-reports" ) )
        {
            Operation operation = manager.createOperation( "get-reports" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "regenerate-reports" ) )
        {
            Operation operation = manager.createOperation( "regenerate-reports" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "edit-user" ) )
        {
            Operation operation = manager.createOperation( "edit-user" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "edit-all-users" ) )
        {
            Operation operation = manager.createOperation( "edit-all-users" );
            manager.saveOperation( operation );
        }

        if ( !manager.operationExists( "remove-roles" ) )
        {
            Operation operation = manager.createOperation( "remove-roles" );
            manager.saveOperation( operation );
        }

        try
        {
            if ( !manager.permissionExists( "Edit Configuration" ) )
            {
                Permission editConfiguration = manager.createPermission( "Edit Configuration", "edit-configuration",
                                                                         manager.getGlobalResource().getIdentifier() );
                manager.savePermission( editConfiguration );
            }

            if ( !manager.permissionExists( "Run Indexer" ) )
            {
                Permission runIndexer = manager.createPermission( "Run Indexer", "run-indexer",
                                                                  manager.getGlobalResource().getIdentifier() );

                manager.savePermission( runIndexer );
            }

            if ( !manager.permissionExists( "Add Repository" ) )
            {
                Permission runIndexer = manager.createPermission( "Add Repository", "add-repository",
                                                                  manager.getGlobalResource().getIdentifier() );
                manager.savePermission( runIndexer );
            }

            if ( !manager.permissionExists( "Edit All Users" ) )
            {
                Permission editAllUsers = manager.createPermission( "Edit All Users", "edit-all-users",
                                                                    manager.getGlobalResource().getIdentifier() );

                manager.savePermission( editAllUsers );
            }

            if ( !manager.permissionExists( "Remove Roles" ) )
            {
                Permission editAllUsers = manager.createPermission( "Remove Roles", "remove-roles",
                                                                    manager.getGlobalResource().getIdentifier() );

                manager.savePermission( editAllUsers );
            }

            if ( !manager.permissionExists( "Regenerate Index" ) )
            {
                Permission regenIndex = manager.createPermission( "Regenerate Index", "regenerate-index",
                                                                  manager.getGlobalResource().getIdentifier() );

                manager.savePermission( regenIndex );
            }

            if ( !manager.roleExists( "User Administrator" ) )
            {
                Role userAdmin = manager.createRole( "User Administrator" );
                userAdmin.addPermission( manager.getPermission( "Edit All Users" ) );
                userAdmin.addPermission( manager.getPermission( "Remove Roles" ) );
                userAdmin.setAssignable( true );
                manager.saveRole( userAdmin );
            }

            if ( !manager.roleExists( "System Administrator" ) )
            {
                Role admin = manager.createRole( "System Administrator" );
                admin.addChildRole( manager.getRole( "User Administrator" ) );
                admin.addPermission( manager.getPermission( "Edit Configuration" ) );
                admin.addPermission( manager.getPermission( "Run Indexer" ) );
                admin.addPermission( manager.getPermission( "Add Repository" ) );
                admin.addPermission( manager.getPermission( "Regenerate Index" ) );
                admin.setAssignable( true );
                manager.saveRole( admin );
            }


        }
        catch ( RbacObjectNotFoundException ne )
        {
            throw new InitializationException( "error in role initialization", ne );
        }

        initialized = true;
    }

    public void addUser( String principal )
        throws RbacStoreException
    {

        // make the resource
        Resource usernameResource = manager.createResource( principal );
        manager.saveResource( usernameResource );

        Permission editUser = manager.createPermission( "Edit Myself - " + principal, "edit-user", principal );
        editUser = manager.savePermission( editUser );

        // todo this one role a user will go away when we have expressions in the resources
        Role userRole = manager.createRole( "Personal Role - " + principal );
        userRole.addPermission( editUser );
        userRole = manager.saveRole( userRole );

        UserAssignment assignment = manager.createUserAssignment( principal );
        assignment.addRole( userRole );
        manager.saveUserAssignment( assignment );

    }

    public void addRepository( String repositoryName )
        throws RbacStoreException
    {
        try
        {
            // make the resource
            Resource repoResource = manager.createResource( repositoryName );
            repoResource = manager.saveResource( repoResource );

            // make the permissions
            Permission editRepo = manager.createPermission( "Edit Repository - " + repositoryName );
            editRepo.setOperation( manager.getOperation( "edit-repository" ) );
            editRepo.setResource( repoResource );
            editRepo = manager.savePermission( editRepo );

            Permission deleteRepo = manager.createPermission( "Delete Repository - " + repositoryName );
            deleteRepo.setOperation( manager.getOperation( "delete-repository" ) );
            deleteRepo.setResource( repoResource );
            deleteRepo = manager.savePermission( deleteRepo );

            Permission getReports = manager.createPermission( "Get Reports - " + repositoryName );
            getReports.setOperation( manager.getOperation( "get-reports" ) );
            getReports.setResource( repoResource );
            getReports = manager.savePermission( getReports );

            Permission regenReports = manager.createPermission( "Regenerate Reports - " + repositoryName );
            regenReports.setOperation( manager.getOperation( "regenerate-reports" ) );
            regenReports.setResource( repoResource );
            regenReports = manager.savePermission( regenReports );

            // make the roles
            Role repositoryObserver = manager.createRole( "Repository Manager - " + repositoryName );
            repositoryObserver.addPermission( editRepo );
            repositoryObserver.setAssignable( true );
            repositoryObserver = manager.saveRole( repositoryObserver );

            Role repositoryManager = manager.createRole( "Repository Manager - " + repositoryName );
            repositoryManager.addPermission( editRepo );
            repositoryManager.addPermission( deleteRepo );
            repositoryManager.addPermission( regenReports );
            repositoryManager.addChildRole( repositoryObserver );
            repositoryManager.setAssignable( true );
            manager.saveRole( repositoryManager );

        }
        catch ( RbacObjectNotFoundException ne )
        {
            throw new RbacStoreException( "rbac object not found in repo role creation", ne );
        }
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public void setInitialized( boolean initialized )
    {
        this.initialized = initialized;
    }
}
