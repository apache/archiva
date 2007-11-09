package org.apache.maven.archiva.web.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork.ActionContext;

import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.security.ArchivaUser;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.users.User;

import java.util.HashMap;
import java.util.Map;

/**
 * ArchivaXworkUser 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.security.ArchivaUser"
 *                   role-hint="xwork"
 */
public class ArchivaXworkUser
    implements ArchivaUser
{
    private Map<String, Object> getContextSession()
    {
        ActionContext context = ActionContext.getContext();
        Map<String, Object> sessionMap = context.getSession();
        if ( sessionMap == null )
        {
            sessionMap = new HashMap<String, Object>();
        }

        return sessionMap;
    }

    private SecuritySession getSecuritySession()
    {
        return (SecuritySession) getContextSession().get( SecuritySystemConstants.SECURITY_SESSION_KEY );        
    }

    public String getActivePrincipal()
    {
        SecuritySession securitySession = getSecuritySession();        
        
        if ( securitySession == null )
        {
            return ArchivaRoleConstants.PRINCIPAL_GUEST;
        }

        User user = securitySession.getUser();        
        if ( user == null )
        {
            return ArchivaRoleConstants.PRINCIPAL_GUEST;
        }

        return (String) user.getPrincipal();
    }
}
