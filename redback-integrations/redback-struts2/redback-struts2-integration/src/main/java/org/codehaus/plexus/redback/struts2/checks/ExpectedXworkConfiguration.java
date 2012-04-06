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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.redback.system.check.EnvironmentCheck;
import org.codehaus.redback.integration.checks.xwork.XworkPackageConfig;

import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;

/**
 * <p/>
 * ExpectedXworkConfiguration reason for existence is to validate that the executing
 * environment has everything needed for a proper execution of
 * Plexus Security :: UI Web components and javascript and jsps.
 * </p>
 * <p/>
 * <p/>
 * It is quite possible for the environment overlay to have not been done.
 * Such as when using <code>"mvn jetty:run"</code>, but forgetting to run
 * <code>"mvn war:inplace"</code> first.
 * </p>
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * TODO: Address comment below and add back in the component declaration
 *
 */
public class ExpectedXworkConfiguration
    extends AbstractXworkConfigurationCheck
    implements EnvironmentCheck
{	
    public void validateEnvironment( List<String> violations )
    {
        // Get the configuration.
        
        Configuration xworkConfig = new ConfigurationManager().getConfiguration();

        if ( xworkConfig != null )
        {
            List<String> internalViolations = new ArrayList<String>();

            /* PLXREDBACK-67
             * TODO: this currently throws a violation since the standard practice is
             * to include the xwork-security namespace in from the war overlay.  Otherwise
             * all actions in the security namespace are also addressable from the 
             * root default action lookup since by extending the security package thats how
             * webwork/xwork deals with the actions
             */
            XworkPackageConfig expectedPackage = new XworkPackageConfig( "/security" );

            expectedPackage.addAction( "account", "redback-account", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "login", "redback-login", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "logout", "redback-logout", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "register", "redback-register", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "password", "redback-password", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            // -----------------------------------------------------------------
            // Security Admin Tests

            expectedPackage.addAction( "systeminfo", "redback-sysinfo", "show" );
            expectedPackage.addAction( "adminConsole", "redback-admin-console", "show" );

            expectedPackage.addAction( "userlist", "redback-admin-user-list", "show" ).addResult( "input" ).addResult(
                "success" );

            expectedPackage.addAction( "useredit", "redback-admin-user-edit", "edit" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "usercreate", "redback-admin-user-create", "edit" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "userdelete", "redback-admin-user-delete", "confirm" ).addResult(
                "input" ).addResult( "error" ).addResult( "success" );

            expectedPackage.addAction( "assignments", "redback-assignments", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "roles", "redback-roles", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            expectedPackage.addAction( "permissions", "redback-permissions", "show" ).addResult( "input" ).addResult(
                "error" ).addResult( "success" );

            checkPackage( internalViolations, expectedPackage, xworkConfig );

            if ( internalViolations.size() > 0 )
            {
                violations.addAll( internalViolations );
                violations.add( "Missing [" + internalViolations.size() + "] xwork.xml configuration elements." );
            }
        }
        else
        {
            violations.add( "Missing xwork.xml configuration." );
        }
    }

}
