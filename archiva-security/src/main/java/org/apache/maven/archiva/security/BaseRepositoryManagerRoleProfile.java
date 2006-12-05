package org.apache.maven.archiva.security;

import org.codehaus.plexus.rbac.profile.AbstractRoleProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * @plexus.component role="org.codehaus.plexus.rbac.profile.RoleProfile"
 * role-hint="archiva-repository-manager-base"
 */
public class BaseRepositoryManagerRoleProfile
    extends AbstractRoleProfile
{
    /**
     * Create the Role name for a Repository Manager, using the provided repository id.
     *
     */
    public String getRoleName( )
    {
        return ArchivaRoleConstants.BASE_REPOSITORY_MANAGER;
    }

    public List getOperations()
    {
        List operations = new ArrayList();

        operations.add( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION );

        return operations;
    }

    public boolean isPermanent()
    {
        return true;
    }

    public boolean isAssignable()
    {
        return false;
    }
}
