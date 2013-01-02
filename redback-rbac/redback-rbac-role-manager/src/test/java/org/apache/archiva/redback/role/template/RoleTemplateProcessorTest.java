package org.apache.archiva.redback.role.template;

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
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.model.io.stax.RedbackRoleModelStaxReader;
import org.apache.archiva.redback.role.processor.RoleModelProcessor;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

/**
 * RoleProfileTest:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RoleTemplateProcessorTest
    extends TestCase
{

    @Inject
    @Named( value = "rbacManager#memory" )
    private RBACManager rbacManager;

    @Inject @Named(value = "modelProcessor#memory")
    private RoleModelProcessor roleProcessor;

    @Inject @Named(value = "templateProcessor#memory")
    private RoleTemplateProcessor templateProcessor;

    /**
     * Creates a new RbacStore which contains no data.
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    String getBasedir()
    {
        return System.getProperty( "basedir" );
    }

    @Test
    public void testLoading()
        throws Exception
    {
        File resource = new File( getBasedir() + "/src/test/template-tests/redback-1.xml" );

        assertNotNull( resource );

        RedbackRoleModelStaxReader modelReader = new RedbackRoleModelStaxReader();

        RedbackRoleModel redback = modelReader.read( resource.getAbsolutePath() );

        assertNotNull( redback );

        roleProcessor.process( redback );

        templateProcessor.create( redback, "test-template", "foo" );

        ModelTemplate template = RoleModelUtils.getModelTemplate( redback, "test-template" );

        String templateName = template.getNamePrefix() + template.getDelimiter() + "foo";

        assertTrue( rbacManager.resourceExists( "cornflakes name" ) );

        assertTrue( rbacManager.roleExists( templateName ) );

        Role testRole = rbacManager.getRole( templateName );

        assertNotNull( testRole );

        Permission testPermission = (Permission) testRole.getPermissions().get( 0 );

        assertNotNull( testPermission );

        assertEquals( "Eat Cornflakes - cornflakes name", testPermission.getName() );

        templateProcessor.remove( redback, "test-template", "foo" );

        assertFalse( rbacManager.roleExists( templateName ) );

        templateProcessor.create( redback, "test-template-2", "foo" );

        ModelTemplate template2 = RoleModelUtils.getModelTemplate( redback, "test-template-2" );

        String templateName2 = template2.getNamePrefix() + template2.getDelimiter() + "foo";

        assertTrue( rbacManager.roleExists( templateName2 ) );

        Role role = rbacManager.getRole( templateName2 );

        assertNotNull( role );

        assertEquals( 3, template2.getPermissions().size() );
        assertEquals( 3, role.getPermissions().size() );

        assertEquals( 1, role.getChildRoleNames().size() );

    }

}