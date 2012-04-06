package org.codehaus.plexus.redback.struts2.action.admin;

/*
 * Copyright 2005-2006 The Codehaus.
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
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.RedbackActionSupport;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.role.RoleConstants;
import org.codehaus.redback.integration.util.PermissionSorter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * PermissionsAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-permissions" )
@Scope( "prototype" )
public class PermissionsAction
    extends RedbackActionSupport
{
    private static final String LIST = "list";

    // ------------------------------------------------------------------
    // Plexus Component Requirements
    // ------------------------------------------------------------------

    /**
     *  role-hint="cached"
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager manager;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private String name;

    private String description;

    private String operationName;

    private String operationDescription;

    private String resourceIdentifier;

    private List<Permission> allPermissions;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String list()
    {
        try
        {
            allPermissions = manager.getAllPermissions();

            if ( allPermissions == null )
            {
                allPermissions = Collections.emptyList();
            }

            Collections.sort( allPermissions, new PermissionSorter() );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.list.all.permissions", Arrays.asList( (Object) e.getMessage() ) ) );
            log.error( "System error:", e );
            allPermissions = Collections.emptyList();
        }

        return LIST;
    }

    public String input()
    {
        if ( name == null )
        {
            addActionError( getText( "cannot.edit.null.permission" ) );
            return ERROR;
        }

        if ( StringUtils.isEmpty( name ) )
        {
            addActionError( getText( "cannot.edit.empty.permission" ) );
            return ERROR;
        }

        if ( !manager.permissionExists( name ) )
        {
            // Means that the permission name doesn't exist.
            // We should exit early and not attempt to look up the permission information.
            return LIST;
        }

        try
        {
            Permission permission = manager.getPermission( name );
            if ( permission == null )
            {
                addActionError( getText( "cannot.operate.null.permission" ) );
                return ERROR;
            }

            description = permission.getDescription();
            Operation operation = permission.getOperation();
            if ( operation != null )
            {
                operationName = operation.getName();
                operationDescription = operation.getDescription();
            }

            Resource resource = permission.getResource();
            if ( resource != null )
            {
                resourceIdentifier = resource.getIdentifier();
            }
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.get.permission", Arrays.asList( (Object) name, e.getMessage() ) ) );
            return ERROR;
        }

        return LIST;
    }

    public String submit()
    {
        if ( name == null )
        {
            addActionError( getText( "cannot.edit.null.permission" ) );
            return ERROR;
        }

        if ( StringUtils.isEmpty( name ) )
        {
            addActionError( getText( "cannot.edit.empty.permission" ) );
            return ERROR;
        }

        try
        {
            Permission permission;
            if ( manager.permissionExists( name ) )
            {
                permission = manager.getPermission( name );
            }
            else
            {
                permission = manager.createPermission( name );
            }

            permission.setDescription( description );

            Operation operation = manager.createOperation( operationName );
            if ( StringUtils.isNotEmpty( operationDescription ) )
            {
                operation.setDescription( operationDescription );
            }
            permission.setOperation( manager.saveOperation( operation ) );

            Resource resource = manager.createResource( resourceIdentifier );
            permission.setResource( manager.saveResource( resource ) );

            manager.savePermission( permission );

            addActionMessage( getText( "save.permission.success", Arrays.asList( (Object) name ) ) );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.get.permission", Arrays.asList( (Object) name, e.getMessage() ) ) );
            return ERROR;
        }

        return LIST;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getOperationDescription()
    {
        return operationDescription;
    }

    public void setOperationDescription( String operationDescription )
    {
        this.operationDescription = operationDescription;
    }

    public String getOperationName()
    {
        return operationName;
    }

    public void setOperationName( String operationName )
    {
        this.operationName = operationName;
    }

    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    public void setResourceIdentifier( String resourceIdentifier )
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    public List<Permission> getAllPermissions()
    {
        return allPermissions;
    }

    public void setAllPermissions( List<Permission> allPermissions )
    {
        this.allPermissions = allPermissions;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION, Resource.GLOBAL );
        return bundle;
    }
}
