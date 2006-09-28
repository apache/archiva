package org.apache.maven.archiva.web.util;

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

import org.apache.maven.archiva.web.ArchivaSecurityDefaults;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.security.rbac.Permission;
import org.codehaus.plexus.security.rbac.RBACManager;
import org.codehaus.plexus.security.rbac.RbacManagerException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.rbac.Role;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;

/**
 * DefaultRoleManager:
 * @todo remove!
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id:$
 * @plexus.component role="org.apache.maven.archiva.web.util.RoleManager"
 * role-hint="default"
 */
public class DefaultRoleManager
    extends AbstractLogEnabled
    implements RoleManager
{

    /**
     * @plexus.requirement
     */
    private RBACManager manager;

    public void addRepository( String repositoryName )
        throws RbacManagerException
    {
        // make the resource
        Resource repoResource = manager.createResource( repositoryName );
        repoResource = manager.saveResource( repoResource );

        // make the permissions
        Permission editRepo =
            manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_EDIT + " - " + repositoryName );
        editRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_EDIT_OPERATION ) );
        editRepo.setResource( repoResource );
        editRepo = manager.savePermission( editRepo );

        Permission deleteRepo =
            manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_DELETE + " - " + repositoryName );
        deleteRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_DELETE_OPERATION ) );
        deleteRepo.setResource( repoResource );
        deleteRepo = manager.savePermission( deleteRepo );

        Permission accessRepo =
            manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_ACCESS + " - " + repositoryName );
        accessRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_ACCESS_OPERATION ) );
        accessRepo.setResource( repoResource );
        accessRepo = manager.savePermission( accessRepo );

        Permission uploadRepo =
            manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_UPLOAD + " - " + repositoryName );
        uploadRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_UPLOAD_OPERATION ) );
        uploadRepo.setResource( repoResource );
        uploadRepo = manager.savePermission( uploadRepo );

        // make the roles
        Role repositoryObserver = manager.createRole( "Repository Observer - " + repositoryName );
        repositoryObserver.addPermission( manager.getPermission( ArchivaSecurityDefaults.REPORTS_ACCESS_PERMISSION ) );
        repositoryObserver.setAssignable( true );
        repositoryObserver = manager.saveRole( repositoryObserver );

        Role repositoryManager = manager.createRole( "Repository Manager - " + repositoryName );
        repositoryManager.addPermission( editRepo );
        repositoryManager.addPermission( deleteRepo );
        repositoryManager.addPermission( accessRepo );
        repositoryManager.addPermission( uploadRepo );
        repositoryManager.addPermission( manager.getPermission( ArchivaSecurityDefaults.REPORTS_GENERATE_PERMISSION ) );
        repositoryManager.addChildRoleName( repositoryObserver.getName() );
        repositoryManager.setAssignable( true );
        manager.saveRole( repositoryManager );
    }

}
