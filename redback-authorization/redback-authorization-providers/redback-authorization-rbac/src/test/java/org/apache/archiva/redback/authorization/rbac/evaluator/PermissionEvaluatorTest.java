package org.apache.archiva.redback.authorization.rbac.evaluator;

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
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.memory.MemoryOperation;
import org.apache.archiva.redback.rbac.memory.MemoryPermission;
import org.apache.archiva.redback.rbac.memory.MemoryResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class PermissionEvaluatorTest
    extends TestCase
{

    @Inject
    PermissionEvaluator permissionEvaluator;

    @Test
    public void testNullResource()
        throws PermissionEvaluationException
    {
        // null resources should be considered as matching if any resource is obtained.
        // we do this instead of using "global" as that is the inverse - you are allocated global rights,
        // which is right to everything. null is the right to anything.

        Resource resource = new MemoryResource();
        resource.setIdentifier( "Resource" );

        Operation operation = new MemoryOperation();
        operation.setName( "Operation" );

        Permission permission = new MemoryPermission();
        permission.setName( "Permission" );
        permission.setOperation( operation );
        permission.setResource( resource );

        assertTrue( permissionEvaluator.evaluate( permission, "Operation", null, "brett" ) );
    }
}
