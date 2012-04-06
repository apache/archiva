package org.codehaus.plexus.redback.authorization;

import org.codehaus.plexus.redback.users.User;

/**
 * @author Jason van Zyl
 */
public class AuthorizationDataSource
{
    Object principal;

    User user;

    Object permission;

    Object resource;

    public AuthorizationDataSource( Object principal, User user, Object permission )
    {
        this.principal = principal;
        this.user = user;
        this.permission = permission;
    }

    public AuthorizationDataSource( Object principal, User user, Object permission, Object resource )
    {
        this.principal = principal;
        this.user = user;
        this.permission = permission;
        this.resource = resource;
    }

    public Object getPrincipal()
    {
        return principal;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public Object getPermission()
    {
        return permission;
    }

    public void setPermission( Object permission )
    {
        this.permission = permission;
    }

    public Object getResource()
    {
        return resource;
    }

    public void setResource( Object resource )
    {
        this.resource = resource;
    }
}
