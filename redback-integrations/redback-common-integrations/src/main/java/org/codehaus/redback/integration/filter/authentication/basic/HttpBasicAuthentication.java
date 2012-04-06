package org.codehaus.redback.integration.filter.authentication.basic;

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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.filter.authentication.HttpAuthenticator;
import org.springframework.stereotype.Service;

/**
 * HttpBasicAuthentication
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("httpAuthenticator#basic")
public class HttpBasicAuthentication
    extends HttpAuthenticator
{

    public String getId()
    {
        return HttpBasicAuthentication.class.getName();
    }

    public AuthenticationResult getAuthenticationResult( HttpServletRequest request, HttpServletResponse response )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        HttpSession httpSession = request.getSession( true );
        SecuritySession securitySession = getSecuritySession( httpSession );
        if ( securitySession != null )
        {
            return securitySession.getAuthenticationResult();
        }

        PasswordBasedAuthenticationDataSource authDataSource;
        String header = request.getHeader( "Authorization" );

        // in tomcat this is : authorization=Basic YWRtaW46TWFuYWdlMDc=
        if ( header == null )
        {
            header = request.getHeader( "authorization" );
        }

        if ( ( header != null ) && header.startsWith( "Basic " ) )
        {
            String base64Token = header.substring( 6 );
            String token = new String( Base64.decodeBase64( base64Token.getBytes() ) );

            String username = "";
            String password = "";
            int delim = token.indexOf( ':' );

            if ( delim != ( -1 ) )
            {
                username = token.substring( 0, delim );
                password = token.substring( delim + 1 );
            }

            authDataSource = new PasswordBasedAuthenticationDataSource( username, password );
            return super.authenticate( authDataSource, httpSession );
        }
        else
        {
            return null;
        }
    }

    /**
     * Return a HTTP 403 - Access Denied response.
     *
     * @param request   the request to use.
     * @param response  the response to use.
     * @param realmName the realm name to state.
     * @param exception the exception to base the message off of.
     * @throws IOException if there was a problem with the {@link HttpServletResponse#sendError(int,String)} call.
     */
    public void challenge( HttpServletRequest request, HttpServletResponse response, String realmName,
                           AuthenticationException exception )
        throws IOException
    {
        response.addHeader( "WWW-Authenticate", "Basic realm=\"" + realmName + "\"" );
        String message = "You must provide a username and password to access this resource.";
        if ( ( exception != null ) && StringUtils.isNotEmpty( exception.getMessage() ) )
        {
            message = exception.getMessage();
        }
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, message );
    }
}
