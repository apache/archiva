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

import java.util.List;
import java.util.ArrayList;

/**
 * @plexus.component role="org.codehaus.plexus.rbac.profile.DynamicRoleProfile"
 * role-hint="archiva-repository-observer"
 */
public class RepositoryObserverDynamicRoleProfile
    extends AbstractDynamicRoleProfile
{
    /**
     * Create the Role name for a Repository Observer, using the provided repository id.
     * 
     * @param repoId the repository id
     */
    public String getRoleName( String repoId )
    {
        return ArchivaRoleConstants.REPOSITORY_OBSERVER_ROLE_PREFIX + ArchivaRoleConstants.DELIMITER + repoId;
    }

    public List getOperations()
    {
        List operations = new ArrayList();
        operations.add( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );
        return operations;
    }

    public boolean isAssignable()
    {
        return true;
    }
}
