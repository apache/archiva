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

import org.codehaus.plexus.rbac.profile.AbstractDynamicRoleProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @plexus.component role="org.codehaus.plexus.rbac.profile.DynamicRoleProfile"
 * role-hint="archiva-repository-manager"
 */
public class RepsitoryManagerDynamicRoleProfile
    extends AbstractDynamicRoleProfile
{
    public String getRoleName( String string )
    {
        return ArchivaRoleConstants.REPOSITORY_MANAGER_ROLE_PREFIX + ArchivaRoleConstants.DELIMITER + string;
    }

    public List getOperations()
    {
        List operations = new ArrayList();

        // I'm not sure these are appropriate roles.
        operations.add( ArchivaRoleConstants.OPERATION_EDIT_REPOSITORY );
        operations.add( ArchivaRoleConstants.OPERATION_DELETE_REPOSITORY );

        operations.add( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );
        operations.add( ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );
        return operations;
    }

    public List getDynamicChildRoles( String string )
    {
        return Collections.singletonList(
            ArchivaRoleConstants.REPOSITORY_OBSERVER_ROLE_PREFIX + ArchivaRoleConstants.DELIMITER + string );
    }

    public boolean isAssignable()
    {
        return true;
    }
}

