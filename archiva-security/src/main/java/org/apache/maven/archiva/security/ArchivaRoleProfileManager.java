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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.rbac.profile.DefaultRoleProfileManager;
import org.codehaus.plexus.rbac.profile.RoleProfileException;

/**
 * Role profile manager.
 *
 * @author Brett Porter
 * @todo composition over inheritence?
 * @plexus.component role="org.codehaus.plexus.rbac.profile.RoleProfileManager" role-hint="archiva"
 */
public class ArchivaRoleProfileManager
    extends DefaultRoleProfileManager
{
    public void initialize()
        throws RoleProfileException
    {
        getRole( "archiva-repository-manager-base" );

        mergeRoleProfiles( "system-administrator", "archiva-system-administrator" );
        mergeRoleProfiles( "user-administrator", "archiva-user-administrator" );
        mergeRoleProfiles( "guest", "archiva-guest" );
        setInitialized( true ); //todo remove the initialization idea from profile managers
    }
}
