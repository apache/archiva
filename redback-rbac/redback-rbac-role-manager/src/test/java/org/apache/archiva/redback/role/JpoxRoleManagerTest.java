package org.apache.archiva.redback.role;

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

import org.apache.archiva.redback.rbac.jdo.JdoRbacManager;
import org.apache.archiva.redback.components.jdo.DefaultConfigurableJdoFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * RoleManagerTest:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/jpox-spring-context.xml" } )
public class JpoxRoleManagerTest
    extends AbstractRoleManagerTest
{
    @Inject
    @Named( value = "jdoFactory#users" )
    DefaultConfigurableJdoFactory jdoFactory;

    @Inject
    @Named( value = "rbacManager#jdo" )
    JdoRbacManager rbacManagerJdo;

    @Inject @Named(value = "roleManager#jpox")
    DefaultRoleManager roleManagerInjected;

    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {

        super.setUp();

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();

        setRbacManager( rbacManagerJdo );

        setRoleManager( roleManagerInjected );
    }

}