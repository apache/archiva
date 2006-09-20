package org.apache.maven.archiva.web.util;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.codehaus.plexus.security.rbac.RbacStoreException;

/**
 * RoleManager:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id:$
 */
public interface RoleManager
{
    public static final String ROLE = RoleManager.class.getName();
    
    public static final String ADMIN_USERNAME = "admin";

    public void addRepository( String repositoryName )
        throws RbacStoreException;

    public void addUser( String principal )
        throws RbacStoreException;

    public void addAdminUser( String principal )
        throws RbacStoreException;

    public boolean isInitialized();
}
