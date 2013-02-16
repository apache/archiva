package org.apache.archiva.redback.rbac.jdo;

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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.jdo.io.stax.RbacJdoModelStaxReader;
import org.apache.archiva.redback.rbac.jdo.io.stax.RbacJdoModelStaxWriter;


/**
 * Test the StAX reader and writer generated.
 */
public class RbacJdoModelStaxTest
    extends TestCase
{
    @SuppressWarnings("unchecked")
    public void testStax()
        throws IOException, XMLStreamException
    {
        RbacDatabase database = new RbacDatabase();

        JdoRole role = new JdoRole();
        role.setAssignable( true );
        role.setDescription( "descriptor" );
        role.setName( "name" );
        role.setPermanent( true );
        role.addChildRoleName( "childRole1" );
        role.addChildRoleName( "childRole2" );

        JdoPermission permission = new JdoPermission();
        permission.setDescription( "permDesc" );
        permission.setName( "permName" );

        JdoOperation operation = new JdoOperation();
        operation.setDescription( "opDesc" );
        operation.setName( "opName" );
        operation.setPermanent( true );
        operation.setResourceRequired( true );
        permission.setOperation( operation );
        database.addOperation( operation );

        JdoResource resource = new JdoResource();
        resource.setIdentifier( "resId" );
        resource.setPattern( true );
        resource.setPermanent( true );
        permission.setResource( resource );
        database.addResource( resource );
        permission.setPermanent( true );
        role.addPermission( permission );
        database.addPermission( permission );

        database.addRole( role );

        JdoUserAssignment assignment = new JdoUserAssignment();
        assignment.setPermanent( false );
        assignment.setPrincipal( "principal" );
        assignment.setTimestamp( new Date() );
        assignment.addRoleName( "name" );

        database.addUserAssignment( assignment );

        StringWriter w = new StringWriter();
        new RbacJdoModelStaxWriter().write( w, database );

        RbacDatabase newDatabase = new RbacJdoModelStaxReader().read( new StringReader( w.toString() ) );

        List<JdoRole> expectedRoles = database.getRoles();
        List<JdoRole> roles = newDatabase.getRoles();
        assertEquals( expectedRoles.size(), roles.size() );
        for ( JdoRole r : roles )
        {
            boolean found = false;
            for ( JdoRole expectedRole : expectedRoles )
            {
                if ( expectedRole.getName().equals( r.getName() ) )
                {
                    found = true;

                    assertRole( expectedRole, r );
                }
            }
            if ( !found )
            {
                fail( "Couldn't find role: " + r.getName() );
            }
        }

        List<JdoUserAssignment> expectedUserAssignments = database.getUserAssignments();
        List<JdoUserAssignment> userAssignments = newDatabase.getUserAssignments();
        assertEquals( expectedUserAssignments.size(), userAssignments.size() );
        for ( JdoUserAssignment a : userAssignments )
        {
            boolean found = false;
            for ( JdoUserAssignment expectedAssignment : expectedUserAssignments )
            {
                if ( expectedAssignment.getPrincipal().equals( a.getPrincipal() ) )
                {
                    found = true;

                    assertUserAssignment( expectedAssignment, a );
                }
            }
            if ( !found )
            {
                fail( "Couldn't find assignment: " + a.getPrincipal() );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void assertRole( JdoRole expectedRole, JdoRole role )
    {
        assertEquals( expectedRole.getDescription(), role.getDescription() );
        assertPermissions( expectedRole.getPermissions(), role.getPermissions() );
        assertEquals( expectedRole.getChildRoleNames(), role.getChildRoleNames() );
    }

    private void assertUserAssignment( JdoUserAssignment expectedAssignment, JdoUserAssignment assignment )
    {
        SimpleDateFormat sdf = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z", Locale.US );
        assertNotNull( expectedAssignment.getTimestamp() );
        assertNotNull( assignment.getTimestamp() );

        assertEquals( sdf.format( expectedAssignment.getTimestamp() ), sdf.format( assignment.getTimestamp() ) );
        assertEquals( expectedAssignment.getRoleNames(), assignment.getRoleNames() );
    }

    private void assertPermissions( List<Permission> expectedPermissions, List<Permission> permissions )
    {
        assertEquals( expectedPermissions.size(), permissions.size() );
        for ( Permission permission : permissions )
        {
            boolean found = false;
            for ( Permission expectedPermission : expectedPermissions )
            {
                if ( expectedPermission.getName().equals( permission.getName() ) )
                {
                    found = true;

                    assertPermission( expectedPermission, permission );
                }
            }
            if ( !found )
            {
                fail( "Couldn't find permission: " + permission.getName() );
            }
        }
    }

    private void assertPermission( Permission expectedPermission, Permission permission )
    {
        assertEquals( expectedPermission.getDescription(), permission.getDescription() );
        assertOperation( expectedPermission.getOperation(), permission.getOperation() );
        assertResource( expectedPermission.getResource(), permission.getResource() );
    }

    private void assertResource( Resource expectedResource, Resource resource )
    {
        assertEquals( expectedResource.getIdentifier(), resource.getIdentifier() );
    }

    private void assertOperation( Operation expectedOperation, Operation operation )
    {
        assertEquals( expectedOperation.getName(), operation.getName() );
        assertEquals( expectedOperation.getDescription(), operation.getDescription() );
    }
}
