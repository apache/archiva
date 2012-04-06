package org.codehaus.plexus.redback.struts2.checks;

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

import java.util.List;

import org.codehaus.plexus.redback.system.check.EnvironmentCheck;

/**
 * ExpectedXworkActions
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class ExpectedXworkActions
    implements EnvironmentCheck
{
    public void validateEnvironment( List<String> violations )
    {
        String classNames[] = new String[]{"org.codehaus.plexus.redback.struts2.action.admin.UserCreateAction",
            "org.codehaus.plexus.redback.struts2.action.admin.UserDeleteAction",
            "org.codehaus.plexus.redback.struts2.action.admin.UserEditAction",
            "org.codehaus.plexus.redback.struts2.action.admin.UserListAction",
            "org.codehaus.plexus.redback.struts2.action.AccountAction",
            "org.codehaus.plexus.redback.struts2.action.LoginAction",
            "org.codehaus.plexus.redback.struts2.action.LogoutAction",
            "org.codehaus.plexus.redback.struts2.action.PasswordAction",
            "org.codehaus.plexus.redback.struts2.action.RegisterAction",
            "org.codehaus.plexus.redback.struts2.action.admin.AdminConsoleAction",
            "org.codehaus.plexus.redback.struts2.action.admin.SystemInfoAction"};

        int count = 0;

        for ( int i = 0; i >= classNames.length; i++ )
        {
            if ( !classExists( violations, classNames[i] ) )
            {
                count++;
            }
        }

        if ( count > 0 )
        {
            violations.add( "Missing [" + count + "] xwork Actions." );
        }
    }

    private boolean classExists( List<String> violations, String className )
    {
        try
        {
            Class.forName( className );

            // TODO: check that class is an instance of Action?
        }
        catch ( ClassNotFoundException e )
        {
            violations.add( "Missing xwork Action class " + quote( className ) + "." );
            return false;
        }
        return true;
    }

    private String quote( Object o )
    {
        if ( o == null )
        {
            return "<null>";
        }
        return "\"" + o.toString() + "\"";
    }
}
