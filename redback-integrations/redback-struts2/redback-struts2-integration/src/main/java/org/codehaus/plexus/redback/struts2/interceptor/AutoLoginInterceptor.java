package org.codehaus.plexus.redback.struts2.interceptor;

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

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.apache.struts2.ServletActionContext;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authentication.TokenBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.keys.AuthenticationKey;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.redback.integration.util.AutoLoginCookies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

/**
 * AutoLoginInterceptor
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Controller( "redbackAutoLoginInterceptor" )
@Scope( "prototype" )
public class AutoLoginInterceptor
    implements Interceptor
{
    private Logger log = LoggerFactory.getLogger( AutoLoginInterceptor.class );

    static final String PASSWORD_CHANGE = "security-must-change-password";

    static final String ACCOUNT_LOCKED = "security-login-locked";

    /**
     *
     */
    @Inject
    private SecuritySystem securitySystem;

    /**
     *
     */
    @Inject
    private AutoLoginCookies autologinCookies;

    public void destroy()
    {
        // Ignore
    }

    public void init()
    {
        // Ignore
    }

    /**
     * @noinspection ProhibitedExceptionDeclared
     */
    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        SecuritySession securitySession = getSecuritySession();

        if ( securitySession != null && securitySession.isAuthenticated() )
        {
            // User already authenticated.
            log.debug( "User already authenticated." );

            if ( !checkCookieConsistency( securitySession ) )
            {
                // update single sign on cookie
                autologinCookies.setSignonCookie( securitySession.getUser().getUsername(),
                                                  ServletActionContext.getResponse(),
                                                  ServletActionContext.getRequest() );
            }
        }
        else
        {
            AuthenticationKey authkey =
                autologinCookies.getSignonKey( ServletActionContext.getResponse(), ServletActionContext.getRequest() );

            if ( authkey != null )
            {
                try
                {
                    securitySession = checkAuthentication( authkey, invocation.getInvocationContext().getName().equals(
                        PASSWORD_CHANGE ) );

                    if ( securitySession != null && securitySession.isAuthenticated() )
                    {
                        ActionContext.getContext().getSession().put( SecuritySystemConstants.SECURITY_SESSION_KEY,
                                                                     securitySession );
                        checkCookieConsistency( securitySession );
                    }
                    else
                    {
                        autologinCookies.removeSignonCookie( ServletActionContext.getResponse(),
                                                             ServletActionContext.getRequest() );
                        autologinCookies.removeRememberMeCookie( ServletActionContext.getResponse(),
                                                                 ServletActionContext.getRequest() );
                    }
                }
                catch ( AccountLockedException e )
                {
                    log.info( "Account Locked : Username [{}]", e.getUser().getUsername(), e );
                    autologinCookies.removeSignonCookie( ServletActionContext.getResponse(),
                                                         ServletActionContext.getRequest() );
                    autologinCookies.removeRememberMeCookie( ServletActionContext.getResponse(),
                                                             ServletActionContext.getRequest() );
                    return ACCOUNT_LOCKED;
                }
                catch ( MustChangePasswordException e )
                {
                    return PASSWORD_CHANGE;
                }
            }
            else if ( autologinCookies.isRememberMeEnabled() )
            {
                authkey = autologinCookies.getRememberMeKey( ServletActionContext.getResponse(),
                                                             ServletActionContext.getRequest() );

                if ( authkey != null )
                {
                    try
                    {
                        securitySession = checkAuthentication( authkey, false );

                        if ( securitySession == null || !securitySession.isAuthenticated() )
                        {
                            autologinCookies.removeRememberMeCookie( ServletActionContext.getResponse(),
                                                                     ServletActionContext.getRequest() );
                        }
                    }
                    catch ( AccountLockedException e )
                    {
                        log.info( "Account Locked : Username [{}]", e.getUser().getUsername(), e );
                        autologinCookies.removeRememberMeCookie( ServletActionContext.getResponse(),
                                                                 ServletActionContext.getRequest() );
                        return ACCOUNT_LOCKED;
                    }
                    catch ( MustChangePasswordException e )
                    {
                        return PASSWORD_CHANGE;
                    }
                }
            }
        }

        return invocation.invoke();
    }

    private boolean checkCookieConsistency( SecuritySession securitySession )
    {
        String username = securitySession.getUser().getUsername();

        boolean failed = false;

        AuthenticationKey key =
            autologinCookies.getRememberMeKey( ServletActionContext.getResponse(), ServletActionContext.getRequest() );
        if ( key != null )
        {
            if ( !key.getForPrincipal().equals( username ) )
            {
                log.debug( "Login invalidated: remember me cookie was for{}; but session was for {}",
                           key.getForPrincipal(), username );
                failed = true;
            }
        }

        if ( !failed )
        {
            key =
                autologinCookies.getSignonKey( ServletActionContext.getResponse(), ServletActionContext.getRequest() );
            if ( key != null )
            {
                if ( !key.getForPrincipal().equals( username ) )
                {
                    log.debug( "Login invalidated: signon cookie was for {}; but session was for {}",
                               key.getForPrincipal(), username );
                    failed = true;
                }
            }
            else
            {
                log.debug( "Login invalidated: signon cookie was removed" );
                failed = true;
            }
        }

        if ( failed )
        {
            removeCookiesAndSession();
        }

        return failed;
    }

    private SecuritySession checkAuthentication( AuthenticationKey authkey, boolean enforcePasswordChange )
        throws AccountLockedException, MustChangePasswordException
    {
        SecuritySession securitySession = null;
        log.debug( "Logging in with an authentication key: {}", authkey.getForPrincipal() );
        TokenBasedAuthenticationDataSource authsource = new TokenBasedAuthenticationDataSource();
        authsource.setPrincipal( authkey.getForPrincipal() );
        authsource.setToken( authkey.getKey() );
        authsource.setEnforcePasswordChange( enforcePasswordChange );

        try
        {
            securitySession = securitySystem.authenticate( authsource );

            if ( securitySession.isAuthenticated() )
            {
                // TODO: this should not happen if there is a password change required - but the password change action needs to log the user in on success to swap them
                log.debug( "Login success." );

                HttpSession session = ServletActionContext.getRequest().getSession( true );
                session.setAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY, securitySession );
                log.debug( "Setting session:{} to {}", SecuritySystemConstants.SECURITY_SESSION_KEY, securitySession );

                autologinCookies.setSignonCookie( authkey.getForPrincipal(), ServletActionContext.getResponse(),
                                                  ServletActionContext.getRequest() );
            }
            else
            {
                AuthenticationResult result = securitySession.getAuthenticationResult();
                log.info( "Login interceptor failed against principal : {}", result.getPrincipal(),
                          result.getException() );
            }

        }
        catch ( AuthenticationException e )
        {
            log.info( "Authentication Exception.", e );
        }
        catch ( UserNotFoundException e )
        {
            log.info( "User Not Found: {}", authkey.getForPrincipal(), e );
        }
        return securitySession;
    }

    private void removeCookiesAndSession()
    {
        autologinCookies.removeRememberMeCookie( ServletActionContext.getResponse(),
                                                 ServletActionContext.getRequest() );
        autologinCookies.removeSignonCookie( ServletActionContext.getResponse(), ServletActionContext.getRequest() );

        HttpSession session = ServletActionContext.getRequest().getSession();
        if ( session != null )
        {
            session.removeAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY );
        }
    }

    private SecuritySession getSecuritySession()
    {
        HttpSession session = ServletActionContext.getRequest().getSession();
        if ( session == null )
        {
            log.debug( "No HTTP Session exists." );
            return null;
        }

        SecuritySession secSession =
            (SecuritySession) session.getAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY );
        log.debug( "Returning Security Session: {}", secSession );
        return secSession;
    }
}
