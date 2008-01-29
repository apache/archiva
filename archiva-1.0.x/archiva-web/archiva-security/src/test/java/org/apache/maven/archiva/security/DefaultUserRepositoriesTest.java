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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;

/**
 * DefaultUserRepositoriesTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultUserRepositoriesTest
    extends PlexusTestCase
{
    private static final String USER_GUEST = "guest";

    private static final String USER_ADMIN = "admin";

    private static final String USER_ALPACA = "alpaca";

    private SecuritySystem securitySystem;

    private RBACManager rbacManager;

    private RoleManager roleManager;

    private ArchivaConfiguration archivaConfiguration;

    private UserRepositories userRepos;

    public void testGetObservableRepositoryIds()
        throws Exception
    {
        // create some users.
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );

        assertEquals( "Expected users", 3, securitySystem.getUserManager().getUsers().size() );

        // some unassigned repo observer roles.
        setupRepository( "central" );
        setupRepository( "corporate" );
        setupRepository( "internal" );
        setupRepository( "snapshots" );
        setupRepository( "secret" );

        // some assigned repo observer roles.
        assignRepositoryObserverRole( USER_ALPACA, "corporate" );
        assignRepositoryObserverRole( USER_ALPACA, "central" );
        assignRepositoryObserverRole( USER_GUEST, "corporate" );
        // the global repo observer role.
        assignGlobalRepositoryObserverRole( USER_ADMIN );

        assertRepoIds( new String[] { "central", "corporate" }, userRepos.getObservableRepositoryIds( USER_ALPACA ) );
        assertRepoIds( new String[] { "coporate" }, userRepos.getObservableRepositoryIds( USER_GUEST ) );
        assertRepoIds( new String[] { "central", "internal", "corporate", "snapshots", "secret" }, userRepos
            .getObservableRepositoryIds( USER_ADMIN ) );
    }

    private void assertRepoIds( String[] expectedRepoIds, List<String> observableRepositoryIds )
    {
        assertNotNull( "Observable Repository Ids cannot be null.", observableRepositoryIds );

        if ( expectedRepoIds.length != observableRepositoryIds.size() )
        {
            fail( "Size of Observable Repository Ids wrong, expected <" + expectedRepoIds.length + "> but got <"
                + observableRepositoryIds.size() + "> instead. \nExpected: [" + StringUtils.join( expectedRepoIds, "," )
                + "]\nActual: [" + StringUtils.join( observableRepositoryIds.iterator(), "," ) + "]" );
        }
    }

    private void setupRepository( String repoId )
        throws Exception
    {
        // Add repo to configuration.
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( repoId );
        repoConfig.setName( "Testable repo <" + repoId + ">" );
        repoConfig.setLocation( getTestPath( "target/test-repo/" + repoId ) );
        archivaConfiguration.getConfiguration().addManagedRepository( repoConfig );

        // Add repo roles to security.
        userRepos.createMissingRepositoryRoles( repoId );
    }

    private void assignGlobalRepositoryObserverRole( String principal )
        throws Exception
    {
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_GLOBAL_REPOSITORY_OBSERVER, principal );
    }

    private void assignRepositoryObserverRole( String principal, String repoId )
        throws Exception
    {
        roleManager.assignTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId, principal );
    }

    private User createUser( String principal, String fullname )
    {
        UserManager userManager = securitySystem.getUserManager();

        User user = userManager.createUser( principal, fullname, principal + "@testable.archiva.apache.org" );
        securitySystem.getPolicy().setEnabled( false );
        userManager.addUser( user );
        securitySystem.getPolicy().setEnabled( true );

        return user;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        File srcConfig = getTestFile( "src/test/resources/repository-archiva.xml" );
        File destConfig = getTestFile( "target/test-conf/archiva.xml" );

        destConfig.getParentFile().mkdirs();
        destConfig.delete();

        FileUtils.copyFile( srcConfig, destConfig );

        securitySystem = (SecuritySystem) lookup( SecuritySystem.class, "testable" );
        rbacManager = (RBACManager) lookup( RBACManager.class, "memory" );
        roleManager = (RoleManager) lookup( RoleManager.class, "default" );
        userRepos = (UserRepositories) lookup( UserRepositories.class, "default" );
        archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );

        // Some basic asserts.
        assertNotNull( securitySystem );
        assertNotNull( rbacManager );
        assertNotNull( roleManager );
        assertNotNull( userRepos );
        assertNotNull( archivaConfiguration );

        // Setup Admin User.
        User adminUser = createUser( USER_ADMIN, "Admin User" );
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_SYSTEM_ADMIN, adminUser.getPrincipal().toString() );

        // Setup Guest User.
        User guestUser = createUser( USER_GUEST, "Guest User" );
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_GUEST, guestUser.getPrincipal().toString() );

    }
}
