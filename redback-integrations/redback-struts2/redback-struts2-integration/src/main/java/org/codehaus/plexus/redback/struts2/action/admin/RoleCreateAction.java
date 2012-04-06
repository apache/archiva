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

import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.struts2.action.AbstractSecurityAction;
import org.codehaus.plexus.redback.struts2.action.AuditEvent;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.model.SimplePermission;
import org.codehaus.redback.integration.role.RoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RoleCreateAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-role-create" )
@Scope( "prototype" )
public class RoleCreateAction
    extends AbstractSecurityAction
{
    // ------------------------------------------------------------------
    //  Component Requirements
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

    private String principal;

    private String roleName;

    private String description;

    private List<SimplePermission> permissions;

    private List<String> childRoles;

    private SimplePermission addpermission;

    private String submitMode;

    protected static final String VALID_ROLENAME_CHARS = "[a-zA-Z_0-9\\-\\s.,]*";

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        if ( permissions == null )
        {
            permissions = new ArrayList<SimplePermission>();
        }

        if ( childRoles == null )
        {
            childRoles = new ArrayList<String>();
        }

        if ( addpermission == null )
        {
            addpermission = new SimplePermission();
        }

        return INPUT;
    }

    public String addpermission()
    {
        if ( addpermission == null )
        {
            addActionError( getText( "cannot.add.null.permission" ) );
            return ERROR;
        }

        if ( permissions == null )
        {
            permissions = new ArrayList<SimplePermission>();
        }

        permissions.add( addpermission );

        addpermission = new SimplePermission();

        return INPUT;
    }

    public String submit()
    {
        if ( StringUtils.equals( getSubmitMode(), "addPermission" ) )
        {
            return addpermission();
        }

        if ( StringUtils.isEmpty( roleName ) )
        {
            addActionError( getText( "cannot.add.empty.role" ) );
            return ERROR;
        }
        if ( !roleName.matches( VALID_ROLENAME_CHARS ) )
        {
            addActionError( getText( "roleName.invalid.characters" ) );
            return ERROR;
        }

        try
        {
            Role _role;
            if ( manager.roleExists( roleName ) )
            {
                _role = manager.getRole( roleName );
            }
            else
            {
                _role = manager.createRole( roleName );
            }

            _role.setDescription( description );
            _role.setChildRoleNames( childRoles );

            List<Permission> _permissionList = new ArrayList<Permission>();
            for ( SimplePermission perm : permissions )
            {
                _permissionList.add(
                    manager.createPermission( perm.getName(), perm.getOperationName(), perm.getResourceIdentifier() ) );
            }

            _role.setPermissions( _permissionList );

            manager.saveRole( _role );

            addActionMessage( getText( "save.role.success", Arrays.asList( (Object) roleName ) ) );
            String currentUser = getCurrentUser();
            AuditEvent event = new AuditEvent( getText( "log.role.create" ) );
            event.setRole( roleName );
            event.setCurrentUser( currentUser );
            event.log();
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.get.role", Arrays.asList( (Object) roleName, e.getMessage() ) ) );
            return ERROR;
        }

        return SUCCESS;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public SimplePermission getAddpermission()
    {
        return addpermission;
    }

    public void setAddpermission( SimplePermission addpermission )
    {
        this.addpermission = addpermission;
    }

    public String getSubmitMode()
    {
        return submitMode;
    }

    public void setSubmitMode( String submitMode )
    {
        this.submitMode = submitMode;
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
