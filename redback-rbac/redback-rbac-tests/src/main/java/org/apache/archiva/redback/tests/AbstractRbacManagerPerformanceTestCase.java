package org.apache.archiva.redback.tests;

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
import junit.framework.TestCase;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.tests.utils.RBACDefaults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class AbstractRbacManagerPerformanceTestCase
    extends TestCase
{
    private RBACManager rbacManager;

    private RBACDefaults rbacDefaults;

    protected Logger logger = LoggerFactory.getLogger( getClass() );

    public void setRbacManager( RBACManager store )
    {
        this.rbacManager = store;
        rbacDefaults = new RBACDefaults( rbacManager );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    private Role getDeveloperRole()
        throws RbacManagerException
    {
        Role role = rbacManager.createRole( "DEVELOPER" );
        role.setAssignable( true );

        Permission perm = rbacManager.createPermission( "EDIT_MY_USER", "EDIT", "User:Self" );

        role.addPermission( perm );

        return role;
    }
    
    private Role getSuperDeveloperRole()
    {
        Role role = rbacManager.createRole( "SUPER_DEVELOPER" );
        role.setAssignable( true );

        return role;
    }

    private static final int ITERATIONS = 10000;

    private static final int ONESECOND = 1000;

    public void assertPerformance( String msg, long startTime, long endTime, int iterations, double threshold )
    {
        long elapsed = endTime - startTime;
        double ratio = (double) elapsed / (double) ONESECOND; // ratio of time to 1 second.
        double opsPerSecond = (double) iterations / ratio;

        logger.info( "Performance {}: {} operations per second. (effective)", msg, opsPerSecond );

        if ( opsPerSecond < threshold )
        {
            // Failure

            StringBuilder stats = new StringBuilder();

            stats.append( "Stats on " ).append( msg );
            stats.append( "\nStart Time (ms): " ).append( Long.toString( startTime ) );
            stats.append( "\nEnd Time (ms)  : " ).append( Long.toString( endTime ) );
            stats.append( "\nElapsed (ms)   : " ).append( Long.toString( elapsed ) );
            stats.append( "\nRatio          : " ).append( Double.toString( ratio ) );
            stats.append( "\nOps per second : " ).append( Double.toString( opsPerSecond ) );

            logger.info( stats.toString() );

            fail( "Performance Error: " + msg + " expecting greater than [" + threshold + "], actual [" + opsPerSecond
                + "]" );
        }
    }

    @Test
    public void testPerformanceResource()
        throws RbacManagerException
    {
        assertNotNull( rbacManager );
        rbacManager.eraseDatabase();

        Resource resource = rbacManager.createResource( "foo" );
        Resource resource2 = rbacManager.createResource( "bar" );

        assertNotNull( resource );

        Resource added = rbacManager.saveResource( resource );
        assertNotNull( added );
        Resource added2 = rbacManager.saveResource( resource2 );
        assertNotNull( added2 );

        assertEquals( 2, rbacManager.getAllResources().size() );

        String resFooId = resource.getIdentifier();
        String resBarId = resource2.getIdentifier();
        long startTime = System.currentTimeMillis();

        for ( int i = 0; i <= ITERATIONS; i++ )
        {
            Resource resFoo = rbacManager.getResource( resFooId );
            Resource resBar = rbacManager.getResource( resBarId );

            assertNotNull( resFoo );
            assertNotNull( resBar );

            assertEquals( "foo", resFoo.getIdentifier() );
            assertEquals( "bar", resBar.getIdentifier() );
        }

        long endTime = System.currentTimeMillis();

        assertPerformance( "Resource", startTime, endTime, ITERATIONS, 500.0 );
    }

    @Test
    public void testPerformanceUserAssignment()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;

        rbacManager.eraseDatabase();

        Role devRole = getDeveloperRole();
        Role devPlusRole = getSuperDeveloperRole();
        devPlusRole.setChildRoleNames( Collections.singletonList( devRole.getName() ) );
        devRole = manager.saveRole( devRole );
        devPlusRole = manager.saveRole( devPlusRole );

        // Setup User / Assignment with 1 role.
        String username = "bob";
        UserAssignment assignment = manager.createUserAssignment( username );
        assignment.addRoleName( devRole );
        assignment = manager.saveUserAssignment( assignment );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( "should be only one role assigned", 1, manager.getAssignedRoles( assignment.getPrincipal() )
            .size() );
        assertEquals( "should be one role left to assign", 1, manager.getUnassignedRoles( assignment.getPrincipal() )
            .size() );
        assertEquals( 2, manager.getAllRoles().size() );

        // assign the same role again to the same user
        assignment.addRoleName( devRole.getName() );
        manager.saveUserAssignment( assignment );

        // we certainly shouldn't have 2 roles here now
        assertEquals( 1, assignment.getRoleNames().size() );

        String bobId = assignment.getPrincipal();

        username = "janet";

        devPlusRole.setChildRoleNames( Collections.singletonList( devRole.getName() ) );
        devRole = manager.saveRole( devRole );
        manager.saveRole( devPlusRole );

        assignment = manager.createUserAssignment( username );
        assignment.addRoleName( devRole );
        assignment = manager.saveUserAssignment( assignment );

        assertEquals( 2, manager.getAllUserAssignments().size() );
        assertEquals( "should be only one role assigned", 1, manager.getAssignedRoles( assignment.getPrincipal() )
            .size() );
        assertEquals( "should be one role left to assign", 1, manager.getUnassignedRoles( assignment.getPrincipal() )
            .size() );
        assertEquals( 2, manager.getAllRoles().size() );

        // assign the same role again to the same user
        assignment.addRoleName( devRole.getName() );
        manager.saveUserAssignment( assignment );

        // we certainly shouldn't have 2 roles here now
        assertEquals( 1, assignment.getRoleNames().size() );

        String janetId = assignment.getPrincipal();

        long startTime = System.currentTimeMillis();

        for ( int i = 0; i <= ITERATIONS; i++ )
        {
            UserAssignment uaBob = rbacManager.getUserAssignment( bobId );
            UserAssignment uaJanet = rbacManager.getUserAssignment( janetId );

            assertNotNull( uaBob );
            assertNotNull( uaJanet );

            assertEquals( "bob", uaBob.getPrincipal() );
            assertEquals( "janet", uaJanet.getPrincipal() );
        }

        long endTime = System.currentTimeMillis();
        assertPerformance( "UserAssignment", startTime, endTime, ITERATIONS, 350.0 );
    }

    @Test
    public void testPerformanceRoles()
        throws RbacManagerException
    {
        rbacDefaults.createDefaults();

        String roleIdSysAdmin = "System Administrator";
        String roleIdUserAdmin = "User Administrator";

        long startTime = System.currentTimeMillis();

        for ( int i = 0; i <= ITERATIONS; i++ )
        {
            Role roleSysAdmin = rbacManager.getRole( roleIdSysAdmin );
            Role roleUserAdmin = rbacManager.getRole( roleIdUserAdmin );

            assertNotNull( roleSysAdmin );
            assertNotNull( roleUserAdmin );

            assertEquals( roleIdSysAdmin, roleSysAdmin.getName() );
            assertEquals( roleIdUserAdmin, roleUserAdmin.getName() );
        }

        long endTime = System.currentTimeMillis();

        assertPerformance( "Roles", startTime, endTime, ITERATIONS, 130 );
    }

    @Test
    public void testPerformancePermissions()
        throws RbacManagerException
    {
        rbacDefaults.createDefaults();

        String permIdRunIndexer = "Run Indexer";
        String permIdAddRepo = "Add Repository";

        long startTime = System.currentTimeMillis();

        for ( int i = 0; i <= ITERATIONS; i++ )
        {
            Permission permRunIndex = rbacManager.getPermission( permIdRunIndexer );
            Permission permAddRepo = rbacManager.getPermission( permIdAddRepo );

            assertNotNull( permRunIndex );
            assertNotNull( permAddRepo );

            assertEquals( permIdRunIndexer, permRunIndex.getName() );
            assertEquals( permIdAddRepo, permAddRepo.getName() );
        }

        long endTime = System.currentTimeMillis();

        assertPerformance( "Permissions", startTime, endTime, ITERATIONS, 350 );
    }

    @Test
    public void testPerformanceOperations()
        throws RbacManagerException
    {
        rbacDefaults.createDefaults();

        String operIdEditRepo = "edit-repository";
        String operIdDelRepo = "delete-repository";

        long startTime = System.currentTimeMillis();

        for ( int i = 0; i <= ITERATIONS; i++ )
        {
            Operation operEditRepo = rbacManager.getOperation( operIdEditRepo );
            Operation operDelRepo = rbacManager.getOperation( operIdDelRepo );

            assertNotNull( operEditRepo );
            assertNotNull( operDelRepo );

            assertEquals( operIdEditRepo, operEditRepo.getName() );
            assertEquals( operIdDelRepo, operDelRepo.getName() );
        }

        long endTime = System.currentTimeMillis();

        assertPerformance( "Operations", startTime, endTime, ITERATIONS, 500 );
    }
}