package org.apache.archiva.redback.role.processor;

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
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.model.io.stax.RedbackRoleModelStaxReader;
import org.apache.archiva.redback.role.validator.RoleModelValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * RoleProfileTest:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RoleModelProcessorTest
    extends TestCase
{
    @Inject
    @Named( value = "rbacManager#memory" )
    private RBACManager rbacManager;

    @Inject
    private RoleModelValidator modelValidator;

    @Inject @Named(value = "modelProcessor#memory")
    private RoleModelProcessor roleProcessor;

    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    String getBasedir()
    {
        return System.getProperty( "basedir" );
    }

    @Test
    public void testProcessing()
        throws Exception
    {
        RedbackRoleModel redback = getModelFromFile( "/src/test/processor-tests/redback-1.xml" );

        processModel( redback );

        assertTrue( rbacManager.resourceExists( "cornflakes" ) );
    }

    private RedbackRoleModel getModelFromFile( String file )
        throws IOException, XMLStreamException
    {
        File resource = new File( getBasedir() + file );

        assertNotNull( resource );

        RedbackRoleModelStaxReader modelReader = new RedbackRoleModelStaxReader();

        RedbackRoleModel redback = modelReader.read( resource.getAbsolutePath() );

        assertNotNull( redback );
        return redback;
    }

    private void processModel( RedbackRoleModel redback )
        throws RoleManagerException
    {
        assertTrue( modelValidator.validate( redback ) );

        roleProcessor.process( redback );
    }

    @Test
    public void testMultipleProcessing()
        throws Exception
    {
        rbacManager.eraseDatabase();

        RedbackRoleModel redback = getModelFromFile( "/src/test/processor-tests/redback-2.xml" );

        processModel( redback );
        roleProcessor.process( redback );

        Role systemAdmin = rbacManager.getRole( "System Administrator" );

        assertTrue( systemAdmin.hasChildRoles() );
    }

    /**
     * @todo there are other things that are not synced - role descriptions, removal of operations, etc.
     */
    @Test
    public void testSyncPermissionsOnUpgrade()
        throws Exception
    {
        rbacManager.eraseDatabase();

        processModel( getModelFromFile( "/src/test/processor-tests/redback-1.xml" ) );

        Role role = rbacManager.getRole( "Baby" );
        assertFalse( hasPermissionOnOperation( role, "Eat Cornflakes" ) );
        assertTrue( hasPermissionOnOperation( role, "Drink Milk" ) );

        processModel( getModelFromFile( "/src/test/processor-tests/redback-1-updated.xml" ) );

        role = rbacManager.getRole( "Baby" );
        assertTrue( hasPermissionOnOperation( role, "Eat Cornflakes" ) );
        assertFalse( hasPermissionOnOperation( role, "Drink Milk" ) );
    }

    private boolean hasPermissionOnOperation( Role role, String operation )
    {
        for ( Permission p : role.getPermissions() )
        {
            if ( p.getOperation().getName().equals( operation ) )
            {
                return true;
            }
        }
        return false;
    }
}