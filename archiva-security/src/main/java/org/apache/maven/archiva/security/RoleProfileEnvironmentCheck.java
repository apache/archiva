package org.apache.maven.archiva.security;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
import org.codehaus.plexus.rbac.profile.RoleProfileManager;
import org.codehaus.plexus.security.system.check.EnvironmentCheck;

import java.util.List;

/**
 * @plexus.component role="org.codehaus.plexus.security.system.check.EnvironmentCheck"
 * role-hint="archiva-role-profile-check"
 * @todo isn't this standard? Shouldn't it be something initializable so it doesn't need to be checked all the time?
 */
public class RoleProfileEnvironmentCheck
    extends AbstractLogEnabled
    implements EnvironmentCheck
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private RoleProfileManager roleProfileManager;

    public void validateEnvironment( List list )
    {
        try
        {
            if ( !roleProfileManager.isInitialized() )
            {
                roleProfileManager.initialize();
            }
        }
        catch ( RoleProfileException rpe )
        {
            list.add( "error inititalizing the role manager: " + rpe.getMessage() );
        }
    }
}
