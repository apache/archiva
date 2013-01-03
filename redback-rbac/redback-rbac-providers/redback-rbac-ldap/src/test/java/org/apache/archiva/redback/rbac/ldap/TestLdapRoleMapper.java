package org.apache.archiva.redback.rbac.ldap;
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
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public class TestLdapRoleMapper
    extends TestCase
{

    Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "ldapRoleMapper#test" )
    LdapRoleMapper ldapRoleMapper;


    @Test
    public void getAllGroups()
        throws Exception
    {
        List<String> allGroups = ldapRoleMapper.getAllGroups();

        log.info( "allGroups: {}", allGroups );

        Assertions.assertThat( allGroups ).isNotNull().isNotEmpty().contains( "archiva-admin",
                                                                              "internal-repo-manager" );
    }

    @Test
    public void getGroupsMember()
        throws Exception
    {
        List<String> users = ldapRoleMapper.getGroupsMember( "archiva-admin" );

        log.info( "users for archiva-admin: {}", users );

        Assertions.assertThat( users ).isNotNull().isNotEmpty().contains( "admin", "user.7" );
    }

    @Test
    public void getGroups()
        throws Exception
    {
        List<String> roles = ldapRoleMapper.getGroups( "admin" );

        log.info( "roles for admin: {}", roles );

        Assertions.assertThat( roles ).isNotNull().isNotEmpty().contains( "archiva-admin", "internal-repo-manager" );
    }
}
