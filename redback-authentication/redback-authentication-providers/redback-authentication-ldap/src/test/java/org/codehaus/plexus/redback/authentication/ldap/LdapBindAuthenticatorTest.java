package org.codehaus.plexus.redback.authentication.ldap;

/*
 * Copyright 2001-2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;
import org.codehaus.plexus.cache.builder.CacheBuilder;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.common.ldap.LdapUser;
import org.codehaus.plexus.redback.common.ldap.connection.LdapConnection;
import org.codehaus.plexus.redback.policy.PasswordEncoder;
import org.codehaus.plexus.redback.policy.encoders.SHA1PasswordEncoder;
import org.codehaus.plexus.redback.users.ldap.service.LdapCacheService;
import org.codehaus.redback.components.apacheds.ApacheDs;
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
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Calendar;
import java.util.Date;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "classpath*:/META-INF/spring-context.xml" )
public class LdapBindAuthenticatorTest
    extends TestCase
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "apacheDS#test" )
    private ApacheDs apacheDs;

    @Inject
    private LdapBindAuthenticator authnr;

    private String suffix;

    private PasswordEncoder passwordEncoder;

    @Inject
    private LdapCacheService ldapCacheService;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        passwordEncoder = new SHA1PasswordEncoder();

        suffix =
            apacheDs.addSimplePartition( "test", new String[]{ "redback", "plexus", "codehaus", "org" } ).getSuffix();

        log.info( "DN Suffix: " + suffix );

        apacheDs.startServer();

        makeUsers();

    }

    @After
    public void tearDown()
        throws Exception
    {
        // clear the cache
        ldapCacheService.removeAllLdapUserDn();

        InitialDirContext context = apacheDs.getAdminContext();

        context.unbind( createDn( "jesse" ) );

        context.unbind( createDn( "joakim" ) );

        context.unbind( createDn( "brent" ) );

        if ( !apacheDs.isStopped() )
        {
            apacheDs.stopServer();
        }

        super.tearDown();
    }

    @Test
    public void testAuthenticationEmptyPassword()
        throws Exception
    {
        PasswordBasedAuthenticationDataSource authDs = new PasswordBasedAuthenticationDataSource();

        // Would throw NPE if attempting to bind, this hack tests bind prevention
        authDs.setPrincipal( "brent" );
        authDs.setPassword( null );
        AuthenticationResult result = authnr.authenticate( authDs );
        assertFalse( result.isAuthenticated() );

        // This passes anyway on ApacheDS, but not true for AD or Novel eDir
        authDs.setPassword( "" );
        result = authnr.authenticate( authDs );
        assertFalse( result.isAuthenticated() );
    }

    @Test
    public void testAuthentication()
        throws Exception
    {
        PasswordBasedAuthenticationDataSource authDs = new PasswordBasedAuthenticationDataSource();
        authDs.setPrincipal( "jesse" );
        authDs.setPassword( passwordEncoder.encodePassword( "foo" ) );
        AuthenticationResult result = authnr.authenticate( authDs );
        assertTrue( result.isAuthenticated() );
    }

    // REDBACK-289/MRM-1488
    @Test
    public void testAuthenticationFromCache()
        throws Exception
    {
        PasswordBasedAuthenticationDataSource authDs = new PasswordBasedAuthenticationDataSource();
        authDs.setPrincipal( "jesse" );
        authDs.setPassword( passwordEncoder.encodePassword( "foo" ) );

        // user shouldn't be in the cache yet
        assertNull( ldapCacheService.getLdapUserDn( "jesse" ) );

        long startTime = Calendar.getInstance().getTimeInMillis();
        AuthenticationResult result = authnr.authenticate( authDs );
        long endTime = Calendar.getInstance().getTimeInMillis();

        assertTrue( result.isAuthenticated() );

        long firstAuth = endTime - startTime;

        // user should be in the cache now
        assertNotNull( ldapCacheService.getLdapUserDn( "jesse" ) );

        startTime = Calendar.getInstance().getTimeInMillis();
        result = authnr.authenticate( authDs );
        endTime = Calendar.getInstance().getTimeInMillis();

        long secondAuth = endTime - startTime;

        assertTrue( "Second authn should be quicker!", secondAuth < firstAuth );        
    }

    private void makeUsers()
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();

        String cn = "jesse";
        bindUserObject( context, cn, createDn( cn ) );

        cn = "joakim";
        bindUserObject( context, cn, createDn( cn ) );

        cn = "brent";
        bindUserObject( context, cn, createDn( cn ) );
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
        attributes.put( "mail", "foo" );
        attributes.put( "userPassword", passwordEncoder.encodePassword( "foo" ) );
        attributes.put( "givenName", "foo" );
        context.createSubcontext( dn, attributes );
    }

    private String createDn( String cn )
    {
        return "cn=" + cn + "," + suffix;
    }
}
