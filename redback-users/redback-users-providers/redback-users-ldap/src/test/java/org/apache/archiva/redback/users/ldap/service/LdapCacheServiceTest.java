package org.apache.archiva.redback.users.ldap.service;

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
import org.apache.archiva.redback.common.ldap.user.LdapUser;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * @author: Maria Odea Ching <oching@apache.org>
 * @version
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class LdapCacheServiceTest
    extends TestCase
{
    @Inject
    private LdapCacheService ldapCacheService;

    private static final String USERNAME = "dummy";

    @After
    public void tearDown()
        throws Exception
    {
        ldapCacheService.removeAllUsers();
        ldapCacheService.removeAllLdapUserDn();

        super.tearDown();
    }

    @Test
    public void testLdapUserDnCache()
        throws Exception
    {
        ldapCacheService.addLdapUserDn( USERNAME, "userDn" );

        assertNotNull( ldapCacheService.getLdapUserDn( USERNAME ) );

        ldapCacheService.removeLdapUserDn( USERNAME );

        assertNull( ldapCacheService.getLdapUserDn( USERNAME ) );
    }

    @Test
    public void testClearLdapUserDnCache()
        throws Exception
    {
        ldapCacheService.addLdapUserDn( USERNAME, "userDn" );

        assertNotNull( ldapCacheService.getLdapUserDn( USERNAME ) );

        ldapCacheService.removeLdapUserDn( USERNAME );

        assertNull( ldapCacheService.getLdapUserDn( USERNAME ) );
    }

    @Test
    public void testLdapUsersCache()
        throws Exception
    {
        LdapUser ldapUser = new LdapUser( USERNAME );

        ldapCacheService.addUser( ldapUser );

        assertNotNull( ldapCacheService.getUser( USERNAME ) );

        ldapCacheService.removeUser( USERNAME );

        assertNull( ldapCacheService.getUser( USERNAME ) );
    }

    @Test
    public void testClearLdapUsersCache()
        throws Exception
    {
        LdapUser ldapUser = new LdapUser( USERNAME );

        ldapCacheService.addUser( ldapUser );

        assertNotNull( ldapCacheService.getUser( USERNAME ) );

        ldapCacheService.removeAllUsers();

        assertNull( ldapCacheService.getUser( USERNAME ) );
    }
}
