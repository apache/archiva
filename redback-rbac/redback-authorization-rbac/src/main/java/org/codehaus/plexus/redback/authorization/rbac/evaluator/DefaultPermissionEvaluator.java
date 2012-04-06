package org.codehaus.plexus.redback.authorization.rbac.evaluator;

import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * DefaultPermissionEvaluator:
 * <p/>
 * Currently only one expression is available for evaluation, ${username} will be replaced with the username
 * of the person making the authorization check
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 * @version $Id$
 */
@Service("permissionEvaluator")
public class DefaultPermissionEvaluator
    implements PermissionEvaluator
{
    @Inject
    @Named(value="userManager#configurable")
    private UserManager userManager;

    public boolean evaluate( Permission permission, Object operation, Object resource, Object principal )
        throws PermissionEvaluationException
    {
        String permissionResource = permission.getResource().getIdentifier();

        // expression evaluation checking
        if ( permissionResource.startsWith( "${" ) )
        {
            String tempStr = permissionResource.substring( 2, permissionResource.indexOf( '}' ) );

            if ( "username".equals( tempStr ) )
            {
                try
                {
                    permissionResource = userManager.findUser( principal.toString() ).getUsername();
                }
                catch ( UserNotFoundException ne )
                {
                    throw new PermissionEvaluationException( "unable to locate user to retrieve username", ne );
                }
            }
        }

        // check if this permission applies to the operation at all
        if ( permission.getOperation().getName().equals( operation.toString() ) )
        {
            // check if it is a global resource, if it is then since the operations match we return true
            if ( Resource.GLOBAL.equals( permission.getResource().getIdentifier() ) )
            {
                return true;
            }

            // if we are not checking a specific resource, the operation is enough
            if ( resource == null )
            {
                return true;
            }
            
            // check if the resource identifier of the permission matches the resource we are checking against
            // if it does then return true
            if ( permissionResource.equals( resource.toString() ) )
            {
                return true;
            }
        }

        return false;
    }

    public UserManager getUserManager()
    {
        return userManager;
    }

    public void setUserManager( UserManager userManager )
    {
        this.userManager = userManager;
    }
}
