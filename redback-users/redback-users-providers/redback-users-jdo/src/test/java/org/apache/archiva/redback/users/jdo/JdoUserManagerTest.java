package org.apache.archiva.redback.users.jdo;

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

import org.apache.archiva.redback.components.jdo.DefaultConfigurableJdoFactory;
import org.apache.archiva.redback.common.jdo.test.StoreManagerDebug;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.provider.test.AbstractUserManagerTestCase;
import org.jpox.AbstractPersistenceManagerFactory;
import org.jpox.SchemaTool;
import org.junit.Before;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * JdoUserManagerTest
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class JdoUserManagerTest
    extends AbstractUserManagerTestCase
{
    @Inject
    @Named(value = "jdoFactory#users")
    DefaultConfigurableJdoFactory jdoFactory;

    @Inject
    @Named(value = "userManager#jdo")
    JdoUserManager jdoUserManager;

    private StoreManagerDebug storeManager;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        jdoFactory.setPersistenceManagerFactoryClass( "org.jpox.PersistenceManagerFactoryImpl" ); //$NON-NLS-1$

        jdoFactory.setDriverName( "org.hsqldb.jdbcDriver" ); //$NON-NLS-1$

        jdoFactory.setUrl( "jdbc:hsqldb:mem:" + getName() ); //$NON-NLS-1$

        jdoFactory.setUserName( "sa" ); //$NON-NLS-1$

        jdoFactory.setPassword( "" ); //$NON-NLS-1$

        jdoFactory.setProperty( "org.jpox.transactionIsolation", "READ_COMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.poid.transactionIsolation", "READ_COMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.autoCreateSchema", "true" ); //$NON-NLS-1$ //$NON-NLS-2$

        Properties properties = jdoFactory.getProperties();

        for ( Map.Entry<?, ?> entry : properties.entrySet() )
        {
            System.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        /* set our own Store Manager to allow counting SQL statements */
        StoreManagerDebug.setup( (AbstractPersistenceManagerFactory) pmf );

        SchemaTool.createSchemaTables(
            new URL[]{ getClass().getResource( "/org/apache/archiva/redback/users/jdo/package.jdo" ) }, new URL[]{ },
            null, false, null ); //$NON-NLS-1$

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();

        setUserManager( jdoUserManager );

        /* save the store manager to access the queries executed */
        JdoUserManager userManager = (JdoUserManager) getUserManager();
        storeManager = StoreManagerDebug.getConfiguredStoreManager( userManager.getPersistenceManager() );

    }

    protected void assertCleanUserManager()
        throws UserManagerException
    {
        // database cleanup
        ( (JdoUserManager) getUserManager() ).eraseDatabase();
        super.assertCleanUserManager();
    }


}
