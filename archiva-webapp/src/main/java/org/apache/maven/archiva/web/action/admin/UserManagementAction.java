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

import org.codehaus.plexus.security.rbac.RBACManager;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;
import org.codehaus.plexus.security.user.UserNotFoundException;
import org.codehaus.plexus.security.system.SecuritySession;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * UserManagementAction: pulled from the class of the same name in plexus-security-ui-web
 * for integrating rbac with user information
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id:$
 * @plexus.component role="com.opensymphony.xwork.Action"
 * role-hint="userManagement"
 */
public class UserManagementAction
    extends PlexusActionSupport
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

    private String email;

    private String fullName;

    private boolean locked;

    private String username;

    private String principal;

    private List availableRoles;

    private List assignedRoles;

    private List resources;

    private String resourceName;

    /**
     * for this method username should be populated
     * 
     * @return
     */
    public String findUser()
    {
        try
        {
            if ( username == null )
            {
                return INPUT;
            }
            else
            {
                user = userManager.findUser( username );
                return SUCCESS;
            }
        }
        catch ( UserNotFoundException ne )
        {
            addActionError( "user could not be found "  + username );
            return ERROR;
        }
    }

    /**
     * For this method, principal should be populated
     *
     * @throws Exception
     */
    public String display()
        throws Exception
    {

        user = userManager.findUser( username );

        principal = user.getPrincipal().toString();
        fullName = user.getFullName();
        email = user.getEmail();
        locked = user.isLocked();

        // for displaying the potential repositories to be displayed, remove the global resource
        // from the list
        resources = rbacManager.getAllResources();
        //resources.remove( rbacManager.getGlobalResource() );

        // check if the user has any roles assigned to them, and populate the lists for
        // rendering assign and remove roles links
        if ( principal != null && rbacManager.userAssignmentExists( principal ) )
        {
            assignedRoles = new ArrayList( rbacManager.getAssignedRoles( principal ) );
            availableRoles = new ArrayList( rbacManager.getUnassignedRoles( principal ) );
        }
        else
        {
            assignedRoles = new ArrayList();
            availableRoles = rbacManager.getAllAssignableRoles();
        }

        return SUCCESS;
    }

    public String save()
        throws Exception
    {
        User temp = userManager.findUser( username );

        temp.setEmail( email );
        temp.setFullName( fullName );
        temp.setLocked( locked );

        temp = userManager.updateUser( temp );

        // overwrite the user in the session with the saved one if and only if it is the
        // save user as the person currently logged in
        User activeUser = (User) session.get( SecuritySession.USERKEY );
        if ( temp.getPrincipal().toString().equals( activeUser.getPrincipal().toString() ) )
        {
            session.put( SecuritySession.USERKEY, temp );
        }

        return SUCCESS;
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

    public void setUser( User user )
    {
        this.user = user;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public void setLocked( boolean locked )
    {
        this.locked = locked;
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
