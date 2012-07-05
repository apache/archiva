package org.apache.archiva.redback.struts2.action.admin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.struts2.action.AbstractSecurityAction;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.struts2.action.AuditEvent;
import org.apache.archiva.redback.struts2.action.CancellableAction;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.apache.archiva.redback.integration.role.RoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;

/**
 * UserDeleteAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Controller( "redback-admin-user-delete" )
@Scope( "prototype" )
public class UserDeleteAction
    extends AbstractSecurityAction
    implements CancellableAction
{
    // ------------------------------------------------------------------
    // Component Requirements
    // ------------------------------------------------------------------

    /**
     *  role-hint="configurable"
     */
    @Inject
    @Named( value = "userManager#configurable" )
    private UserManager userManager;

    /**
     *  role-hint="cached"
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager rbacManager;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private String username;

    private User user;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String confirm()
    {
        if ( username == null )
        {
            addActionError( getText( "cannot.remove.user.null.username" ) );
            return SUCCESS;
        }

        try
        {
            user = userManager.findUser( username );
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.remove.user.not.found", Arrays.asList( (Object) username ) ) );
            return SUCCESS;
        }

        return INPUT;
    }

    public String submit()
    {
        if ( username == null )
        {
            addActionError( getText( "invalid.user.credentials" ) );
            return SUCCESS;
        }

        if ( StringUtils.isEmpty( username ) )
        {
            addActionError( getText( "cannot.remove.user.empty.username" ) );
            return SUCCESS;
        }

        try
        {
            rbacManager.removeUserAssignment( username );
        }
        catch ( RbacObjectNotFoundException e )
        {
            // ignore, this is possible since the user may never have had roles assigned
        }
        catch ( RbacObjectInvalidException e )
        {
            addActionError( getText( "cannot.remove.user.role", Arrays.asList( (Object) username, e.getMessage() ) ) );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.remove.user.role", Arrays.asList( (Object) username, e.getMessage() ) ) );
        }

        if ( getActionErrors().isEmpty() )
        {
            try
            {
                userManager.deleteUser( username );
            }
            catch ( UserNotFoundException e )
            {
                addActionError( getText( "cannot.remove.user.non.existent", Arrays.asList( (Object) username ) ) );
            }
        }
        String currentUser = getCurrentUser();

        AuditEvent event = new AuditEvent( getText( "log.account.delete" ) );
        event.setAffectedUser( username );
        event.setCurrentUser( currentUser );
        event.log();

        return SUCCESS;
    }

    /**
     * Returns the cancel result. <p/> A basic implementation would simply be to return CANCEL.
     *
     * @return
     */
    public String cancel()
    {
        return CANCEL;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION, Resource.GLOBAL );
        return bundle;
    }

}
