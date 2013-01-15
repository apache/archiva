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

import net.sf.ehcache.CacheManager;
import org.apache.archiva.redback.components.apacheds.ApacheDs;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.encoders.SHA1PasswordEncoder;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.ldap.LdapRbacManager;
import org.apache.archiva.redback.tests.AbstractRbacManagerTestCase;
import org.junit.Before;

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

import org.junit.After;
import org.junit.BeforeClass;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_CLASS )
public class LdapRbacManagerTest
    extends AbstractRbacManagerTestCase
{

    @Inject
    @Named( value = "rbacManager#ldap" )
    LdapRbacManager rbacManager;

    @Inject
    @Named( value = "apacheDS#test" )
    private ApacheDs apacheDs;

    private String suffix, groupSuffix;

    private PasswordEncoder passwordEncoder;

    private Map<String, List<String>> usersPerGroup;

    private List<String> users;


    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        CacheManager.getInstance().clearAll();
        setRbacManager( rbacManager );

        assertTrue( getRbacManager() instanceof LdapRbacManager );

        rbacManager.setWritableLdap( true );

        passwordEncoder = new SHA1PasswordEncoder();

        usersPerGroup = new HashMap<String, List<String>>( 3 );

        usersPerGroup.put( "theADMIN", Arrays.asList( "admin", "user.9", "bob" ) );

        usersPerGroup.put( "thePROJECT_ADMIN", Arrays.asList( "admin", "bob" ) );

        usersPerGroup.put( "theDEVELOPER", Arrays.asList( "admin", "user.7", "bob" ) );

        users = new ArrayList<String>( 4 );
        users.add( "admin" );
        users.add( "user.7" );
        users.add( "user.8" );
        users.add( "user.9" );

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

        //makeUsers();

        //createGroups();
    }

    @After
    public void tearDown()
        throws Exception
    {

        InitialDirContext context = apacheDs.getAdminContext();
        /*
        for ( String uid : users )
        {
            context.unbind( createDn( uid ) );
        }

        for ( Map.Entry<String, List<String>> group : usersPerGroup.entrySet() )
        {
            context.unbind( createGroupDn( group.getKey() ) );
        }
        */
        context.unbind( suffix );

        apacheDs.stopServer();

        super.tearDown();
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

    private String createGroupDn( String cn )
    {
        return "cn=" + cn + "," + groupSuffix;
    }


    private String createDn( String cn )
    {
        return "cn=" + cn + "," + suffix;
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
        if ( !users.isEmpty() )
        {
            BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );
            for ( String user : users )
            {
                basicAttribute.add( "uid=" + user + "," + suffix );// dc=archiva,dc=apache,dc=org" );
            }

            attributes.put( basicAttribute );
        }

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


    @Override
    public void testStoreInitialization()
        throws Exception
    {
        CacheManager.getInstance().clearAll();
        //rbacManager.eraseDatabase();
        super.testStoreInitialization();
    }

    /*
     * event count workflow in cachedRbacMaanger is not working like JDO or Memory provider
     * trigger doesnt exist here.
     *  some test throws 1 event
     *  some test throws 2 events
     */
    @Override
    public void assertEventCount()
    {
        assertTrue( ( ( eventTracker.initCount > 0 ) && ( eventTracker.initCount <= 2 ) ) );
    }
}
