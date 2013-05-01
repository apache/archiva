package org.apache.archiva.redback.common.ldap.role;

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
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.components.apacheds.ApacheDs;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.encoders.SHA1PasswordEncoder;
import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-role-mapper.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestLdapRoleMapper
    extends TestCase
{

    Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "apacheDS#test")
    private ApacheDs apacheDs;

    private String suffix;

    private String groupSuffix;

    private PasswordEncoder passwordEncoder;

    //@Inject
    //private LdapCacheService ldapCacheService;

    @Inject
    @Named(value = "ldapRoleMapper#test")
    LdapRoleMapper ldapRoleMapper;

    @Inject
    LdapConnectionFactory ldapConnectionFactory;

    List<String> roleNames =
        Arrays.asList( "Archiva System Administrator", "Internal Repo Manager", "Internal Repo Observer" );

    LdapConnection ldapConnection;

    DirContext context;

    private Map<String, List<String>> usersPerGroup;

    private List<String> users;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        usersPerGroup = new HashMap<String, List<String>>( 3 );

        usersPerGroup.put( "internal-repo-manager", Arrays.asList( "admin", "user.9" ) );
        usersPerGroup.put( "internal-repo-observer", Arrays.asList( "admin", "user.7", "user.8" ) );
        usersPerGroup.put( "archiva-admin", Arrays.asList( "admin", "user.7" ) );

        users = new ArrayList<String>( 4 );
        users.add( "admin" );
        users.add( "user.7" );
        users.add( "user.8" );
        users.add( "user.9" );

        passwordEncoder = new SHA1PasswordEncoder();

        groupSuffix = apacheDs.addSimplePartition( "test", new String[]{ "archiva", "apache", "org" } ).getSuffix();

        log.info( "groupSuffix: {}", groupSuffix );

        suffix = "ou=People,dc=archiva,dc=apache,dc=org";

        log.info( "DN Suffix: {}", suffix );

        apacheDs.startServer();

        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        Attributes attributes = new BasicAttributes( true );
        attributes.put( objectClass );
        attributes.put( "organizationalUnitName", "foo" );

        apacheDs.getAdminContext().createSubcontext( suffix, attributes );

        makeUsers();

        createGroups();


    }

    @After
    public void tearDown()
        throws Exception
    {
        // clear cache
        //ldapCacheService.removeAllUsers();

        InitialDirContext context = apacheDs.getAdminContext();

        for ( String uid : users )
        {
            context.unbind( createDn( uid ) );
        }

        for ( Map.Entry<String, List<String>> group : usersPerGroup.entrySet() )
        {
            context.unbind( createGroupDn( group.getKey() ) );
        }

        context.unbind( suffix );

        context.close();

        ldapConnection.close();

        apacheDs.stopServer();

        super.tearDown();
    }

    protected DirContext getDirContext()
        throws Exception
    {
        ldapConnection = ldapConnectionFactory.getConnection();
        context = ldapConnection.getDirContext();
        return context;
    }

    private void createGroups()
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();

        for ( Map.Entry<String, List<String>> group : usersPerGroup.entrySet() )
        {
            createGroup( context, group.getKey(), createGroupDn( group.getKey() ), group.getValue() );
        }

    }

    private void createGroup( DirContext context, String groupName, String dn, List<String> users )
        throws Exception
    {

        Attributes attributes = new BasicAttributes( true );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        attributes.put( objectClass );
        attributes.put( "cn", groupName );
        BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );
        for ( String user : users )
        {
            basicAttribute.add( "uid=" + user + "," + suffix );// dc=archiva,dc=apache,dc=org" );
        }

        attributes.put( basicAttribute );
        context.createSubcontext( dn, attributes );
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
        attributes.put( "mail", cn + "@apache.org" );
        attributes.put( "userPassword", passwordEncoder.encodePassword( "foo" ) );
        attributes.put( "givenName", "foo" );
        context.createSubcontext( dn, attributes );
    }

    private void makeUsers()
        throws Exception
    {

        for ( String uid : users )
        {
            makeUser( uid );
        }

    }

    private void makeUser( String uid )
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();

        bindUserObject( context, uid, createDn( uid ) );
        assertExist( context, createDn( uid ), "cn", uid );
    }


    private void assertExist( DirContext context, String dn, String attribute, String value )
        throws NamingException
    {
        SearchControls ctls = new SearchControls();

        ctls.setDerefLinkFlag( true );
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        ctls.setReturningAttributes( new String[]{ "*" } );

        BasicAttributes matchingAttributes = new BasicAttributes();
        matchingAttributes.put( attribute, value );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "inetOrgPerson" );
        matchingAttributes.put( objectClass );

        NamingEnumeration<SearchResult> results = context.search( suffix, matchingAttributes );

        assertTrue( results.hasMoreElements() );
        SearchResult result = results.nextElement();
        Attributes attrs = result.getAttributes();
        Attribute testAttr = attrs.get( attribute );
        assertEquals( value, testAttr.get() );

    }

    private String createDn( String cn )
    {
        return "cn=" + cn + "," + suffix;
    }

    private String createGroupDn( String cn )
    {
        return "cn=" + cn + "," + groupSuffix;
    }


    @Test
    public void getAllGroups()
        throws Exception
    {
        List<String> allGroups = ldapRoleMapper.getAllGroups( getDirContext() );

        log.info( "allGroups: {}", allGroups );

        Assertions.assertThat( allGroups ).isNotNull().isNotEmpty().contains( "archiva-admin",
                                                                              "internal-repo-manager" );
    }

    @Test
    public void getGroupsMember()
        throws Exception
    {
        List<String> users = ldapRoleMapper.getGroupsMember( "archiva-admin", getDirContext() );

        log.info( "users for archiva-admin: {}", users );

        Assertions.assertThat( users ).isNotNull().isNotEmpty().hasSize( 2 ).contains( "admin", "user.7" );

        users = ldapRoleMapper.getGroupsMember( "internal-repo-observer", getDirContext() );

        Assertions.assertThat( users ).isNotNull().isNotEmpty().hasSize( 3 ).contains( "admin", "user.7", "user.8" );
    }

    @Test
    public void getGroups()
        throws Exception
    {
        List<String> groups = ldapRoleMapper.getGroups( "admin", getDirContext() );

        log.info( "groups for admin: {}", groups );

        Assertions.assertThat( groups ).isNotNull().isNotEmpty().hasSize( 3 ).contains( "archiva-admin",
                                                                                        "internal-repo-manager",
                                                                                        "internal-repo-observer" );

        groups = ldapRoleMapper.getGroups( "user.8", getDirContext() );

        Assertions.assertThat( groups ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "internal-repo-observer" );

        groups = ldapRoleMapper.getGroups( "user.7", getDirContext() );

        Assertions.assertThat( groups ).isNotNull().isNotEmpty().hasSize( 2 ).contains( "archiva-admin",
                                                                                        "internal-repo-observer" );
    }

    @Test
    public void getRoles()
        throws Exception
    {
        List<String> roles = ldapRoleMapper.getRoles( "admin", getDirContext(), roleNames );

        log.info( "roles for admin: {}", roles );

        Assertions.assertThat( roles ).isNotNull().isNotEmpty().hasSize( 3 ).contains( "Archiva System Administrator",
                                                                                       "Internal Repo Manager",
                                                                                       "Internal Repo Observer" );

        roles = ldapRoleMapper.getRoles( "user.7", getDirContext(), roleNames );

        log.info( "roles for user.7: {}", roles );

        Assertions.assertThat( roles ).isNotNull().isNotEmpty().hasSize( 2 ).contains( "Archiva System Administrator",
                                                                                       "Internal Repo Observer" );

        roles = ldapRoleMapper.getRoles( "user.8", getDirContext(), roleNames );

        log.info( "roles for user.8: {}", roles );

        Assertions.assertThat( roles ).isNotNull().isNotEmpty().hasSize( 1 ).contains( "Internal Repo Observer" );

    }

    @Test
    public void hasRoleFound()
        throws Exception
    {
        assertTrue( ldapRoleMapper.hasRole( getDirContext(), "Internal Repo Observer" ) );
    }

    @Test
    public void hasRoleNotFound()
        throws Exception
    {
        assertFalse( ldapRoleMapper.hasRole( getDirContext(), "Australian wine is good but not as French! " ) );
    }


}
