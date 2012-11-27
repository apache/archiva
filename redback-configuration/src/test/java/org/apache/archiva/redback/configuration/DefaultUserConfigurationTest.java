package org.apache.archiva.redback.configuration;

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
import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * DefaultUserConfigurationTest
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class DefaultUserConfigurationTest
    extends TestCase
{

    @Inject  @Named(value = "test")
    DefaultUserConfiguration config;

    private void assertEmpty( String str )
    {
        if ( StringUtils.isNotEmpty( str ) )
        {
            fail( "Expected String to be empty." );
        }
    }

    @Test
    public void testLoad()
        throws Exception
    {
        assertNotNull( config );
        // check that the configuration loaded correctly - if this fails, maybe you aren't in the right basedir
        assertNotNull( config.getString( "test.value" ) );
    }

    @Test
    public void testGetString()
        throws Exception
    {
        // Test default configuration entry
        assertEquals( "25", config.getString( "email.smtp.port" ) );
        // Test overlaid configuration entry
        assertEquals( "127.0.2.2", config.getString( "email.smtp.host" ) );
        // Test default value
        assertEquals( "127.0.0.1", config.getString( "email.smtp.host.bad", "127.0.0.1" ) );
/* Requires commons-configuration 1.4
        // Test expressions
        assertEquals( "jdbc:derby:" + System.getProperty( "plexus.home" ) + "/database;create=true",
                      config.getString( "jdbc.url" ) );
        assertEquals( "foo/bar", config.getString( "test.expression" ) );
*/

        assertEmpty( config.getString( "email.smtp.foo.foo" ) );
    }

    @Test
    public void testGetBoolean()
        throws Exception
    {
        assertFalse( config.getBoolean( "email.smtp.ssl.enabled" ) );
        assertFalse( config.getBoolean( "email.smtp.tls.enabled" ) );
    }

    @Test
    public void testGetInt()
        throws Exception
    {
        assertEquals( 25, config.getInt( "email.smtp.port" ) );
        assertEquals( 8080, config.getInt( "email.smtp.port.bad", 8080 ) );
    }

    @Test
    public void testConcatenatedList()
    {
        assertEquals( "uid=brett,dc=codehaus,dc=org", config.getConcatenatedList( "ldap.bind.dn", null ) );
        assertEquals( "dc=codehaus,dc=org", config.getConcatenatedList( "ldap.base.dn", null ) );
        assertEquals( "foo", config.getConcatenatedList( "short.list", null ) );
        assertEquals( "bar,baz", config.getConcatenatedList( "no.list", "bar,baz" ) );
    }
}
