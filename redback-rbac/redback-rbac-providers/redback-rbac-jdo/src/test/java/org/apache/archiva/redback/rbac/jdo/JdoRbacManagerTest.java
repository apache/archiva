package org.apache.archiva.redback.rbac.jdo;

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
import org.apache.archiva.redback.components.jdo.DefaultConfigurableJdoFactory;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.common.jdo.test.StoreManagerDebug;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.tests.AbstractRbacManagerTestCase;
import org.jpox.AbstractPersistenceManagerFactory;
import org.jpox.SchemaTool;
import org.junit.Before;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import org.springframework.test.annotation.DirtiesContext;

/**
 * JdoRbacManagerTest:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public class JdoRbacManagerTest
    extends AbstractRbacManagerTestCase
{
    private StoreManagerDebug storeManager;

    @Inject
    @Named( value = "jdoFactory#users" )
    DefaultConfigurableJdoFactory jdoFactory;

    @Inject
    @Named( value = "rBACManager#jdo" )
    RBACManager rbacManager;

    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {

        super.setUp();

        assertEquals( DefaultConfigurableJdoFactory.class.getName(), jdoFactory.getClass().getName() );

        jdoFactory.setPersistenceManagerFactoryClass( "org.jpox.PersistenceManagerFactoryImpl" ); //$NON-NLS-1$

        jdoFactory.setDriverName(
            System.getProperty( "jdo.test.driver", "org.hsqldb.jdbcDriver" ) ); //$NON-NLS-1$  //$NON-NLS-2$

        jdoFactory.setUrl(
            System.getProperty( "jdo.test.url", "jdbc:hsqldb:mem:" + getName() ) ); //$NON-NLS-1$  //$NON-NLS-2$

        jdoFactory.setUserName( System.getProperty( "jdo.test.user", "sa" ) ); //$NON-NLS-1$

        jdoFactory.setPassword( System.getProperty( "jdo.test.pass", "" ) ); //$NON-NLS-1$

        jdoFactory.setProperty( "org.jpox.transactionIsolation", "READ_COMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.poid.transactionIsolation", "READ_COMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.autoCreateSchema", "true" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.autoCreateTables", "true" );

        jdoFactory.setProperty( "javax.jdo.option.RetainValues", "true" );

        jdoFactory.setProperty( "javax.jdo.option.RestoreValues", "true" );

        // jdoFactory.setProperty( "org.jpox.autoCreateColumns", "true" );

        jdoFactory.setProperty( "org.jpox.validateTables", "true" );

        jdoFactory.setProperty( "org.jpox.validateColumns", "true" );

        jdoFactory.setProperty( "org.jpox.validateConstraints", "true" );

        /* Enable the level 2 Ehcache class-based cache */
        jdoFactory.setProperty( "org.jpox.cache.level2", "true" );
        jdoFactory.setProperty( "org.jpox.cache.level2.type", "ehcacheclassbased" );
        jdoFactory.setProperty( "org.jpox.cache.level2.configurationFile", "/ehcache.xml" ); // ehcache config
        jdoFactory.setProperty( "org.jpox.cache.level2.cacheName", "default" ); // default cache name

        Properties properties = jdoFactory.getProperties();

        for ( Map.Entry<Object, Object> entry : properties.entrySet() )
        {
            System.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        URL[] jdoFileUrls =
            new URL[]{ getClass().getResource( "/org/apache/archiva/redback/rbac/jdo/package.jdo" ) }; //$NON-NLS-1$



        if ( ( jdoFileUrls == null ) || ( jdoFileUrls[0] == null ) )
        {
            fail( "Unable to process test " + getName() + " - missing package.jdo." );
        }

        File propsFile = null; // intentional
        boolean verbose = true;

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        /* set our own Store Manager to allow counting SQL statements */
        StoreManagerDebug.setup( (AbstractPersistenceManagerFactory) pmf );

        /* clean up the db */
        SchemaTool.deleteSchemaTables( jdoFileUrls, new URL[]{ }, propsFile, verbose );
        SchemaTool.createSchemaTables( jdoFileUrls, new URL[]{ }, propsFile, verbose, null );

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();

        setRbacManager( rbacManager );

        /* save the store manager to access the queries executed */
        JdoRbacManager rbacManager = (JdoRbacManager) getRbacManager();
        storeManager = StoreManagerDebug.getConfiguredStoreManager( rbacManager.getJdo().getPersistenceManager() );
    }


    @Override
    public void testGetAssignedRoles()
        throws RbacManagerException
    {
        storeManager.resetCounter();
        super.testGetAssignedRoles();
        int counter = storeManager.counter();
        /* without Level 2 cache: 15 queries */
        /* with    Level 2 cache:  8 queries */
        assertEquals( "Number of SQL queries", 8, counter );
    }

    @Override
    public void testGetAssignedPermissionsDeep()
        throws RbacManagerException
    {
        super.testGetAssignedPermissionsDeep();
        int counter = storeManager.counter();
        /* without Level 2 cache: 26 queries */
        /* with    Level 2 cache: 10 queries */
        assertEquals( "Number of SQL queries", 10, counter );
    }

    @Override
    protected void afterSetup()
    {
        super.afterSetup();
        storeManager.resetCounter();
    }

    @Override
    public void testLargeApplicationInit()
        throws RbacManagerException
    {
        for (String cacheName : CacheManager.getInstance().getCacheNames())
        {
            CacheManager.getInstance().getCache( cacheName ).removeAll();
        }
        super.testLargeApplicationInit();
    }

    @Override
    public void testGetRolesDeep()
        throws RbacManagerException
    {
        for (String cacheName : CacheManager.getInstance().getCacheNames())
        {
            CacheManager.getInstance().getCache( cacheName ).removeAll();
        }
        super.testGetRolesDeep();
    }


    @Override
    public void testStoreInitialization()
        throws Exception
    {
        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );
        super.testStoreInitialization();
        assertEquals( EVENTCOUNT, eventTracker.initCount );
    }
}
