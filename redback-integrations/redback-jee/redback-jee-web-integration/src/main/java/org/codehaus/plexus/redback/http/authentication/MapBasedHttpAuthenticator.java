package org.codehaus.plexus.redback.http.authentication;

import java.util.Map;

import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.users.User;

/**
 * An HttpAuthenticator using a Map for session storage
 *
 * @author Andrew Williams
 * @version $Id$
 * @since 1.0
 */
public abstract class MapBasedHttpAuthenticator
    extends AbstractHttpAuthenticator
{

    protected Object getSessionValue( Object session, String key )
    {
        if ( !( session instanceof Map ) )
        {
            throw new IllegalArgumentException( "The session for a MapBasedAuthenticator must be a java.util.Map" );
        }

        return ( (Map) session ).get( key );
    }

    protected void setSessionValue( Object session, String key, Object value )
    {
        if ( !( session instanceof Map ) )
        {
            throw new IllegalArgumentException( "The session for a MapBasedAuthenticator must be a java.util.Map" );
        }

        ( (Map) session ).put( key, value );
    }

    public AuthenticationResult authenticate( AuthenticationDataSource ds, Map session )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        return super.authenticate( ds, session );
    }

    public User getSessionUser( Map session )
    {
        return super.getSessionUser( session );
    }

    public boolean isAlreadyAuthenticated( Map session )
    {
        return super.isAlreadyAuthenticated( session );
    }

    public SecuritySession getSecuritySession( Map session )
    {
        return super.getSecuritySession( session );
    }

    public void setSecuritySession( SecuritySession session, Map sessionObj )
    {
        super.setSecuritySession( session, sessionObj );
    }

    public void setSessionUser( User user, Map session )
    {
        super.setSessionUser( user, session );
    }

    public String storeDefaultUser( String principal, Map session )
    {
        return super.storeDefaultUser( principal, session );
    }
}
