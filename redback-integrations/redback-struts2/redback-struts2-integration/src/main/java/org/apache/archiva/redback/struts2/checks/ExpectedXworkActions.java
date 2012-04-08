package org.apache.archiva.redback.struts2.checks;

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

import java.util.List;

import org.apache.archiva.redback.system.check.EnvironmentCheck;

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
        String classNames[] = new String[]{"org.apache.archiva.redback.struts2.action.admin.UserCreateAction",
            "org.apache.archiva.redback.struts2.action.admin.UserDeleteAction",
            "org.apache.archiva.redback.struts2.action.admin.UserEditAction",
            "org.apache.archiva.redback.struts2.action.admin.UserListAction",
            "org.apache.archiva.redback.struts2.action.AccountAction",
            "org.apache.archiva.redback.struts2.action.LoginAction",
            "org.apache.archiva.redback.struts2.action.LogoutAction",
            "org.apache.archiva.redback.struts2.action.PasswordAction",
            "org.apache.archiva.redback.struts2.action.RegisterAction",
            "org.apache.archiva.redback.struts2.action.admin.AdminConsoleAction",
            "org.apache.archiva.redback.struts2.action.admin.SystemInfoAction"};

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
