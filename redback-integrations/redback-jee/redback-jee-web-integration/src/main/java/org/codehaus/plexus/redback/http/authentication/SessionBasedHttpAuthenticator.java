package org.codehaus.plexus.redback.http.authentication;

import javax.servlet.http.HttpSession;

import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.users.User;

/**
 * An HttpAuthenticator using an HttpSession for session storage
 *
 * @author Andrew Williams
 * @version $Id$
 * @since 1.0
 */
public abstract class SessionBasedHttpAuthenticator
    extends AbstractHttpAuthenticator
{

    protected Object getSessionValue( Object session, String key )
    {
        if ( !( session instanceof HttpSession ) )
        {
            throw new IllegalArgumentException( "The session for a SessionBasedAuthenticator must be a javax.servlet.http.HttpSession" );
        }

        return ( (HttpSession) session ).getAttribute( key );
    }

    protected void setSessionValue( Object session, String key, Object value )
    {
        if ( !( session instanceof HttpSession ) )
        {
            throw new IllegalArgumentException( "The session for a SessionBasedAuthenticator must be a javax.servlet.http.HttpSession" );
        }

        ( (HttpSession) session ).setAttribute( key, value );
    }

    public AuthenticationResult authenticate( AuthenticationDataSource ds, HttpSession session )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        return super.authenticate( ds, session );
    }

    public User getSessionUser( HttpSession session )
    {
        return super.getSessionUser( session );
    }

    public boolean isAlreadyAuthenticated( HttpSession session )
    {
        return super.isAlreadyAuthenticated( session );
    }

    public SecuritySession getSecuritySession( HttpSession session )
    {
        return super.getSecuritySession( session );
    }

    public void setSecuritySession( SecuritySession session, HttpSession sessionObj )
    {
        super.setSecuritySession( session, sessionObj );
    }

    public void setSessionUser( User user, HttpSession session )
    {
        super.setSessionUser( user, session );
    }

    public String storeDefaultUser( String principal, HttpSession session )
    {
        return super.storeDefaultUser( principal, session );
    }
}
