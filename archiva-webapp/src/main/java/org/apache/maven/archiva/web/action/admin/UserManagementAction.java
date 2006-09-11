package org.apache.maven.archiva.web.action.admin;


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

import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import org.codehaus.plexus.security.rbac.RBACManager;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * LoginAction:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id:$
 * @plexus.component role="com.opensymphony.xwork.Action"
 * role-hint="userManagement"
 */
public class UserManagementAction
    extends PlexusActionSupport
    implements ModelDriven, Preparable
{

    /**
     * @plexus.requirement
     */
    private UserManager userManager;

    /**
     * @plexus.requirement
     */
    private RBACManager rbacManager;

    private User user;

    private String username;

    private String principal;

    private List availableRoles;

    private List assignedRoles;

    private List resources;

    private String resourceName;

    public void prepare()
        throws Exception
    {
        if ( username == null )
        {
            username = ( (User) session.get( "user" ) ).getUsername();
            user = userManager.findUser( username );
        }
        else
        {
            user = userManager.findUser( username );
        }

        resources = rbacManager.getAllResources();

        availableRoles = rbacManager.getAllAssignableRoles();

        principal = ( (User) session.get( "user" ) ).getPrincipal().toString();

        if ( principal != null && rbacManager.userAssignmentExists( principal ) )
        {
            getLogger().info( "recovering assigned roles" );
            assignedRoles = new ArrayList( rbacManager.getAssignedRoles( principal ) );
            availableRoles = new ArrayList( rbacManager.getUnassignedRoles( principal ) );
        }
        else
        {
            getLogger().info( "new assigned roles" );
            assignedRoles = new ArrayList();
            availableRoles = rbacManager.getAllAssignableRoles();

        }

        getLogger().info( "assigned roles: " + assignedRoles.size() );
        getLogger().info( "available roles: " + availableRoles.size() );
    }

    public String save()
        throws Exception
    {
        User temp = userManager.findUser( username );

        temp.setEmail( user.getEmail() );
        temp.setFullName( user.getFullName() );
        temp.setLocked( user.isLocked() );

        userManager.updateUser( temp );

        return SUCCESS;
    }

    public Object getModel()
    {
        return user;
    }

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

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public List getAvailableRoles()
    {
        return availableRoles;
    }

    public void setAvailableRoles( List availableRoles )
    {
        this.availableRoles = availableRoles;
    }

    public List getAssignedRoles()
    {
        return assignedRoles;
    }

    public void setAssignedRoles( List assignedRoles )
    {
        this.assignedRoles = assignedRoles;
    }

    public List getResources()
    {
        return resources;
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public void setResourceName( String resourceName )
    {
        this.resourceName = resourceName;
    }
}
