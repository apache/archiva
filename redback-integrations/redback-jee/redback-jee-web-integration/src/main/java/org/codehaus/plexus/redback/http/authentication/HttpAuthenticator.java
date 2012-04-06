package org.codehaus.plexus.redback.http.authentication;

import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HttpAuthenticator
 *
 * @author Andrew Williams
 * @version $Id$
 */
public interface HttpAuthenticator
{
    /**
     * Entry point for a Filter.
     *
     * @param request
     * @param response
     * @throws AuthenticationException
     */
    void authenticate( HttpServletRequest request, HttpServletResponse response )
        throws AuthenticationException;

    /**
     * Issue a Challenge Response back to the HTTP Client.
     *
     * @param request
     * @param response
     * @param realmName
     * @param exception
     * @throws java.io.IOException
     */
    void challenge( HttpServletRequest request, HttpServletResponse response, String realmName,
                    AuthenticationException exception )
        throws IOException;

    /**
     * Parse the incoming request and return an AuthenticationResult.
     *
     * @param request
     * @param response
     * @return null if no http auth credentials, or the actual authentication result based on the credentials.
     * @throws AuthenticationException
     * @throws org.codehaus.plexus.redback.policy.MustChangePasswordException
     *
     * @throws org.codehaus.plexus.redback.policy.AccountLockedException
     *
     */
    AuthenticationResult getAuthenticationResult( HttpServletRequest request, HttpServletResponse response )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException;

}
