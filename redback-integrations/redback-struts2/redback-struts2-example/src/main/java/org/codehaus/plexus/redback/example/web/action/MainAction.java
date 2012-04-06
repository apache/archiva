package org.codehaus.plexus.redback.example.web.action;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.struts2.action.RedbackActionSupport;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

/**
 * MainAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "main" )
@Scope( "prototype" )
public class MainAction
    extends RedbackActionSupport
{
    @Inject
    private SecuritySystem securitySystem;

    @Inject
    private RoleManager roleManager;

    public String show()
    {
        if ( securitySystem == null )
        {
            session.put( "SecuritySystemWARNING", "SecuritySystem is null!" );
        }
        else
        {
            session.put( "security_id_authenticator", securitySystem.getAuthenticatorId() );
            session.put( "security_id_authorizor", securitySystem.getAuthorizerId() );
            session.put( "security_id_user_management", securitySystem.getUserManagementId() );
        }

        try
        {

            if ( !roleManager.templatedRoleExists( "template1", "Test Resource A" ) )
            {
                roleManager.createTemplatedRole( "template1", "Test Resource A" );
            }
            if ( !roleManager.templatedRoleExists( "template2", "Test Resource A" ) )
            {
                roleManager.createTemplatedRole( "template2", "Test Resource A" );
            }
            if ( !roleManager.templatedRoleExists( "template3", "Test Resource A" ) )
            {
                roleManager.createTemplatedRole( "template3", "Test Resource A" );
            }
            if ( !roleManager.templatedRoleExists( "template1", "Test Resource B" ) )
            {
                roleManager.createTemplatedRole( "template1", "Test Resource B" );
            }
            if ( !roleManager.templatedRoleExists( "template2", "Test Resource B" ) )
            {
                roleManager.createTemplatedRole( "template2", "Test Resource B" );
            }
            if ( !roleManager.templatedRoleExists( "template3", "Test Resource B" ) )
            {
                roleManager.createTemplatedRole( "template3", "Test Resource B" );
            }
        }
        catch ( RoleManagerException e )
        {
            e.printStackTrace();
        }
        return SUCCESS;
    }
}
