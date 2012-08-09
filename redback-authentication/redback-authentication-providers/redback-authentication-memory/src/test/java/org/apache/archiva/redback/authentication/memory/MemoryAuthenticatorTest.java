package org.apache.archiva.redback.authentication.memory;

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
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * MemoryAuthenticatorTest:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class MemoryAuthenticatorTest
    extends TestCase
{
    @Inject
    @Named(value = "authenticator#memory")
    Authenticator authenticator;

    @Test
    public void testSimpleAuthentication()
        throws Exception
    {
        assertNotNull( authenticator );

        PasswordBasedAuthenticationDataSource ds = new PasswordBasedAuthenticationDataSource();
        ds.setPrincipal( "jason" );
        ds.setPassword( "kungfu" );

        AuthenticationResult ar = authenticator.authenticate( ds );

        assertTrue( ar.isAuthenticated() );

        assertEquals( "jason", ar.getPrincipal() );
    }
}
