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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.redback.role.RoleManager;

/**
 * RoleProfilesTest 
 *
 * @version $Id: RoleManagerTest.java 4330 2007-05-10 17:28:56Z jmcconnell $
 */
public class RoleManagerTest
    extends PlexusInSpringTestCase
{
    /**
     * @plexus.requirement role-hint="default"
     */
    RoleManager roleManager;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        roleManager = (RoleManager) lookup( RoleManager.ROLE, "default" );    
    }
    
    public void testExpectedRoles()
        throws Exception
    {
        assertNotNull( roleManager );
        
        assertTrue( roleManager.roleExists( "system-administrator" ) );
        assertTrue( roleManager.roleExists( "user-administrator" ) );
        assertTrue( roleManager.roleExists( "archiva-global-repository-observer" ) );
        assertTrue( roleManager.roleExists( "archiva-guest" ) );        
        assertTrue( roleManager.roleExists( "guest" ) );
    }
}
