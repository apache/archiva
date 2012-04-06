package org.codehaus.plexus.redback.struts2.action;

/*
 * Copyright 2009 The Codehaus.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AuditEvent
{
    private Logger logger = LoggerFactory.getLogger( AuditEvent.class.getName() );

    private final String action;

    private String affectedUser;

    private String role;

    private String currentUser;

    public AuditEvent( String action )
    {
        this.action = action;
    }

    public void setRole( String role )
    {
        this.role = role;
    }

    public String getRole()
    {
        return role;
    }

    public void setAffectedUser( String affectedUser )
    {
        this.affectedUser = affectedUser;
    }

    public String getAffectedUser()
    {
        return affectedUser;
    }

    public void setCurrentUser( String currentUser )
    {
        this.currentUser = currentUser;
    }

    public String getCurrentUser()
    {
        return currentUser;
    }

    public void log()
    {
        // TODO: it would be better to push this into the login interceptor so it is always set consistently 
        //   (same for IP address)
        if ( currentUser != null )
        {
            MDC.put( "redback.currentUser", currentUser );
        }

        if ( affectedUser != null )
        {
            if ( role != null )
            {
                logger.info( action, affectedUser, role );
            }
            else
            {
                logger.info( action, affectedUser );

            }
        }
    }
}
