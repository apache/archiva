package org.apache.archiva.redback.users.ldap;

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
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.policy.encoders.SHA1PasswordEncoder;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.ldap.service.LdapCacheService;
import org.apache.archiva.redback.components.apacheds.ApacheDs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.List;


/**
 * LdapUserManagerTest 
 *
 * @author <a href="mailto:jesse@codehaus.org">Jesse McConnell</a>
 *
 */  

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class LdapUserManagerTest
    extends TestCase
{
    
    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "userManager#ldap")
    private UserManager userManager;

    @Inject
    @Named( value = "apacheDS#test" )
    private ApacheDs apacheDs;

    private String suffix;

    private PasswordEncoder passwordEncoder;

    @Inject
    @Named(value = "ldapConnectionFactory#configurable")
    private LdapConnectionFactory connectionFactory;

    @Inject
    private LdapCacheService ldapCacheService;

    public void testFoo()
        throws Exception
    {

    }

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        passwordEncoder = new SHA1PasswordEncoder();

        suffix = apacheDs.addSimplePartition( "test", new String[] { "redback", "plexus", "codehaus", "org" } )
            .getSuffix();

        log.info( "DN Suffix: " + suffix );

        apacheDs.startServer();
        
        clearManyUsers();

        makeUsers();

    }

    @After
    public void tearDown()
        throws Exception
    {
        // clear cache
        ldapCacheService.removeAllUsers();

        InitialDirContext context = apacheDs.getAdminContext();

        context.unbind( createDn( "jesse" ) );

        context.unbind( createDn( "joakim" ) );

        apacheDs.stopServer();

        super.tearDown();
    }

    private void makeUsers()
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();

        String cn = "jesse";
        bindUserObject( context, cn, createDn( cn ) );
        assertExist( context, createDn( cn ), "cn", cn );

        cn = "joakim";
        bindUserObject( context, cn, createDn( cn ) );
        assertExist( context, createDn( cn ), "cn", cn );

    }

    @Test
    public void testConnection()
        throws Exception
    {
        assertNotNull( connectionFactory );

        LdapConnection connection = null; 
        try
        {
        connection = connectionFactory.getConnection();

        assertNotNull( connection );

        DirContext context = connection.getDirContext();

        assertNotNull( context );
        } finally {
            connection.close();
        }
    }

    @Test
    public void testDirectUsersExistence()
        throws Exception
    {
        LdapConnection connection = null; 
        try
        {
        connection = connectionFactory.getConnection();

        DirContext context = connection.getDirContext();

        assertExist( context, createDn( "jesse" ), "cn", "jesse" );
        assertExist( context, createDn( "joakim" ), "cn", "joakim" );
        } finally {
            connection.close();
        }
        
    }

    @Test
    public void testUserManager()
        throws Exception
    {
        assertNotNull( userManager );

        //assertNull( ldapCacheService.getUser( "jesse" ) );

        assertTrue( userManager.userExists( "jesse" ) );

        //assertNotNull( ldapCacheService.getUser( "jesse" ) );

        List<User> users = userManager.getUsers();

        assertNotNull( users );

        assertEquals( 2, users.size() );

        User jesse = userManager.findUser( "jesse" );

        assertNotNull( jesse );

        assertEquals( "jesse", jesse.getUsername() );
        assertEquals( "jesse@apache.org", jesse.getEmail() );
        assertEquals( "foo", jesse.getFullName() );
        log.info( "=====>{}",jesse.getEncodedPassword());
        log.info( "=====>{}",passwordEncoder.encodePassword( "foo" ));
        assertTrue( passwordEncoder.isPasswordValid( jesse.getEncodedPassword(), "foo" ) );

    }

    @Test
    public void testUserNotFoundException()
        throws Exception
    {
        try
        {
            userManager.findUser( "foo bar" );
            fail( "not a UserNotFoundException with an unknown user" );
        }
        catch ( UserNotFoundException e )
        {
            // cool it works !
        }
    }

    @Test
    public void testWithManyUsers()
        throws Exception
    {
        makeManyUsers();
        
        assertNotNull( userManager );

        assertTrue( userManager.userExists( "user10" ) );

        List<User> users = userManager.getUsers();

        assertNotNull( users );

        assertEquals( 10002, users.size() );

        User user10 = userManager.findUser( "user10" );

        assertNotNull( user10 );
    }
    
    private void makeManyUsers()
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();
        
        for ( int i = 0 ; i < 10000 ; i++ )
        {    
            String cn = "user"+i;
            bindUserObject( context, cn, createDn( cn ) );
        }
    
    }
    
    private void clearManyUsers()
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();
        
        for ( int i = 0 ; i < 10000 ; i++ )
        {    
            String cn = "user"+i;
            try
            {
                context.unbind( createDn( cn ) );
            }
            catch ( NamingException e )
            {
                // OK lets try with next one
            }
        }
    
    }
    
    private void bindUserObject( DirContext context, String cn, String dn )
        throws Exception
    {
        Attributes attributes = new BasicAttributes( true );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "inetOrgPerson" );
        objectClass.add( "person" );
        objectClass.add( "organizationalperson" );
        attributes.put( objectClass );
        attributes.put( "cn", cn );
        attributes.put( "sn", "foo" );
        attributes.put( "mail", cn+"@apache.org" );
        attributes.put( "userPassword", passwordEncoder.encodePassword( "foo" ) );
        attributes.put( "givenName", "foo" );
        context.createSubcontext( dn, attributes );
    }

    private String createDn( String cn )
    {
        return "cn=" + cn + "," + suffix;
    }

    private void assertExist( DirContext context, String dn, String attribute, String value )
        throws NamingException
    {
        SearchControls ctls = new SearchControls();

        ctls.setDerefLinkFlag( true );
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        ctls.setReturningAttributes( new String[] { "*" } );

        BasicAttributes matchingAttributes = new BasicAttributes();
        matchingAttributes.put( attribute, value );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "inetOrgPerson" );
        matchingAttributes.put( objectClass );

        NamingEnumeration<SearchResult> results = context.search( suffix, matchingAttributes );
        // NamingEnumeration<SearchResult> results = context.search( suffix, "(" + attribute + "=" + value + ")", ctls
        // );

        assertTrue( results.hasMoreElements() );
        SearchResult result = results.nextElement();
        Attributes attrs = result.getAttributes();
        Attribute testAttr = attrs.get( attribute );
        assertEquals( value, testAttr.get() );

    }

}
