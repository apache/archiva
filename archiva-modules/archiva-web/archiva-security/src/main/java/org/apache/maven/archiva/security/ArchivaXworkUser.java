package org.apache.maven.archiva.security;

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

import java.util.Map;

import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.registry.Registry;

/**
 * ArchivaXworkUser 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.security.ArchivaXworkUser"
 */
public class ArchivaXworkUser
{
    /**
     * @plexus.requirement role-hint="commons-configuration"
     */
    private Registry registry;
    
    private static final String KEY = "org.codehaus.plexus.redback";
    
    private static String guest;
            
    public String getActivePrincipal( Map<String, Object> sessionMap )
    {   
        if ( sessionMap == null )
        {
            return getGuest();
        }

    	SecuritySession securitySession =
            (SecuritySession) sessionMap.get( SecuritySystemConstants.SECURITY_SESSION_KEY );

        if ( securitySession == null )
        {
            securitySession = (SecuritySession) sessionMap.get( SecuritySession.ROLE );
        }

        if ( securitySession == null )
        {
            return getGuest();
        }

        User user = securitySession.getUser();        
        if ( user == null )
        {
            return getGuest();
        }

        return (String) user.getPrincipal();
    }    
   
    public String getGuest()
    {
        if( guest == null || "".equals( guest ) )
        {
            Registry subset = registry.getSubset( KEY );
            guest = subset.getString( "redback.default.guest", ArchivaRoleConstants.PRINCIPAL_GUEST );
        }
        
        return guest;
    }
    
    public void setGuest( String guesT )
    {
        guest = guesT;
    }
}
