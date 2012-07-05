package org.apache.archiva.redback.struts2.action;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.apache.archiva.redback.policy.PasswordRuleViolationException;
import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystemConstants;
import org.apache.archiva.redback.integration.interceptor.SecureAction;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;

/**
 * AbstractSecurityAction
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public abstract class AbstractSecurityAction
    extends RedbackActionSupport
    implements SecureAction
{
    protected static final String REQUIRES_AUTHENTICATION = "requires-authentication";

    private SecureActionBundle securityBundle;

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        if ( securityBundle == null )
        {
            securityBundle = initSecureActionBundle();
        }

        return securityBundle;
    }

    public abstract SecureActionBundle initSecureActionBundle()
        throws SecureActionException;

    protected void setAuthTokens( SecuritySession securitySession )
    {
        session.put( SecuritySystemConstants.SECURITY_SESSION_KEY, securitySession );
        this.setSession( session );
    }

    protected SecuritySession getSecuritySession()
    {
        return (SecuritySession) session.get( SecuritySystemConstants.SECURITY_SESSION_KEY );
    }

    // ------------------------------------------------------------------
    // Internal Support Methods
    // ------------------------------------------------------------------
    protected void processPasswordRuleViolations( PasswordRuleViolationException e )
    {
        processPasswordRuleViolations( e, "user.password" );
    }

    protected void processPasswordRuleViolations( PasswordRuleViolationException e, String field )
    {
        PasswordRuleViolations violations = e.getViolations();

        if ( violations != null )
        {
            for ( String violation : violations.getLocalizedViolations() )
            {
                addFieldError( field, violation );
            }
        }
    }

    protected String getBaseUrl()
    {
        HttpServletRequest req = ServletActionContext.getRequest();
        return req.getScheme() + "://" + req.getServerName()
            + ( req.getServerPort() == 80 ? "" : ":" + req.getServerPort() ) + req.getContextPath();
    }

    protected String getCurrentUser()
    {
        SecuritySession securitySession = getSecuritySession();
        if ( securitySession != null && securitySession.getUser() != null )
        {
            return securitySession.getUser().getPrincipal().toString();
        }
        else
        {
            return null;
        }
    }
}
