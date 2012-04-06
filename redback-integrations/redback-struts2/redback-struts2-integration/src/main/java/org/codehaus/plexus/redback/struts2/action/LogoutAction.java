package org.codehaus.plexus.redback.struts2.action;

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

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.SessionMap;
import org.codehaus.plexus.cache.Cache;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.util.AutoLoginCookies;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * LogoutAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-logout" )
@Scope( "prototype" )
public class LogoutAction
    extends AbstractSecurityAction
{
    // Result Names.
    private static final String LOGOUT = "security-logout";

    /**
     * cache used for user assignments
     *
     *  role-hint="userAssignments"
     */
    @Inject
    @Named( value = "cache#userAssignments" )
    private Cache userAssignmentsCache;

    /**
     * cache used for user permissions
     *
     *  role-hint="userPermissions"
     */
    @Inject
    @Named( value = "cache#userPermissions" )
    private Cache userPermissionsCache;

    /**
     * Cache used for users
     *
     *  role-hint="users"
     */
    @Inject
    @Named( value = "cache#users" )
    private Cache usersCache;

    /**
     *
     */
    @Inject
    private AutoLoginCookies autologinCookies;

    public String logout()
    {
        if ( getSecuritySession().getUser() == null )
        {
            return LOGOUT;
        }

        String currentUser = (String) getSecuritySession().getUser().getPrincipal();

        if ( getSecuritySession() != null )
        {
            // [PLXREDBACK-65] this is a bit of a hack around the cached managers since they don't have the ability to 
            // purge their caches through the API.  Instead try and bring them in here and invalidate 
            // the keys directly.  This will not be required once we move to a different model for pre-calculated
            // permission sets since that will not have the overhead that required these caches in the first place.
            Object principal = (String) getSecuritySession().getUser().getPrincipal();
            if ( userAssignmentsCache != null )
            {
                userAssignmentsCache.remove( principal );
            }
            if ( userPermissionsCache != null )
            {
                userPermissionsCache.remove( principal );
            }
            if ( usersCache != null )
            {
                usersCache.remove( principal );
            }
        }

        autologinCookies.removeRememberMeCookie( ServletActionContext.getResponse(),
                                                 ServletActionContext.getRequest() );
        autologinCookies.removeSignonCookie( ServletActionContext.getResponse(), ServletActionContext.getRequest() );

        setAuthTokens( null );

        if ( session != null )
        {
            ( (SessionMap) session ).invalidate();
        }

        AuditEvent event = new AuditEvent( getText( "log.logout.success" ) );
        event.setAffectedUser( currentUser );
        event.log();

        return LOGOUT;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        return SecureActionBundle.OPEN;
    }
}
