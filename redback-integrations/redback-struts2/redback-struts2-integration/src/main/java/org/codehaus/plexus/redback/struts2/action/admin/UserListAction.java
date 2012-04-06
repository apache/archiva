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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts2.ServletActionContext;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.struts2.action.AbstractSecurityAction;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserQuery;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.reports.Report;
import org.codehaus.redback.integration.reports.ReportManager;
import org.codehaus.redback.integration.role.RoleConstants;
import org.extremecomponents.table.context.Context;
import org.extremecomponents.table.context.HttpServletRequestContext;
import org.extremecomponents.table.limit.FilterSet;
import org.extremecomponents.table.limit.Limit;
import org.extremecomponents.table.limit.LimitFactory;
import org.extremecomponents.table.limit.TableLimit;
import org.extremecomponents.table.limit.TableLimitFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UserListAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-admin-user-list" )
@Scope( "prototype" )
public class UserListAction
    extends AbstractSecurityAction
{
    // ------------------------------------------------------------------
    // Component Requirements
    // ------------------------------------------------------------------

    /**
     *
     */
    @Inject
    private SecuritySystem securitySystem;

    /**
     *  role-hint="cached"
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager rbac;

    /**
     *
     */
    @Inject
    private ReportManager reportManager;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private List<User> users;

    private List<Role> roles;

    private String roleName;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        try
        {
            roles = rbac.getAllRoles();
        }
        catch ( RbacManagerException e )
        {
            roles = Collections.emptyList();
        }

        if ( StringUtils.isEmpty( roleName ) )
        {
            users = findUsersWithFilter();
        }
        else
        {
            roleName = StringEscapeUtils.escapeXml( roleName );

            try
            {
                Role target = rbac.getRole( roleName );
                Set<String> targetRoleNames = new HashSet<String>();

                for ( int i = 0; i < roles.size(); i++ )
                {
                    Role r = roles.get( i );
                    if ( rbac.getEffectiveRoles( r ).contains( target ) )
                    {
                        targetRoleNames.add( r.getName() );
                    }
                }

                users = findUsers( targetRoleNames );
            }
            catch ( RbacObjectNotFoundException e )
            {
                users = Collections.emptyList();
            }
            catch ( RbacManagerException e )
            {
                users = Collections.emptyList();
            }
        }

        if ( users == null )
        {
            users = Collections.emptyList();
        }

        return INPUT;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_ROLE_OPERATION, Resource.GLOBAL );
        return bundle;
    }

    private List<User> findUsers( Collection<String> roleNames )
    {
        List<String> usernames = getUsernamesForRoles( roleNames );
        List<User> filteredUsers = new ArrayList<User>();

        for ( User user : findUsersWithFilter() )
        {
            if ( usernames.contains( user.getUsername() ) )
            {
                filteredUsers.add( user );
            }
        }

        return filteredUsers;
    }

    private List<User> findUsersWithFilter()
    {
        Context context = new HttpServletRequestContext( ServletActionContext.getRequest() );
        LimitFactory limitFactory = new TableLimitFactory( context );
        Limit limit = new TableLimit( limitFactory );
        FilterSet filterSet = limit.getFilterSet();

        UserQuery query = getUserManager().createUserQuery();
        if ( filterSet.getFilter( "username" ) != null )
        {
            query.setUsername( filterSet.getFilter( "username" ).getValue() );
        }
        if ( filterSet.getFilter( "fullName" ) != null )
        {
            query.setFullName( filterSet.getFilter( "fullName" ).getValue() );
        }
        if ( filterSet.getFilter( "email" ) != null )
        {
            query.setEmail( filterSet.getFilter( "email" ).getValue() );
        }
        return getUserManager().findUsersByQuery( query );
    }

    private List<String> getUsernamesForRoles( Collection<String> roleNames )
    {
        Set<String> usernames = new HashSet<String>();

        try
        {
            List<UserAssignment> userAssignments = rbac.getUserAssignmentsForRoles( roleNames );

            if ( userAssignments != null )
            {
                for ( UserAssignment a : userAssignments )
                {
                    usernames.add( a.getPrincipal() );
                }
            }
        }
        catch ( RbacManagerException e )
        {
            log.warn( "Unable to get user assignments for roles " + roleNames, e );
        }

        return new ArrayList<String>( usernames );
    }

    private UserManager getUserManager()
    {
        return securitySystem.getUserManager();
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers( List<User> users )
    {
        this.users = users;
    }

    public String getRoleName()
    {
        if ( StringUtils.isEmpty( roleName ) )
        {
            return "Any";
        }
        return roleName;
    }

    public void setRoleName( String roleName )
    {
        this.roleName = roleName;
    }

    public List<Role> getRoles()
    {
        return roles;
    }

    public Map<String, Map<String, Report>> getReportMap()
    {
        return reportManager.getReportMap();
    }
}
