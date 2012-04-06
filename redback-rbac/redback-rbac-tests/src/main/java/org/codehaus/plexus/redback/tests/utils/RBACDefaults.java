package org.codehaus.plexus.redback.tests.utils;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.codehaus.plexus.redback.rbac.Operation;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.Role;

public class RBACDefaults
{
    private final RBACManager manager;

    public RBACDefaults( RBACManager manager )
    {
        this.manager = manager;
    }

    public RBACManager createDefaults()
        throws RbacManagerException
    {
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

        if ( !manager.permissionExists( "Edit Configuration" ) )
        {
            Permission editConfiguration = manager.createPermission( "Edit Configuration", "edit-configuration",
                                                                     manager.getGlobalResource().getIdentifier() );
            manager.savePermission( editConfiguration );
        }

        if ( !manager.permissionExists( "Run Indexer" ) )
        {
            Permission runIndexer = manager.createPermission( "Run Indexer", "run-indexer", manager.getGlobalResource()
                .getIdentifier() );

            manager.savePermission( runIndexer );
        }

        if ( !manager.permissionExists( "Add Repository" ) )
        {
            Permission runIndexer = manager.createPermission( "Add Repository", "add-repository", manager
                .getGlobalResource().getIdentifier() );
            manager.savePermission( runIndexer );
        }

        if ( !manager.permissionExists( "Edit All Users" ) )
        {
            Permission editAllUsers = manager.createPermission( "Edit All Users", "edit-all-users", manager
                .getGlobalResource().getIdentifier() );

            manager.savePermission( editAllUsers );
        }

        if ( !manager.permissionExists( "Remove Roles" ) )
        {
            Permission editAllUsers = manager.createPermission( "Remove Roles", "remove-roles", manager
                .getGlobalResource().getIdentifier() );

            manager.savePermission( editAllUsers );
        }

        if ( !manager.permissionExists( "Regenerate Index" ) )
        {
            Permission regenIndex = manager.createPermission( "Regenerate Index", "regenerate-index", manager
                .getGlobalResource().getIdentifier() );

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
            admin.addChildRoleName( "User Administrator" );
            admin.addPermission( manager.getPermission( "Edit Configuration" ) );
            admin.addPermission( manager.getPermission( "Run Indexer" ) );
            admin.addPermission( manager.getPermission( "Add Repository" ) );
            admin.addPermission( manager.getPermission( "Regenerate Index" ) );
            admin.setAssignable( true );
            manager.saveRole( admin );
        }

        if ( !manager.roleExists( "Trusted Developer" ) )
        {
            Role developer = manager.createRole( "Trusted Developer" );
            developer.addChildRoleName( "System Administrator" );
            developer.addPermission( manager.getPermission( "Run Indexer" ) );
            developer.setAssignable( true );
            manager.saveRole( developer );
        }

        if ( !manager.roleExists( "Developer" ) )
        {
            Role developer = manager.createRole( "Developer" );
            developer.addChildRoleName( "Trusted Developer" );
            developer.addPermission( manager.getPermission( "Run Indexer" ) );
            developer.setAssignable( true );
            manager.saveRole( developer );
        }

        return manager;
    }
}
